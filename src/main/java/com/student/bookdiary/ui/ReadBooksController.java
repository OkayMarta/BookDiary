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
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Контролер для відображення списку прочитаних книг.
 * Відповідає за відображення, фільтрацію та сортування книг у вигляді плиток.
 */
public class ReadBooksController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(ReadBooksController.class);

    /** FXML елементи інтерфейсу */
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortComboBox;
    @FXML private FlowPane bookFlowPane;
    @FXML private ComboBox<String> genreFilterComboBox;

    /** Об'єкт для роботи з даними книг */
    private BookDao bookDao;
    private PrimaryController primaryController;

    /** Константи для опцій сортування */
    private static final String SORT_BY_DATE_READ_DESC = "Дата прочитання (новіші)";
    private static final String SORT_BY_DATE_READ_ASC = "Дата прочитання (старіші)";
    private static final String SORT_BY_TITLE = "Назва (А-Я)";
    private static final String SORT_BY_RATING_DESC = "Рейтинг (спадання)";
    private static final String SORT_BY_RATING_ASC = "Рейтинг (зростання)";
    private static final String ALL_GENRES_OPTION = "Всі жанри";

    /** Форматувач для відображення дат */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    /**
     * Встановлює посилання на головний контролер.
     * @param primaryController Головний контролер програми
     */
    public void setPrimaryController(PrimaryController primaryController) {
        this.primaryController = primaryController;
    }

    /**
     * Ініціалізує контролер після завантаження FXML.
     * Налаштовує початкові значення та обробники подій.
     */
    @FXML
    private void initialize() {
        log.info("Ініціалізація ReadBooksController...");
        this.bookDao = new SqliteBookDao();

        setupSortComboBox();
        setupGenreFilterComboBox();

        // Налаштування слухачів змін для автоматичного оновлення списку
        searchField.textProperty().addListener((observable, oldValue, newValue) -> loadBooks());
        sortComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) loadBooks();
        });
        genreFilterComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) loadBooks();
        });

        // Налаштування відступів для FlowPane
        bookFlowPane.setHgap(15);
        bookFlowPane.setVgap(15);
        bookFlowPane.setPadding(new Insets(15));

        loadBooks();
        log.info("ReadBooksController успішно ініціалізовано");
    }

    /**
     * Налаштовує ComboBox для сортування книг.
     */
    private void setupSortComboBox() {
        sortComboBox.getItems().addAll(
                SORT_BY_DATE_READ_DESC, SORT_BY_DATE_READ_ASC, SORT_BY_TITLE,
                SORT_BY_RATING_DESC, SORT_BY_RATING_ASC
        );
        sortComboBox.setValue(SORT_BY_DATE_READ_DESC);
    }

    /**
     * Завантажує та відображає список прочитаних книг з урахуванням фільтрів та сортування.
     */
    public void loadBooks() {
        log.debug("Завантаження списку прочитаних книг...");
        try {
            List<Book> books = bookDao.getBooksByStatus(ReadingStatus.READ);

            // Застосування фільтра пошуку
            String searchTerm = searchField.getText().trim().toLowerCase();
            if (!searchTerm.isEmpty()) {
                books = books.stream()
                        .filter(book -> book.getTitle().toLowerCase().contains(searchTerm) ||
                                (book.getAuthor() != null && book.getAuthor().toLowerCase().contains(searchTerm)))
                        .collect(Collectors.toList());
            }

            // Застосування фільтра за жанром
            String selectedGenre = genreFilterComboBox.getValue();
            if (selectedGenre != null && !selectedGenre.equals(ALL_GENRES_OPTION) && !selectedGenre.isBlank()) {
                books = books.stream()
                        .filter(book -> selectedGenre.equalsIgnoreCase(book.getGenre()))
                        .collect(Collectors.toList());
            }

            // Застосування сортування
            String sortOption = sortComboBox.getValue();
            if (sortOption != null) {
                Comparator<Book> comparator = switch (sortOption) {
                    case SORT_BY_DATE_READ_ASC -> Comparator.comparing(Book::getDateRead, Comparator.nullsLast(Comparator.naturalOrder()));
                    case SORT_BY_TITLE -> Comparator.comparing(Book::getTitle, String.CASE_INSENSITIVE_ORDER);
                    case SORT_BY_RATING_DESC -> Comparator.comparingInt(Book::getRating).reversed();
                    case SORT_BY_RATING_ASC -> Comparator.comparingInt(Book::getRating);
                    default -> Comparator.comparing(Book::getDateRead, Comparator.nullsLast(Comparator.reverseOrder()));
                };
                books.sort(comparator);
            }

            // Оновлення відображення
            bookFlowPane.getChildren().clear();
            for (Book book : books) {
                Node bookTileNode = createBookTile(book);
                if (bookTileNode != null) {
                    bookFlowPane.getChildren().add(bookTileNode);
                }
            }
            log.info("Список прочитаних книг оновлено. Відображено {} книг", bookFlowPane.getChildren().size());

        } catch (Exception e) {
            log.error("Помилка завантаження списку прочитаних книг", e);
            bookFlowPane.getChildren().clear();
            Label errorLabel = new Label("Не вдалося завантажити книги.");
            errorLabel.getStyleClass().add("error-text");
            bookFlowPane.getChildren().add(errorLabel);
            if (primaryController != null) {
                primaryController.showErrorAlert("Помилка", "Не вдалося завантажити список книг.", e.getMessage());
            }
        }
    }

    /**
     * Створює візуальне представлення книги у вигляді плитки.
     * @param book Книга для відображення
     * @return Node, що представляє плитку книги, або null у разі помилки
     */
    private Node createBookTile(Book book) {
        if (book == null) {
            log.warn("Спроба створити плитку для null книги");
            return null;
        }

        // Головний контейнер плитки
        VBox tileContainer = new VBox();
        tileContainer.getStyleClass().add("book-tile");
        tileContainer.setPrefWidth(200);
        tileContainer.setMinWidth(170);

        // Обкладинка книги
        ImageView coverImageView = new ImageView();
        coverImageView.getStyleClass().add("book-tile-cover");
        coverImageView.setFitHeight(170);
        coverImageView.setFitWidth(110);
        coverImageView.setPreserveRatio(true);
        loadTileCoverImage(book.getCoverImagePath(), coverImageView);

        // Контейнер для назви та автора
        VBox mainContentBox = new VBox(4);
        mainContentBox.setAlignment(Pos.TOP_CENTER);

        Label titleLabel = new Label(book.getTitle());
        titleLabel.getStyleClass().add("book-tile-title");

        Label authorLabel = new Label(book.getAuthor() != null ? book.getAuthor() : "");
        authorLabel.getStyleClass().add("book-tile-author");

        mainContentBox.getChildren().addAll(titleLabel, authorLabel);
        VBox.setVgrow(mainContentBox, Priority.ALWAYS);

        // Нижній блок з рейтингом та додатковою інформацією
        VBox overallBottomContainer = new VBox(4);
        overallBottomContainer.getStyleClass().add("book-tile-bottom-container");
        overallBottomContainer.setAlignment(Pos.CENTER);

        // Рейтинг
        HBox ratingRow = new HBox();
        ratingRow.getStyleClass().add("book-tile-rating-box");
        if (book.getRating() > 0) {
            for (int i = 0; i < 5; i++) {
                Label starLabel = new Label(i < book.getRating() ? "★" : "☆");
                starLabel.getStyleClass().add(i < book.getRating() ? "book-tile-star-filled" : "book-tile-star-empty");
                ratingRow.getChildren().add(starLabel);
            }
        } else {
            Label noRatingLabel = new Label("Без оцінки");
            noRatingLabel.getStyleClass().add("book-tile-no-rating");
            ratingRow.getChildren().add(noRatingLabel);
        }

        // Дата прочитання та кнопка "Улюблене"
        HBox dateFavoriteRow = new HBox();
        dateFavoriteRow.setAlignment(Pos.CENTER_LEFT);
        dateFavoriteRow.setSpacing(8);

        Label dateReadLabel = new Label(book.getDateRead() != null ? book.getDateRead().format(DATE_FORMATTER) : "");
        dateReadLabel.getStyleClass().add("book-tile-date");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button favoriteButton = new Button();
        favoriteButton.getStyleClass().add("book-tile-favorite-button");
        ImageView favoriteIconView = new ImageView();
        favoriteButton.setGraphic(favoriteIconView);
        
        updateFavoriteButtonStyle(favoriteButton, book.isFavorite(), favoriteIconView);

        favoriteButton.setOnAction(event -> handleToggleFavorite(book, favoriteButton, favoriteIconView));

        dateFavoriteRow.getChildren().addAll(dateReadLabel, spacer, favoriteButton);

        overallBottomContainer.getChildren().addAll(ratingRow, dateFavoriteRow);

        tileContainer.getChildren().addAll(coverImageView, mainContentBox, overallBottomContainer);

        // Обробник подвійного кліку для відкриття деталей
        tileContainer.setOnMouseClicked(event -> {
            Node target = (Node) event.getTarget();
            boolean clickedOnFavoriteButton = false;
            while (target != null && target != tileContainer) {
                if (target == favoriteButton) {
                    clickedOnFavoriteButton = true;
                    break;
                }
                target = target.getParent();
            }

            if (!clickedOnFavoriteButton && event.getClickCount() == 2) {
                if (primaryController != null) {
                    log.debug("Відкриття деталей книги: {}", book.getTitle());
                    primaryController.showBookDetailView(book, "read_books_view.fxml");
                }
            }
        });

        return tileContainer;
    }

    /**
     * Оновлює стиль кнопки "Улюблене" залежно від статусу книги.
     */
    private void updateFavoriteButtonStyle(Button favoriteButton, boolean isFavorite, ImageView iconView) {
        String iconPath;
        String tooltipText;

        if (isFavorite) {
            iconPath = "/com/student/bookdiary/ui/icons/heart_filled.png";
            tooltipText = "Видалити з улюбленого";
            favoriteButton.getStyleClass().remove("favorite-empty");
            favoriteButton.getStyleClass().add("favorite-filled");
        } else {
            iconPath = primaryController != null && primaryController.isDarkTheme() 
                ? "/com/student/bookdiary/ui/icons/heart_outline_black.png"
                : "/com/student/bookdiary/ui/icons/heart_outline_white.png";
            tooltipText = "Додати в улюблене";
            favoriteButton.getStyleClass().remove("favorite-filled");
            favoriteButton.getStyleClass().add("favorite-empty");
        }

        try {
            if (iconView == null) {
                log.error("ImageView для іконки улюбленого є null");
                return;
            }

            Image iconImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream(iconPath)));
            iconView.setImage(iconImage);
            iconView.setFitWidth(40);
            iconView.setFitHeight(40);
            iconView.setPreserveRatio(true);

            log.debug("Оновлено стиль кнопки 'Улюблене': isFavorite={}", isFavorite);

        } catch (NullPointerException | IllegalArgumentException e) {
            log.error("Помилка завантаження іконки улюбленого: {}", e.getMessage());
            if (iconView != null) {
                iconView.setImage(null);
            }
        }
        favoriteButton.setTooltip(new Tooltip(tooltipText));
    }

    /**
     * Завантажує зображення обкладинки для плитки книги.
     */
    private void loadTileCoverImage(String filename, ImageView imageView) {
        imageView.setImage(null);

        // Завантаження зображення-заглушки
        URL defaultCoverUrl = getClass().getResource("/com/student/bookdiary/ui/icons/default_book_cover.png");
        if (defaultCoverUrl == null) {
            log.error("Не вдалося знайти зображення-заглушку для обкладинки книги");
            return;
        }
        Image defaultImage = new Image(defaultCoverUrl.toString());

        if (filename == null || filename.isBlank()) {
            imageView.setImage(defaultImage);
            return;
        }

        try {
            Path fullImagePath = Paths.get(App.COVERS_DIRECTORY_NAME, filename);
            File imageFile = fullImagePath.toFile();

            if (imageFile.exists() && imageFile.isFile()) {
                Image image = new Image(imageFile.toURI().toString(), 110, 170, true, true, true);
                if (image.isError()) {
                    log.error("Помилка завантаження зображення для плитки [{}]: {}", fullImagePath, image.getException().getMessage());
                    imageView.setImage(defaultImage);
                } else {
                    imageView.setImage(image);
                }
            } else {
                log.warn("Файл обкладинки для плитки не знайдено: {}", fullImagePath);
                imageView.setImage(defaultImage);
            }
        } catch (Exception e) {
            log.error("Не вдалося завантажити зображення для плитки з файлу '{}': {}", filename, e.getMessage());
            imageView.setImage(defaultImage);
        }
    }

    /**
     * Обробляє зміну статусу "Улюблене" для книги.
     */
    private void handleToggleFavorite(Book book, Button favoriteButton, ImageView favoriteIconView) {
        if (book == null) return;
        boolean oldStatus = book.isFavorite();
        try {
            book.setFavorite(!oldStatus);
            bookDao.updateBook(book);
            log.info("Статус 'Улюблене' для книги ID={} змінено на {}", book.getId(), book.isFavorite());

            updateFavoriteButtonStyle(favoriteButton, book.isFavorite(), favoriteIconView);

        } catch (Exception e) {
            book.setFavorite(oldStatus);
            log.error("Помилка під час оновлення статусу 'Улюблене' для книги ID={}", book.getId(), e);
            if (primaryController != null) {
                primaryController.showErrorAlert("Помилка", "Не вдалося оновити статус книги.", e.getMessage());
            }
        }
    }

    /**
     * Налаштовує ComboBox для фільтрації за жанрами.
     */
    private void setupGenreFilterComboBox() {
        try {
            List<String> genres = bookDao.getDistinctGenres();
            List<String> genreOptionsList = new java.util.ArrayList<>();
            genreOptionsList.add(ALL_GENRES_OPTION);
            genres.stream()
                    .filter(g -> g != null && !g.isBlank())
                    .distinct()
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .forEach(genreOptionsList::add);

            genreFilterComboBox.setItems(FXCollections.observableArrayList(genreOptionsList));
            genreFilterComboBox.setValue(ALL_GENRES_OPTION);
        } catch (Exception e) {
            log.error("Не вдалося завантажити жанри для фільтра", e);
            genreFilterComboBox.setItems(FXCollections.observableArrayList(ALL_GENRES_OPTION));
            genreFilterComboBox.setValue(ALL_GENRES_OPTION);
        }
    }

    @Override
    public void refreshData() {
        log.info("Оновлення даних у ReadBooksController");
        loadBooks();
    }
}