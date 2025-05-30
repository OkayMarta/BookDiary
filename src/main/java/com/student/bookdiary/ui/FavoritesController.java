package com.student.bookdiary.ui;

import com.student.bookdiary.model.Book;
import com.student.bookdiary.persistence.BookDao;
import com.student.bookdiary.persistence.SqliteBookDao;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent; // Для обробки кліку на плитку
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Objects;

public class FavoritesController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(FavoritesController.class);

    // --- FXML Поля ---
    @FXML private TextField searchField; // Поле для пошуку книг
    @FXML private ComboBox<String> sortComboBox; // Випадаючий список для сортування
    @FXML private FlowPane bookFlowPane; // Панель для відображення книг у вигляді плиток
    @FXML private ComboBox<String> genreFilterComboBox; // Випадаючий список для фільтрації за жанром

    // --- DAO та інше ---
    private BookDao bookDao; // Об'єкт для доступу до даних книг
    private PrimaryController primaryController; // Контролер головного вікна для навігації

    // --- Константи ---
    private static final String SORT_BY_DATE_READ_DESC = "Дата прочитання (новіші)"; // Сортування за датою прочитання (спочатку новіші)
    private static final String SORT_BY_DATE_READ_ASC = "Дата прочитання (старіші)"; // Сортування за датою прочитання (спочатку старіші)
    private static final String SORT_BY_TITLE = "Назва (А-Я)"; // Сортування за назвою книги
    private static final String SORT_BY_RATING_DESC = "Рейтинг (спадання)"; // Сортування за рейтингом (від вищого до нижчого)
    private static final String SORT_BY_RATING_ASC = "Рейтинг (зростання)"; // Сортування за рейтингом (від нижчого до вищого)
    private static final String SORT_BY_DATE_ADDED_DESC = "Дата додавання до улюблених (новіші)"; // Сортування за датою додавання до улюблених
    private static final String ALL_GENRES_OPTION = "Всі жанри"; // Опція для відображення всіх жанрів

    // Форматер для дати
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    /**
     * Встановлює посилання на головний контролер.
     * @param primaryController головний контролер програми.
     */
    public void setPrimaryController(PrimaryController primaryController) {
        this.primaryController = primaryController;
    }

    /**
     * Метод ініціалізації контролера, викликається після завантаження FXML.
     * Налаштовує елементи UI, обробники подій та завантажує початковий список книг.
     */
    @FXML
    private void initialize() {
        log.info("Ініціалізація FavoritesController...");
        this.bookDao = new SqliteBookDao();
        setupSortComboBox();
        setupGenreFilterComboBox();

        // Програмне застосування CSS класів до елементів керування
        searchField.getStyleClass().add("filter-search-field");
        sortComboBox.getStyleClass().add("filter-combo-box");
        genreFilterComboBox.getStyleClass().add("filter-combo-box");

        // Додавання слухачів для автоматичного оновлення списку при зміні тексту пошуку або вибору сортування/фільтра
        searchField.textProperty().addListener((_, _, _) -> {
            log.trace("Змінено текст пошуку, оновлення списку улюблених книг.");
            loadBooks();
        });
        sortComboBox.valueProperty().addListener((_, _, newVal) -> {
            if (newVal != null) {
                log.trace("Змінено опцію сортування на '{}', оновлення списку улюблених книг.", newVal);
                loadBooks();
            }
        });
        genreFilterComboBox.valueProperty().addListener((_, _, newVal) -> {
            if (newVal != null) {
                log.trace("Змінено фільтр жанру на '{}', оновлення списку улюблених книг.", newVal);
                loadBooks();
            }
        });

        loadBooks(); // Початкове завантаження списку улюблених книг
        log.info("FavoritesController успішно ініціалізовано.");
    }

    /**
     * Налаштовує випадаючий список для сортування улюблених книг.
     * Додає доступні опції сортування та встановлює значення за замовчуванням.
     */
    private void setupSortComboBox() {
        sortComboBox.getItems().addAll(
                SORT_BY_DATE_READ_DESC,
                SORT_BY_DATE_ADDED_DESC,
                SORT_BY_TITLE,
                SORT_BY_RATING_DESC,
                SORT_BY_RATING_ASC,
                SORT_BY_DATE_READ_ASC
        );
        sortComboBox.setValue(SORT_BY_DATE_ADDED_DESC); // Сортування за замовчуванням
        log.debug("Випадаючий список сортування налаштовано.");
    }

    /**
     * Налаштовує випадаючий список для фільтрації улюблених книг за жанром.
     * Завантажує унікальні жанри з бази даних та додає опцію "Всі жанри".
     */
    private void setupGenreFilterComboBox() {
        List<String> genres = bookDao.getDistinctGenres(); // Отримуємо список унікальних жанрів
        genres.sort(String.CASE_INSENSITIVE_ORDER); // Сортуємо жанри за алфавітом
        genres.addFirst(ALL_GENRES_OPTION); // Додаємо опцію "Всі жанри" на початок списку
        genreFilterComboBox.setItems(FXCollections.observableArrayList(genres));
        genreFilterComboBox.setValue(ALL_GENRES_OPTION); // Встановлюємо "Всі жанри" як значення за замовчуванням
        log.debug("Випадаючий список фільтрації за жанром налаштовано.");
    }

    /**
     * Завантажує список улюблених книг з бази даних, застосовує фільтрацію та сортування,
     * та оновлює відображення книг у FlowPane.
     */
    public void loadBooks() {
        log.debug("Завантаження та оновлення списку улюблених книг...");
        try {
            List<Book> books = bookDao.getFavoriteBooks(); // Отримуємо список всіх улюблених книг

            // Фільтрація за пошуковим запитом
            String searchTerm = searchField.getText().trim().toLowerCase();
            if (!searchTerm.isEmpty()) {
                log.debug("Застосування фільтра пошуку: '{}'", searchTerm);
                books = books.stream()
                        .filter(book -> book.getTitle().toLowerCase().contains(searchTerm) ||
                                (book.getAuthor() != null && book.getAuthor().toLowerCase().contains(searchTerm)))
                        .collect(Collectors.toList());
            } else {
                log.trace("Пошуковий запит порожній, фільтрація за пошуком не застосовується.");
            }

            // Фільтрація за вибраним жанром
            String selectedGenre = genreFilterComboBox.getValue();
            if (selectedGenre != null && !selectedGenre.equals(ALL_GENRES_OPTION) && !selectedGenre.isBlank()) {
                log.debug("Застосування фільтра за жанром: '{}'", selectedGenre);
                books = books.stream()
                        .filter(book -> selectedGenre.equalsIgnoreCase(book.getGenre()))
                        .collect(Collectors.toList());
            } else {
                log.trace("Фільтр за жанром не вибрано або вибрано '{}', фільтрація за жанром не застосовується.", ALL_GENRES_OPTION);
            }

            // Сортування списку книг
            String sortOption = sortComboBox.getValue();
            if (sortOption != null) {
                log.debug("Застосування сортування: '{}'", sortOption);
                Comparator<Book> comparator = switch (sortOption) {
                    case SORT_BY_DATE_READ_ASC -> Comparator.comparing(Book::getDateRead, Comparator.nullsLast(Comparator.naturalOrder()));
                    case SORT_BY_TITLE -> Comparator.comparing(Book::getTitle, String.CASE_INSENSITIVE_ORDER);
                    case SORT_BY_RATING_DESC -> Comparator.comparingInt(Book::getRating).reversed();
                    case SORT_BY_RATING_ASC -> Comparator.comparingInt(Book::getRating);
                    case SORT_BY_DATE_ADDED_DESC -> Comparator.comparing(Book::getDateAdded, Comparator.nullsLast(Comparator.reverseOrder()));
                    default -> // За замовчуванням (SORT_BY_DATE_READ_DESC)
                            Comparator.comparing(Book::getDateRead, Comparator.nullsLast(Comparator.reverseOrder()));
                };
                books.sort(comparator);
            }

            // Оновлення FlowPane плитками книг
            bookFlowPane.getChildren().clear();
            for (Book book : books) {
                Node bookTile = createBookTile(book);
                bookFlowPane.getChildren().add(bookTile);
            }
            log.info("Список улюблених книг оновлено. Показано {} плиток.", books.size());

        } catch (Exception e) {
            log.error("Помилка завантаження списку улюблених книг", e);
            bookFlowPane.getChildren().clear(); // Очищаємо панель у разі помилки
            if (primaryController != null) {
                primaryController.showErrorAlert("Помилка Завантаження", "Не вдалося завантажити список улюблених книг.", e.getMessage());
            }
        }
    }

    /**
     * Створює вузол (Node) для відображення книги у вигляді плитки.
     * Кожна плитка містить обкладинку, назву, автора, рейтинг, дату прочитання та кнопку для видалення з улюблених.
     * @param book Книга для відображення.
     * @return Node, що представляє плитку книги.
     */
    private Node createBookTile(Book book) {
        // Головний контейнер плитки
        VBox tileContainer = new VBox(5); // 5px вертикальний відступ між елементами
        tileContainer.getStyleClass().add("book-tile");
        tileContainer.setPrefWidth(200); // Бажана ширина плитки
        tileContainer.setMinWidth(170);  // Мінімальна ширина плитки

        // 1. Обкладинка книги
        ImageView coverImageView = new ImageView();
        coverImageView.setFitHeight(170); // Висота обкладинки
        coverImageView.setFitWidth(110);  // Ширина обкладинки
        coverImageView.setPreserveRatio(true); // Зберігати пропорції зображення
        coverImageView.getStyleClass().add("book-tile-cover");
        loadTileCoverImage(book.getCoverImagePath(), coverImageView);

        // 2. Контейнер для назви та автора
        VBox mainContentBox = new VBox(4); // 4px вертикальний відступ
        mainContentBox.setAlignment(Pos.TOP_CENTER);

        Label titleLabel = new Label(book.getTitle());
        titleLabel.getStyleClass().add("book-tile-title");

        Label authorLabel = new Label(book.getAuthor() != null ? book.getAuthor() : "");
        authorLabel.getStyleClass().add("book-tile-author");

        mainContentBox.getChildren().addAll(titleLabel, authorLabel);
        VBox.setVgrow(mainContentBox, Priority.ALWAYS); // Дозволяє цьому блоку розтягуватися по вертикалі

        // 3. Нижній блок (для рейтингу, дати та кнопки "улюблене")
        VBox overallBottomContainer = new VBox(4); // 4px вертикальний відступ
        overallBottomContainer.getStyleClass().add("book-tile-bottom-container");
        overallBottomContainer.setAlignment(Pos.CENTER);

        // 3.1. Рядок з рейтингом
        HBox ratingRow = new HBox();
        ratingRow.getStyleClass().add("book-tile-rating-box");
        if (book.getRating() > 0) {
            for (int i = 0; i < 5; i++) {
                Label starLabel = new Label(i < book.getRating() ? "★" : "☆"); // Заповнена або порожня зірка
                starLabel.getStyleClass().add(i < book.getRating() ? "book-tile-star-filled" : "book-tile-star-empty");
                ratingRow.getChildren().add(starLabel);
            }
        } else {
            Label noRatingLabel = new Label("Без оцінки");
            noRatingLabel.getStyleClass().add("book-tile-no-rating");
            ratingRow.getChildren().add(noRatingLabel);
        }

        // 3.2. Рядок з датою прочитання та кнопкою "Прибрати з улюблених"
        HBox dateFavoriteRow = new HBox();
        dateFavoriteRow.setAlignment(Pos.CENTER_LEFT); // Вирівнювання елементів по лівому краю
        dateFavoriteRow.setSpacing(8); // 8px горизонтальний відступ між елементами

        // 3.2.1. Дата прочитання
        Label dateReadLabel = new Label(book.getDateRead() != null ? book.getDateRead().format(DATE_FORMATTER) : "");
        dateReadLabel.getStyleClass().add("book-tile-date");

        // 3.2.2. Розширювач для кнопки "улюблене" (щоб кнопка була справа)
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS); // Розтягується, щоб заповнити доступний простір

        // 3.2.3. Кнопка "Прибрати з улюблених"
        Button favoriteButton = new Button();
        favoriteButton.getStyleClass().add("book-tile-favorite-button");

        ImageView favoriteIconView = new ImageView();
        try {
            // Завантаження іконки "розбите серце"
            Image iconImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/com/student/bookdiary/ui/icons/heart_broken.png")));
            favoriteIconView.setImage(iconImage);
            favoriteIconView.setFitWidth(40);  // Розмір іконки
            favoriteIconView.setFitHeight(40); // Розмір іконки
            favoriteIconView.setPreserveRatio(true);
        } catch (Exception e) {
            log.error("Помилка завантаження іконки heart_broken.png для кнопки 'Прибрати з улюблених'", e);
        }
        favoriteButton.setGraphic(favoriteIconView);
        favoriteButton.setTooltip(new Tooltip("Прибрати з улюблених"));

        favoriteButton.setOnAction(event -> {
            log.debug("Натиснуто кнопку 'Прибрати з улюблених' для книги: {}", book.getTitle());
            handleToggleFavorite(book);
        });

        dateFavoriteRow.getChildren().addAll(dateReadLabel, spacer, favoriteButton);

        // Додавання рядків до загального нижнього контейнера
        overallBottomContainer.getChildren().addAll(ratingRow, dateFavoriteRow);

        // Додавання всіх основних частин до головного контейнера плитки
        tileContainer.getChildren().addAll(coverImageView, mainContentBox, overallBottomContainer);

        // Обробник подвійного кліку на плитку для переходу до деталей книги
        tileContainer.setOnMouseClicked(event -> {
            // Перевіряємо, чи клік не був на кнопці "улюблене"
            Node target = (Node) event.getTarget();
            boolean clickedOnFavoriteButton = false;
            while (target != null && target != tileContainer) {
                if (target == favoriteButton) {
                    clickedOnFavoriteButton = true;
                    break;
                }
                target = target.getParent();
            }

            if (!clickedOnFavoriteButton && event.getClickCount() == 2) { // Подвійний клік
                if (primaryController != null) {
                    log.debug("Подвійний клік на плитку улюбленої книги: '{}'. Перехід до деталей.", book.getTitle());
                    primaryController.showBookDetailView(book, "favorites_view.fxml"); // Передаємо назву поточного FXML для повернення
                } else {
                    log.warn("PrimaryController не встановлено, неможливо показати деталі книги.");
                }
            }
        });
        return tileContainer;
    }

    /**
     * Завантажує зображення обкладинки для плитки книги.
     * Якщо файл обкладинки не знайдено або сталася помилка, встановлює зображення за замовчуванням.
     * @param filename Ім'я файлу обкладинки.
     * @param imageView ImageView, в який буде завантажено зображення.
     */
    private void loadTileCoverImage(String filename, ImageView imageView) {
        imageView.setImage(null); // Скидаємо попереднє зображення
        if (filename == null || filename.isBlank()) {
            log.trace("Ім'я файлу обкладинки відсутнє, завантаження дефолтного зображення.");
            imageView.setImage(loadDefaultImage());
            return;
        }
        try {
            Path fullImagePath = Paths.get(App.COVERS_DIRECTORY_NAME, filename);
            File imageFile = fullImagePath.toFile();
            if (imageFile.exists() && imageFile.isFile()) {
                // Завантажуємо зображення з кешуванням та фоновим завантаженням
                Image image = new Image(imageFile.toURI().toString(), 100, 150, true, true, true);
                if (image.isError()) {
                    log.error("Помилка завантаження зображення обкладинки (плитка) [{}]: {}", fullImagePath, image.getException().getMessage());
                    imageView.setImage(loadDefaultImage());
                } else {
                    imageView.setImage(image);
                    log.trace("Обкладинку '{}' успішно завантажено для плитки.", filename);
                }
            } else {
                log.warn("Файл обкладинки (плитка) '{}' не знайдено.", fullImagePath);
                imageView.setImage(loadDefaultImage());
            }
        } catch (Exception e) {
            log.error("Не вдалося завантажити зображення обкладинки (плитка) з файлу '{}': {}", filename, e.getMessage(), e);
            imageView.setImage(loadDefaultImage());
        }
    }

    /**
     * Завантажує стандартне зображення-заглушку для обкладинки книги з ресурсів.
     * @return Об'єкт Image з дефолтним зображенням або null, якщо завантаження не вдалося.
     */
    private Image loadDefaultImage() {
        try (InputStream is = getClass().getResourceAsStream("/com/student/bookdiary/ui/icons/default_book_cover.png")) {
            if (is == null) {
                log.error("Не вдалося знайти ресурс з дефолтним зображенням обкладинки.");
                return null;
            }
            return new Image(is);
        } catch (Exception e) {
            log.error("Помилка завантаження дефолтного зображення обкладинки: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Обробляє подію пошуку (натискання Enter у полі пошуку або клік на кнопку "Пошук", якщо вона є).
     * @param event Подія дії.
     */
    @FXML
    private void handleSearch(ActionEvent event) {
        log.debug("Виконано дію пошуку (наприклад, натискання Enter у полі пошуку).");
        loadBooks();
    }

    /**
     * Обробляє дію прибирання книги зі списку улюблених.
     * Оновлює статус книги в базі даних та перезавантажує список улюблених.
     * @param book Книга, яку потрібно прибрати з улюблених.
     */
    private void handleToggleFavorite(Book book) {
        if (book == null) {
            log.warn("Спроба прибрати з улюблених null книгу.");
            return;
        }
        log.debug("Прибирання книги ID={} ('{}') з улюблених.", book.getId(), book.getTitle());
        try {
            book.setFavorite(false); // Знімаємо позначку "улюблене"
            bookDao.updateBook(book);
            log.info("Книгу ID={} ('{}') прибрано з улюблених.", book.getId(), book.getTitle());
            loadBooks(); // Перезавантажуємо список, щоб книга зникла з нього
        } catch (Exception e) {
            log.error("Помилка під час прибирання книги ID={} ('{}') з улюблених.", book.getId(), book.getTitle(), e);
            // Важливо повідомити користувача про помилку
            showErrorAlertHelper("Помилка Оновлення", "Не вдалося оновити статус книги.", e.getMessage());
        }
    }

    /**
     * Обробляє запит на редагування книги.
     * Викликається, наприклад, з BookDetailController.
     * @param book Книга для редагування.
     */
    private void handleEditBook(Book book) {
        if (book == null) {
            log.warn("Спроба редагувати null книгу.");
            return;
        }
        log.debug("Запит на редагування улюбленої книги: {}", book.getTitle());
        if (primaryController != null) {
            primaryController.showAddEditBookDialog(book);
        } else {
            log.error("PrimaryController не встановлено! Неможливо відкрити вікно редагування.");
            showErrorAlertHelper("Помилка", "Неможливо відкрити вікно редагування.", "PrimaryController не ініціалізовано.");
        }
    }

    /**
     * Обробляє запит на повне видалення книги з системи.
     * Викликається, наприклад, з BookDetailController.
     * Показує діалог підтвердження перед видаленням.
     * @param book Книга для видалення.
     */
    private void handleDeleteBook(Book book) {
        if (book == null) {
            log.warn("Спроба видалити null книгу.");
            return;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(getOwnerWindow());
        alert.setTitle("Підтвердження видалення");
        alert.setHeaderText("Видалити книгу \"" + book.getTitle() + "\" повністю?");
        alert.setContentText("Книга буде видалена з бази даних, включаючи всі списки (прочитані, бажані, улюлюблені). Цю дію неможливо скасувати.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK){
            log.info("Підтверджено повне видалення улюбленої книги ID={} ('{}').", book.getId(), book.getTitle());
            try {
                // Видалення файлу обкладинки, якщо він існує
                if (book.getCoverImagePath() != null && !book.getCoverImagePath().isBlank()) {
                    deleteCoverFile(book.getCoverImagePath());
                }
                bookDao.deleteBook(book.getId());
                log.info("Книгу ID={} ('{}') повністю видалено з БД.", book.getId(), book.getTitle());
                loadBooks(); // Оновлюємо список улюблених (книга зникне)
            } catch (Exception e) {
                log.error("Помилка під час повного видалення книги ID={} ('{}')", book.getId(), book.getTitle(), e);
                showErrorAlertHelper("Помилка Видалення", "Не вдалося видалити книгу.", e.getMessage());
            }
        } else {
            log.debug("Повне видалення книги ID={} ('{}') скасовано користувачем.", book.getId(), book.getTitle());
        }
    }

    /**
     * Видаляє файл обкладинки з диску.
     * @param filename Ім'я файлу обкладинки.
     */
    private void deleteCoverFile(String filename) {
        if (filename == null || filename.isBlank()) {
            log.trace("Ім'я файлу обкладинки для видалення відсутнє або порожнє.");
            return;
        }
        try {
            Path filePath = Paths.get(App.COVERS_DIRECTORY_NAME, filename);
            if (Files.deleteIfExists(filePath)) {
                log.info("Файл обкладинки '{}' успішно видалено.", filePath);
            } else {
                log.warn("Файл обкладинки '{}' не знайдено для видалення (можливо, вже видалено або не існував).", filePath);
            }
        } catch (IOException e) {
            log.error("Помилка при видаленні файлу обкладинки '{}'", filename, e);
            // Не показуємо помилку користувачу, оскільки це фонова операція
        }
    }

    /**
     * Допоміжний метод для відображення діалогового вікна з повідомленням про помилку.
     * @param title Заголовок вікна.
     * @param header Текст заголовка повідомлення.
     * @param content Детальний текст повідомлення про помилку.
     */
    private void showErrorAlertHelper(String title, String header, String content) {
        Alert errorAlert = new Alert(Alert.AlertType.ERROR);
        errorAlert.initOwner(getOwnerWindow()); // Встановлюємо батьківське вікно
        errorAlert.setTitle(title);
        errorAlert.setHeaderText(header);
        errorAlert.setContentText(content != null ? content : "Деталі помилки відсутні.");
        errorAlert.showAndWait();
    }

    /**
     * Отримує батьківське вікно для діалогових вікон.
     * Це потрібно для правильного відображення Alert'ів.
     * @return Об'єкт Window, що є батьківським, або null, якщо визначити не вдалося.
     */
    private javafx.stage.Window getOwnerWindow() {
        // Спроба отримати вікно через FlowPane або інший доступний елемент UI
        if (bookFlowPane != null && bookFlowPane.getScene() != null) {
            return bookFlowPane.getScene().getWindow();
        }
        if (searchField != null && searchField.getScene() != null) {
            return searchField.getScene().getWindow();
        }
        log.warn("Не вдалося визначити батьківське вікно для Alert у FavoritesController. Діалог може відобразитися некоректно.");
        return null; // Якщо вікно не знайдено
    }

    @Override
    public void refreshData() {
        log.info("Оновлення даних у FavoritesController");
        loadBooks();
    }
}