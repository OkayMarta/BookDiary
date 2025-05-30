package com.student.bookdiary.ui;

import com.student.bookdiary.model.Book;
import com.student.bookdiary.model.ReadingStatus;
import com.student.bookdiary.persistence.BookDao;
import com.student.bookdiary.persistence.SqliteBookDao;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent; // Необхідний для обробки кліку на плитці книги
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
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
import java.util.Optional; // Необхідний для діалогу підтвердження видалення
import java.util.stream.Collectors;

/**
 * Контролер для управління списком бажаних книг ("Хочу прочитати").
 * Відповідає за відображення, фільтрацію, сортування та керування книгами,
 * які користувач планує прочитати. Використовує {@link FlowPane} для гнучкого
 * відображення книг у вигляді плиток.
 */
public class WishlistController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(WishlistController.class);

    /** Шлях до стандартного зображення-заглушки для обкладинки книги. */
    private static final String DEFAULT_COVER_PATH = "/com/student/bookdiary/ui/icons/default_book_cover.png";

    /** Компоненти інтерфейсу, що автоматично ін'єктуються з FXML-файлу. */
    @FXML private TextField searchField;            // Поле для пошуку книг
    @FXML private ComboBox<String> sortComboBox;    // Випадаючий список для вибору критерію сортування
    @FXML private FlowPane bookFlowPane;            // Контейнер для відображення книг у вигляді плиток
    @FXML private ComboBox<String> genreFilterComboBox; // Випадаючий список для фільтрації за жанром

    /** Сервіси та залежності. */
    private BookDao bookDao;                        // Об'єкт доступу до даних (DAO) для книг
    private PrimaryController primaryController;    // Головний контролер програми
    private boolean isInitialized = false;          // Прапорець, що вказує на завершення ініціалізації контролера

    /** Константи для опцій сортування. */
    private static final String SORT_BY_DATE_ADDED_DESC = "Дата додавання (новіші)";
    private static final String SORT_BY_DATE_ADDED_ASC = "Дата додавання (старіші)";
    private static final String SORT_BY_TITLE = "Назва (А-Я)";
    private static final String SORT_BY_AUTHOR = "Автор (А-Я)";

    /** Константи для опцій фільтрації. */
    private static final String ALL_GENRES_OPTION = "Всі жанри";

    /** Форматер для дати. */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    /**
     * Встановлює головний контролер ({@link PrimaryController}) для цього контролера.
     * Цей метод повинен бути викликаний після створення екземпляра {@code WishlistController},
     * але перед будь-якими операціями, що потребують взаємодії з головним контролером
     * (наприклад, відображення діалогів помилок або перехід до інших подань).
     * Якщо контролер вже ініціалізовано, викликає завантаження книг.
     *
     * @param primaryController Головний контролер програми.
     */
    @Override
    public void setPrimaryController(PrimaryController primaryController) {
        this.primaryController = primaryController;
        if (isInitialized) {
            log.info("PrimaryController встановлено для WishlistController. Завантаження списку бажаних книг...");
            loadBooks(); // Завантажуємо книги, оскільки primaryController тепер доступний
        } else {
            log.warn("WishlistController ще не був ініціалізований на момент встановлення PrimaryController. Книги будуть завантажені після initialize().");
        }
    }

    /**
     * Метод ініціалізації контролера. Викликається автоматично після завантаження FXML-файлу.
     * Налаштовує DAO, елементи управління (комбо-бокси, поле пошуку), встановлює слухачів подій
     * та програмно додає CSS класи для стилізації.
     */
    @FXML
    private void initialize() {
        log.info("Ініціалізація WishlistController (використовується FlowPane для відображення книг)...");
        this.bookDao = new SqliteBookDao();
        setupSortComboBox();
        setupGenreFilterComboBox();

        // Програмне додавання CSS класів для кращого контролю стилів
        searchField.getStyleClass().add("filter-search-field");
        sortComboBox.getStyleClass().add("filter-combo-box");
        genreFilterComboBox.getStyleClass().add("filter-combo-box");

        setupEventListeners();

        isInitialized = true;
        // Завантаження книг відбудеться після встановлення primaryController або через refreshData()
        log.info("WishlistController (з FlowPane) успішно ініціалізовано. Очікування PrimaryController для завантаження книг.");
    }

    /**
     * Налаштовує слухачів подій для елементів управління (комбо-бокси, поле пошуку).
     * При зміні значення в будь-якому з цих елементів викликається метод {@link #loadBooks()}
     * для оновлення списку відображуваних книг.
     */
    private void setupEventListeners() {
        sortComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals(oldVal)) {
                log.debug("Змінено критерій сортування на: {}", newVal);
                loadBooks();
            }
        });

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            // Миттєвий пошук при введенні тексту, можна додати debounce логіку при необхідності
            log.debug("Змінено текст пошуку на: '{}'", newVal);
            loadBooks();
        });

        genreFilterComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals(oldVal)) {
                log.debug("Змінено фільтр жанру на: {}", newVal);
                loadBooks();
            }
        });
    }

    /**
     * Налаштовує випадаючий список (ComboBox) для вибору критерію сортування книг.
     * Встановлює доступні опції сортування та значення за замовчуванням.
     */
    private void setupSortComboBox() {
        sortComboBox.getItems().addAll(
                SORT_BY_DATE_ADDED_DESC, SORT_BY_DATE_ADDED_ASC, SORT_BY_TITLE, SORT_BY_AUTHOR
        );
        sortComboBox.setValue(SORT_BY_DATE_ADDED_DESC); // Встановлення значення за замовчуванням
        log.debug("Комбо-бокс сортування налаштовано. Значення за замовчуванням: {}", SORT_BY_DATE_ADDED_DESC);
    }

    /**
     * Налаштовує випадаючий список (ComboBox) для фільтрації книг за жанром.
     * Завантажує унікальні жанри з бази даних, сортує їх та додає опцію "Всі жанри".
     */
    private void setupGenreFilterComboBox() {
        List<String> genres = bookDao.getDistinctGenres();
        log.debug("Завантажено {} унікальних жанрів з БД.", genres.size());
        genres.sort(String.CASE_INSENSITIVE_ORDER); // Сортування жанрів за алфавітом
        genres.add(0, ALL_GENRES_OPTION); // Додавання опції "Всі жанри" на початок списку
        genreFilterComboBox.setItems(FXCollections.observableArrayList(genres));
        genreFilterComboBox.setValue(ALL_GENRES_OPTION); // Встановлення значення за замовчуванням
        log.debug("Комбо-бокс фільтрації за жанром налаштовано. Значення за замовчуванням: {}", ALL_GENRES_OPTION);
    }

    /**
     * Завантажує та відображає список книг зі статусом "Хочу прочитати".
     * Перед відображенням застосовує вибрані фільтри та сортування.
     * Якщо {@link PrimaryController} не встановлено, завантаження відкладається.
     */
    public void loadBooks() {
        log.debug("Спроба завантаження та оновлення списку бажаних книг для FlowPane...");

        if (primaryController == null) {
            log.warn("PrimaryController не встановлено. Завантаження списку бажаних книг відкладено.");
            // Можна показати повідомлення користувачу або просто нічого не робити до встановлення контролера
            return;
        }
        if (bookDao == null) {
            log.error("BookDao не ініціалізовано. Неможливо завантажити книги.");
            primaryController.showErrorAlert("Критична помилка", "Помилка доступу до даних.", "BookDao не доступний.");
            return;
        }


        try {
            List<Book> books = bookDao.getBooksByStatus(ReadingStatus.WANT_TO_READ);
            log.info("Завантажено {} книг зі статусом 'Хочу прочитати' з БД.", books.size());
            applyFiltersAndSorting(books); // Застосування фільтрів та сортування
            displayBooks(books); // Відображення книг
            log.info("Список бажаних книг оновлено та відображено: {} книг відповідають критеріям.", books.size());
        } catch (Exception e) {
            log.error("Помилка під час завантаження або обробки списку бажаних книг.", e);
            if (bookFlowPane != null) {
                bookFlowPane.getChildren().clear(); // Очищення панелі у разі помилки
            }
            primaryController.showErrorAlert("Помилка Завантаження", "Не вдалося завантажити список бажаних книг.", e.getMessage());
        }
    }

    /**
     * Застосовує поточні фільтри (пошуковий запит, жанр) та сортування до наданого списку книг.
     * Модифікує переданий список книг.
     * @param books Список книг, до якого потрібно застосувати фільтри та сортування.
     */
    private void applyFiltersAndSorting(List<Book> books) {
        // Фільтрація за пошуковим запитом
        String searchTerm = searchField.getText().trim().toLowerCase();
        if (!searchTerm.isEmpty()) {
            log.debug("Застосування фільтра пошуку: '{}'", searchTerm);
            books.removeIf(book -> !matchesSearchTerm(book, searchTerm));
        }

        // Фільтрація за жанром
        String selectedGenre = genreFilterComboBox.getValue();
        if (selectedGenre != null && !selectedGenre.equals(ALL_GENRES_OPTION)) {
            log.debug("Застосування фільтра за жанром: '{}'", selectedGenre);
            books.removeIf(book -> !selectedGenre.equalsIgnoreCase(book.getGenre()));
        }

        // Застосування сортування
        applySorting(books);
    }

    /**
     * Перевіряє, чи відповідає книга заданому пошуковому запиту.
     * Пошук здійснюється за назвою та автором книги (без урахування регістру).
     * @param book Книга для перевірки.
     * @param searchTerm Пошуковий запит (в нижньому регістрі).
     * @return {@code true}, якщо книга відповідає запиту, інакше {@code false}.
     */
    private boolean matchesSearchTerm(Book book, String searchTerm) {
        boolean titleMatches = book.getTitle().toLowerCase().contains(searchTerm);
        boolean authorMatches = book.getAuthor() != null && book.getAuthor().toLowerCase().contains(searchTerm);
        return titleMatches || authorMatches;
    }

    /**
     * Застосовує вибраний критерій сортування до списку книг.
     * Модифікує переданий список книг.
     * @param books Список книг для сортування.
     */
    private void applySorting(List<Book> books) {
        String sortOption = sortComboBox.getValue();
        if (sortOption == null) {
            log.warn("Критерій сортування не вибрано (null). Сортування не буде застосовано.");
            return;
        }
        log.debug("Застосування сортування: '{}'", sortOption);

        Comparator<Book> comparator = switch (sortOption) {
            case SORT_BY_DATE_ADDED_ASC -> Comparator.comparing(Book::getDateAdded,
                    Comparator.nullsLast(Comparator.naturalOrder()));
            case SORT_BY_TITLE -> Comparator.comparing(Book::getTitle,
                    String.CASE_INSENSITIVE_ORDER);
            case SORT_BY_AUTHOR -> Comparator.comparing(Book::getAuthor,
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            default -> Comparator.comparing(Book::getDateAdded, // SORT_BY_DATE_ADDED_DESC або невідомий
                    Comparator.nullsLast(Comparator.reverseOrder()));
        };
        books.sort(comparator);
    }

    /**
     * Відображає відфільтрований та відсортований список книг у {@link FlowPane}.
     * Кожна книга представляється у вигляді окремої плитки.
     * @param books Список книг для відображення.
     */
    private void displayBooks(List<Book> books) {
        if (bookFlowPane == null) {
            log.error("bookFlowPane не ініціалізовано. Неможливо відобразити книги.");
            return;
        }
        bookFlowPane.getChildren().clear(); // Очищення попередніх плиток
        if (books.isEmpty()) {
            log.info("Список книг для відображення порожній.");
            Label emptyLabel = new Label("Немає книг у списку бажань, що відповідають вашим критеріям.");
            emptyLabel.getStyleClass().add("empty-list-label");
            bookFlowPane.getChildren().add(emptyLabel);
        } else {
            books.forEach(book -> bookFlowPane.getChildren().add(createBookTile(book)));
            log.debug("Відображено {} плиток книг у FlowPane.", books.size());
        }
    }

    /**
     * Створює візуальне представлення (плитку) для однієї книги.
     * Плитка містить обкладинку, назву, автора, дату додавання та кнопку "Прочитано!".
     * @param book Книга, для якої створюється плитка.
     * @return {@link Node}, що представляє плитку книги.
     */
    private Node createBookTile(Book book) {
        VBox tileContainer = new VBox(5); // Вертикальний контейнер з відступом 5px між елементами
        tileContainer.getStyleClass().add("book-tile"); // CSS клас для стилізації
        tileContainer.setPadding(new Insets(10)); // Внутрішні відступи
        tileContainer.setAlignment(Pos.TOP_CENTER); // Вирівнювання вмісту
        tileContainer.setPrefWidth(200);  // Бажана ширина плитки
        tileContainer.setMinWidth(170); // Мінімальна ширина плитки

        ImageView coverImageView = createCoverImageView(); // Створення ImageView для обкладинки
        loadTileCoverImage(book.getCoverImagePath(), coverImageView); // Завантаження зображення обкладинки

        Label titleLabel = createTitleLabel(book.getTitle()); // Мітка для назви
        Label authorLabel = createAuthorLabel(book.getAuthor()); // Мітка для автора

        Region spacer = new Region(); // Розпірка для заповнення простору
        VBox.setVgrow(spacer, Priority.ALWAYS); // Дозволяє розпірці розтягуватися

        VBox bottomDetails = createBottomDetails(book); // Нижня частина плитки з датою та кнопкою

        tileContainer.getChildren().addAll(coverImageView, titleLabel, authorLabel, spacer, bottomDetails);
        setupTileClickHandler(tileContainer, book); // Налаштування обробника кліку по плитці

        return tileContainer;
    }

    /**
     * Створює та налаштовує компонент {@link ImageView} для відображення обкладинки книги.
     * @return Налаштований {@link ImageView}.
     */
    private ImageView createCoverImageView() {
        ImageView coverImageView = new ImageView();
        coverImageView.setFitHeight(170); // Бажана висота обкладинки
        coverImageView.setFitWidth(110);  // Бажана ширина обкладинки
        coverImageView.setPreserveRatio(true); // Збереження пропорцій зображення
        coverImageView.getStyleClass().add("book-tile-cover"); // CSS клас
        return coverImageView;
    }

    /**
     * Створює та налаштовує мітку {@link Label} для відображення назви книги.
     * @param title Назва книги.
     * @return Налаштована {@link Label}.
     */
    private Label createTitleLabel(String title) {
        Label label = new Label(title);
        label.getStyleClass().add("book-tile-title"); // CSS клас
        label.setFont(Font.font("System", FontWeight.BOLD, 13)); // Шрифт
        label.setWrapText(true); // Дозвіл переносу тексту
        label.setMaxWidth(160);  // Максимальна ширина для коректного переносу
        label.setAlignment(Pos.CENTER_LEFT);
        label.setMaxWidth(Double.MAX_VALUE); // Дозволяє займати всю доступну ширину в батьківському контейнері
        return label;
    }

    /**
     * Створює та налаштовує мітку {@link Label} для відображення автора книги.
     * @param author Автор книги.
     * @return Налаштована {@link Label}.
     */
    private Label createAuthorLabel(String author) {
        Label label = new Label(author != null ? author : ""); // Якщо автор не вказаний, порожній рядок
        label.getStyleClass().add("book-tile-author"); // CSS клас
        label.setFont(Font.font("System", 11)); // Шрифт
        label.setWrapText(true); // Дозвіл переносу тексту
        label.setMaxWidth(160);  // Максимальна ширина
        label.setAlignment(Pos.CENTER_LEFT);
        label.setMaxWidth(Double.MAX_VALUE); // Дозволяє займати всю доступну ширину
        return label;
    }

    /**
     * Створює нижню частину плитки книги, що містить дату додавання та кнопку "Прочитано!".
     * @param book Книга, для якої створюється ця частина плитки.
     * @return {@link VBox} контейнер з елементами нижньої частини плитки.
     */
    private VBox createBottomDetails(Book book) {
        VBox container = new VBox(5); // Вертикальний контейнер з відступом
        container.setAlignment(Pos.CENTER_LEFT); // Вирівнювання
        VBox.setMargin(container, new Insets(5, 0, 0, 0)); // Зовнішній відступ зверху

        // Мітка для дати додавання
        Label dateLabel = new Label("Додано: " +
                (book.getDateAdded() != null ? book.getDateAdded().format(DATE_FORMATTER) : "N/A"));
        dateLabel.getStyleClass().add("book-tile-date");
        dateLabel.setFont(Font.font("System", 10));
        dateLabel.setMaxWidth(Double.MAX_VALUE);
        dateLabel.setAlignment(Pos.CENTER_LEFT);

        // Кнопка "Прочитано!"
        Button markReadButton = new Button("Прочитано!");
        markReadButton.getStyleClass().add("book-tile-mark-read-button");
        markReadButton.setMaxWidth(Double.MAX_VALUE); // Кнопка займає всю доступну ширину
        markReadButton.setOnAction(event -> handleMarkAsRead(book)); // Обробник натискання

        container.getChildren().addAll(dateLabel, markReadButton);
        return container;
    }

    /**
     * Налаштовує обробник кліку для плитки книги.
     * Клік по плитці (не по кнопці всередині неї) відкриває детальний вигляд книги.
     * @param tileContainer Контейнер плитки (VBox).
     * @param book Книга, що асоційована з плиткою.
     */
    private void setupTileClickHandler(VBox tileContainer, Book book) {
        tileContainer.setOnMouseClicked(event -> {
            // Перевірка, чи клік був не по кнопці "Прочитано!" або її дочірніх елементах
            if (!(event.getTarget() instanceof Button ||
                    (event.getTarget() instanceof Node && ((Node) event.getTarget()).getParent() instanceof Button))) {

                if (primaryController == null) {
                    log.warn("Неможливо показати деталі книги '{}': PrimaryController не встановлено.", book.getTitle());
                    showErrorDialog("Помилка відображення", "Неможливо відкрити деталі книги.",
                            "Основний контролер програми не доступний. Будь ласка, спробуйте перезавантажити програму.");
                    return;
                }

                log.debug("Клік по плитці книги: '{}'. Відкриття детального вигляду.", book.getTitle());
                primaryController.showBookDetailView(book, "wishlist_view.fxml"); // Передача шляху для повернення
            }
        });
    }

    /**
     * Завантажує зображення обкладинки для плитки книги.
     * Якщо файл обкладинки не знайдено або виникає помилка, встановлюється зображення-заглушка.
     * @param filename Ім'я файлу обкладинки.
     * @param imageView Компонент {@link ImageView} для відображення обкладинки.
     */
    private void loadTileCoverImage(String filename, ImageView imageView) {
        imageView.setImage(null); // Очищення попереднього зображення

        Image defaultImage = getDefaultCoverImage(); // Отримання зображення-заглушки

        if (filename == null || filename.isBlank()) {
            log.warn("Ім'я файлу обкладинки не вказано. Використовується стандартна обкладинка.");
            imageView.setImage(defaultImage);
            return;
        }

        try {
            Path fullImagePath = Paths.get(App.COVERS_DIRECTORY_NAME, filename);
            File imageFile = fullImagePath.toFile();
            if (imageFile.exists() && imageFile.isFile()) {
                // Завантаження зображення з оптимізацією розміру та фоновим завантаженням
                Image image = new Image(imageFile.toURI().toString(), 110, 170, true, true, true);
                if (image.isError()) {
                    log.error("Помилка під час завантаження зображення з файлу [{}]: {}",
                            fullImagePath, image.getException() != null ? image.getException().getMessage() : "Невідома помилка зображення");
                    imageView.setImage(defaultImage);
                } else {
                    imageView.setImage(image);
                    log.debug("Зображення обкладинки '{}' успішно завантажено.", filename);
                }
            } else {
                log.warn("Файл обкладинки не знайдено за шляхом: {}. Використовується стандартна обкладинка.", fullImagePath);
                imageView.setImage(defaultImage);
            }
        } catch (Exception e) {
            log.error("Непередбачена помилка під час завантаження зображення обкладинки з файлу '{}': {}", filename, e.getMessage(), e);
            imageView.setImage(defaultImage);
        }
    }

    /**
     * Отримує стандартне зображення-заглушку для обкладинки книги.
     * @return Об'єкт {@link Image}, що представляє заглушку, або {@code null} у разі помилки.
     */
    private Image getDefaultCoverImage() {
        try (InputStream is = getClass().getResourceAsStream(DEFAULT_COVER_PATH)) {
            if (is == null) {
                log.error("Не вдалося знайти ресурс стандартної обкладинки: {}", DEFAULT_COVER_PATH);
                return null; // Або створити порожнє зображення, якщо потрібно
            }
            return new Image(is);
        } catch (IOException e) {
            log.error("Помилка під час завантаження стандартної обкладинки з ресурсів: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Обробник події для кнопки пошуку або натискання Enter у полі пошуку.
     * Викликає метод {@link #loadBooks()} для оновлення списку книг.
     * @param event Подія дії (наприклад, клік по кнопці).
     */
    @FXML
    private void handleSearch(ActionEvent event) {
        log.debug("Виконано пошук (кнопка або Enter) у списку бажань. Пошуковий запит: '{}'", searchField.getText());
        loadBooks();
    }


    /**
     * Обробляє дію "Позначити як прочитану" для вибраної книги.
     * Відкриває діалогове вікно додавання/редагування книги, передаючи обрану книгу
     * для зміни її статусу та інших деталей.
     * @param book Книга, яку потрібно позначити як прочитану.
     */
    private void handleMarkAsRead(Book book) {
        if (primaryController == null) {
            log.warn("Неможливо позначити книгу '{}' як прочитану: PrimaryController не встановлено.", book.getTitle());
            showErrorDialog("Помилка операції", "Неможливо відкрити вікно для позначки книги прочитаною.",
                    "Основний контролер програми не доступний.");
            return;
        }
        log.debug("Ініціювання позначення книги '{}' (ID: {}) як прочитаної. Відкриття діалогу редагування.", book.getTitle(), book.getId());
        // Передача книги в діалог редагування дозволить змінити статус та, за потреби, дату прочитання.
        primaryController.showAddEditBookDialog(book);
    }

    /**
     * Обробляє видалення книги зі списку бажань.
     * Показує діалог підтвердження перед видаленням. Якщо користувач підтверджує,
     * книга видаляється з бази даних, а також видаляється пов'язаний файл обкладинки.
     * Цей метод може бути викликаний, наприклад, з контекстного меню плитки або з детального вигляду книги.
     * @param book Книга, яку потрібно видалити.
     */
    private void handleDeleteBook(Book book) {
        if (book == null) {
            log.warn("Спроба видалити null книгу.");
            return;
        }
        log.debug("Ініціювання видалення книги '{}' (ID: {}) зі списку бажань.", book.getTitle(), book.getId());

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(getOwnerWindow());
        alert.setTitle("Підтвердження видалення");
        alert.setHeaderText("Ви дійсно хочете видалити книгу \"" + book.getTitle() + "\" зі списку бажань?");
        alert.setContentText("Цю дію неможливо буде скасувати. Файл обкладинки (якщо існує) також буде видалено.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            log.info("Користувач підтвердив видалення книги ID={}", book.getId());
            try {
                if (book.getCoverImagePath() != null && !book.getCoverImagePath().isBlank()) {
                    deleteCoverFile(book.getCoverImagePath()); // Видалення файлу обкладинки
                }
                bookDao.deleteBook(book.getId()); // Видалення книги з БД
                log.info("Книгу ID={} успішно видалено з БД та файлової системи (обкладинка).", book.getId());
                loadBooks(); // Оновлення списку плиток для відображення змін
            } catch (Exception e) {
                log.error("Помилка під час видалення книги ID={}: {}", book.getId(), e.getMessage(), e);
                showErrorDialog("Помилка Видалення", "Не вдалося видалити книгу \"" + book.getTitle() + "\".", e.getMessage());
            }
        } else {
            log.debug("Видалення книги ID={} скасовано користувачем.", book.getId());
        }
    }

    /**
     * Видаляє файл обкладинки книги з файлової системи.
     * @param filename Ім'я файлу обкладинки.
     */
    private void deleteCoverFile(String filename) {
        if (filename == null || filename.isBlank()) {
            log.debug("Ім'я файлу обкладинки для видалення не вказано або порожнє.");
            return;
        }
        try {
            Path filePath = Paths.get(App.COVERS_DIRECTORY_NAME, filename);
            if (Files.deleteIfExists(filePath)) {
                log.info("Файл обкладинки '{}' успішно видалено з файлової системи.", filePath);
            } else {
                log.warn("Файл обкладинки '{}' не знайдено для видалення (можливо, вже видалено або шлях невірний).", filePath);
            }
        } catch (IOException e) {
            log.error("Помилка під час видалення файлу обкладинки '{}': {}", filename, e.getMessage(), e);
        }
    }

    /**
     * Показує діалогове вікно з повідомленням про помилку.
     * @param title Заголовок вікна помилки.
     * @param header Заголовок повідомлення всередині вікна.
     * @param content Детальний текст повідомлення про помилку.
     */
    private void showErrorDialog(String title, String header, String content) {
        log.warn("Показ діалогу помилки: Title='{}', Header='{}', Content='{}'", title, header, content);
        Alert errorAlert = new Alert(Alert.AlertType.ERROR);
        errorAlert.initOwner(getOwnerWindow()); // Встановлення батьківського вікна
        errorAlert.setTitle(title);
        errorAlert.setHeaderText(header);
        errorAlert.setContentText(content != null ? content : "Деталі помилки не вказані.");
        errorAlert.showAndWait();
    }

    /**
     * Отримує батьківське вікно для відображення діалогових вікон.
     * Це необхідно для правильного позиціонування та модальності діалогів.
     * @return Об'єкт {@link javafx.stage.Window}, що представляє батьківське вікно, або {@code null}.
     */
    private javafx.stage.Window getOwnerWindow() {
        if (bookFlowPane != null && bookFlowPane.getScene() != null) {
            return bookFlowPane.getScene().getWindow();
        }
        // Резервний варіант, якщо bookFlowPane ще не має сцени (наприклад, на ранніх етапах ініціалізації)
        if (searchField != null && searchField.getScene() != null) {
            return searchField.getScene().getWindow();
        }
        log.warn("Не вдалося визначити батьківське вікно для діалогу в WishlistController. Діалог може з'явитися некоректно.");
        return null;
    }

    /**
     * Метод, що викликається для оновлення даних у поданні.
     * Реалізує метод з {@link BaseController}. У цьому випадку, просто перезавантажує список книг.
     */
    @Override
    public void refreshData() {
        log.info("Викликано refreshData() для WishlistController. Оновлення списку бажаних книг...");
        loadBooks();
    }
}