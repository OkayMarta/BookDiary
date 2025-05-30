package com.student.bookdiary.ui;

import com.student.bookdiary.model.Book;
import com.student.bookdiary.model.ReadingStatus;
import com.student.bookdiary.persistence.BookDao;
import com.student.bookdiary.persistence.DataAccessException; // Припускаємо, що цей виняток існує
import com.student.bookdiary.persistence.SqliteBookDao;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream; // Потрібно для getResourceAsStream
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Контролер для вікна детального перегляду інформації про книгу.
 * Відображає обкладинку, назву, автора, жанр, дати, рейтинг, коментар
 * та надає кнопки для редагування, видалення, додавання в улюблене та повернення назад.
 */
public class BookDetailController {

    private static final Logger log = LoggerFactory.getLogger(BookDetailController.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final String DEFAULT_COVER_IMAGE_PATH = "/com/student/bookdiary/ui/icons/default_book_cover.png";
    private static final int COVER_DISPLAY_WIDTH = 400;  // Бажана ширина обкладинки для відображення
    private static final int COVER_DISPLAY_HEIGHT = 600; // Бажана висота обкладинки для відображення


    // --- Поля, ін'єктовані з FXML ---
    @FXML private ImageView coverImageView;
    @FXML private Label titleLabel;
    @FXML private Label authorLabel;
    @FXML private Label genreLabel;
    @FXML private HBox ratingBoxContainer;
    @FXML private HBox ratingBox; // Контейнер для зірочок рейтингу
    @FXML private Label dateReadLabel;
    @FXML private Label dateAddedLabel;
    @FXML private TextArea commentArea;
    @FXML private Button backButton;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private Button favoriteButton;

    // --- Внутрішні поля класу ---
    private Book currentBook; // Поточна книга, деталі якої відображаються
    private PrimaryController primaryController; // Посилання на головний контролер для навігації
    private String returnViewFxml; // Шлях до FXML-файлу попереднього вигляду для кнопки "Назад"
    private BookDao bookDao; // Об'єкт доступу до даних для книг

    /**
     * Метод ініціалізації контролера. Викликається JavaFX після завантаження FXML.
     * Встановлює об'єкт DAO та налаштовує TextArea для перенесення тексту.
     */
    @FXML
    private void initialize() {
        log.debug("Ініціалізація BookDetailController...");
        this.bookDao = new SqliteBookDao(); // Створення екземпляра DAO
        commentArea.setWrapText(true); // Ввімкнення автоматичного перенесення тексту в полі коментаря
        log.debug("BookDetailController успішно ініціалізовано.");
    }

    /**
     * Встановлює дані книги для відображення на формі.
     * Цей метод викликається з {@link PrimaryController} після завантаження FXML-файлу цього вигляду.
     *
     * @param book Книга, деталі якої потрібно відобразити.
     * @param primaryController Посилання на головний контролер програми.
     * @param returnViewFxml Шлях до FXML-файлу попереднього вигляду, на який відбудеться повернення при натисканні кнопки "Назад".
     */
    public void setData(Book book, PrimaryController primaryController, String returnViewFxml) {
        this.currentBook = book;
        this.primaryController = primaryController;
        this.returnViewFxml = returnViewFxml;

        if (book == null) {
            log.error("Спроба відобразити деталі для неіснуючої книги (book is null). Повернення до попереднього вигляду.");
            // Показати повідомлення про помилку або просто повернутися назад
            showErrorAlertHelper("Помилка даних", "Не вдалося завантажити деталі книги.", "Об'єкт книги не передано.");
            handleBackButton(); // Спроба повернутися назад
            return;
        }

        log.info("Відображення деталей для книги ID={}, Назва='{}'", book.getId(), book.getTitle());

        loadCoverImage(); // Завантаження та відображення обкладинки

        // Заповнення основних текстових полів інформацією про книгу
        titleLabel.setText(book.getTitle());
        authorLabel.setText(book.getAuthor() != null && !book.getAuthor().isBlank() ? book.getAuthor() : "Автор невідомий");
        genreLabel.setText("Жанр: " + (book.getGenre() != null && !book.getGenre().isBlank() ? book.getGenre() : "Не вказано"));
        dateAddedLabel.setText("Додано: " + (book.getDateAdded() != null ? book.getDateAdded().format(DATE_FORMATTER) : "Невідомо"));
        commentArea.setText(book.getComment() != null ? book.getComment() : ""); // Встановлюємо порожній рядок, якщо коментар null

        // Налаштування видимості полів, що залежать від статусу "Прочитана"
        boolean isBookRead = book.getStatus() == ReadingStatus.READ;

        ratingBoxContainer.setVisible(isBookRead);
        ratingBoxContainer.setManaged(isBookRead);
        dateReadLabel.setVisible(isBookRead);
        dateReadLabel.setManaged(isBookRead);
        favoriteButton.setVisible(isBookRead); // Кнопка "Улюблене" видима тільки для прочитаних книг
        favoriteButton.setManaged(isBookRead);

        if (isBookRead) {
            populateRatingStars(book.getRating()); // Відображення рейтингу
            dateReadLabel.setText("Прочитано: " + (book.getDateRead() != null ? book.getDateRead().format(DATE_FORMATTER) : "Дата не вказана"));
            updateFavoriteButtonState(); // Оновлення вигляду кнопки "Улюблене"
        }
        log.debug("Дані для книги '{}' успішно встановлено на формі.", book.getTitle());
    }

    /**
     * Завантажує зображення обкладинки книги.
     * Якщо файл обкладинки вказано та існує, він завантажується.
     * В іншому випадку встановлюється зображення-заглушка.
     */
    private void loadCoverImage() {
        coverImageView.setImage(null); // Попереднє очищення
        String filename = (currentBook != null) ? currentBook.getCoverImagePath() : null;

        if (filename == null || filename.isBlank()) {
            log.warn("Файл обкладинки не вказано для книги ID={}. Встановлення зображення-заглушки.", (currentBook != null ? currentBook.getId() : "N/A"));
            coverImageView.setImage(loadDefaultImage());
            return;
        }

        try {
            Path coverFilePath = Paths.get(App.COVERS_DIRECTORY_NAME, filename);
            File imageFile = coverFilePath.toFile();

            if (imageFile.exists() && imageFile.isFile()) {
                log.debug("Завантаження обкладинки з файлу: {}", coverFilePath.toAbsolutePath());
                Image image = new Image(imageFile.toURI().toString(), COVER_DISPLAY_WIDTH, COVER_DISPLAY_HEIGHT, true, true); // Збереження пропорцій, плавне масштабування
                if (image.isError()) {
                    log.error("Помилка під час завантаження об'єкта Image з файлу '{}': {}", coverFilePath, image.getException().getMessage());
                    coverImageView.setImage(loadDefaultImage());
                } else {
                    coverImageView.setImage(image);
                    log.debug("Обкладинку '{}' успішно завантажено та відображено.", filename);
                }
            } else {
                log.warn("Файл обкладинки '{}' не знайдено за шляхом {}. Встановлення зображення-заглушки.", filename, coverFilePath.toAbsolutePath());
                coverImageView.setImage(loadDefaultImage());
            }
        } catch (Exception e) {
            log.error("Непередбачена помилка під час завантаження обкладинки '{}': {}", filename, e.getMessage(), e);
            coverImageView.setImage(loadDefaultImage());
        }
    }

    /**
     * Завантажує зображення-заглушку з ресурсів програми.
     * Використовується, якщо основний файл обкладинки відсутній або пошкоджений.
     *
     * @return Об'єкт {@link Image} із зображенням-заглушкою, або {@code null} у разі помилки завантаження.
     */
    private Image loadDefaultImage() {
        try (InputStream is = getClass().getResourceAsStream(DEFAULT_COVER_IMAGE_PATH)) {
            if (is == null) {
                log.error("Ресурс зображення-заглушки не знайдено за шляхом: {}", DEFAULT_COVER_IMAGE_PATH);
                return null;
            }
            // Завантажуємо заглушку з фіксованими розмірами, що відповідають бажаним розмірам відображення
            return new Image(is, COVER_DISPLAY_WIDTH, COVER_DISPLAY_HEIGHT, true, true);
        } catch (Exception e) {
            log.error("Помилка під час завантаження зображення-заглушки з ресурсів: {}", DEFAULT_COVER_IMAGE_PATH, e);
            return null;
        }
    }

    /**
     * Заповнює контейнер {@code ratingBox} зірочками для візуального відображення рейтингу книги.
     * @param rating Оцінка книги (від 0 до 5).
     */
    private void populateRatingStars(int rating) {
        if (ratingBox == null || ratingBoxContainer == null) {
            log.warn("Контейнери для рейтингу (ratingBox або ratingBoxContainer) не ініціалізовані.");
            return;
        }
        ratingBox.getChildren().clear(); // Очищення попередніх зірочок

        if (rating > 0 && rating <= 5) {
            ratingBoxContainer.setVisible(true);
            log.debug("Відображення рейтингу: {} зірочок.", rating);
            for (int i = 0; i < 5; i++) {
                Label starLabel = new Label(i < rating ? "★" : "☆"); // Заповнена або порожня зірочка
                starLabel.getStyleClass().add(i < rating ? "book-detail-star-filled" : "book-detail-star-empty");
                ratingBox.getChildren().add(starLabel);
            }
        } else {
            ratingBoxContainer.setVisible(true); // Показуємо контейнер, навіть якщо немає оцінки
            Label noRatingLabel = new Label("Без оцінки");
            noRatingLabel.getStyleClass().add("book-detail-no-rating-text"); // Стиль для тексту "Без оцінки"
            ratingBox.getChildren().add(noRatingLabel);
            log.debug("Рейтинг не встановлено (0 або недійсний). Відображено 'Без оцінки'.");
        }
    }

    /**
     * Оновлює вигляд кнопки "Улюблене" (текст та стиль) відповідно до поточного статусу "favorite" книги.
     */
    private void updateFavoriteButtonState() {
        if (currentBook != null && favoriteButton != null && favoriteButton.isVisible()) {
            boolean isFavorite = currentBook.isFavorite();
            favoriteButton.setText(isFavorite ? "♥ В улюблених" : "♡ Додати в улюблене");
            // Управління CSS-класом для зміни вигляду кнопки (наприклад, кольору іконки або фону)
            String activeFavoriteClass = "is-favorite"; // Цей клас має бути визначений у CSS
            if (isFavorite) {
                if (!favoriteButton.getStyleClass().contains(activeFavoriteClass)) {
                    favoriteButton.getStyleClass().add(activeFavoriteClass);
                }
            } else {
                favoriteButton.getStyleClass().remove(activeFavoriteClass);
            }
            log.trace("Оновлено стан кнопки 'Улюблене'. Поточний стан: {}", (isFavorite ? "улюблене" : "не улюблене"));
        }
    }

    /**
     * Обробляє натискання кнопки "Назад".
     * Повертає користувача до попереднього вигляду, вказаного у {@code returnViewFxml}.
     */
    @FXML
    private void handleBackButton() {
        log.info("Натиснуто кнопку 'Назад'. Повернення до вигляду: {}", returnViewFxml);
        if (primaryController != null && returnViewFxml != null) {
            primaryController.loadView(returnViewFxml);
        } else {
            log.error("Неможливо повернутися назад: primaryController ({}) або returnViewFxml ({}) не встановлено.",
                    primaryController, returnViewFxml);
            showErrorAlertHelper("Помилка навігації", "Не вдалося повернутися до попереднього екрану.", null);
        }
    }

    /**
     * Обробляє натискання кнопки "Редагувати".
     * Відкриває діалогове вікно для редагування поточної книги.
     * Після закриття діалогу оновлює дані на поточній сторінці деталей.
     */
    @FXML
    private void handleEditButton() {
        if (currentBook == null || primaryController == null) {
            log.error("Неможливо відкрити редагування: книга або головний контролер не ініціалізовані.");
            return;
        }
        log.info("Натиснуто кнопку 'Редагувати' для книги ID={}", currentBook.getId());
        primaryController.showAddEditBookDialog(currentBook); // Відкриття діалогу редагування

        // Оновлення даних на цій сторінці після закриття діалогу редагування
        // Завантажуємо оновлену книгу з БД, щоб відобразити можливі зміни
        bookDao.getBookById(currentBook.getId()).ifPresentOrElse(
                updatedBook -> {
                    log.debug("Оновлення даних на сторінці деталей після редагування книги ID={}", updatedBook.getId());
                    setData(updatedBook, primaryController, returnViewFxml);
                },
                () -> {
                    log.warn("Не вдалося знайти книгу ID={} після спроби редагування. Можливо, її було видалено.", currentBook.getId());
                    handleBackButton(); // Якщо книгу не знайдено, повертаємося назад
                }
        );
    }

    /**
     * Обробляє натискання кнопки "Видалити".
     * Показує діалог підтвердження та, у разі згоди користувача, видаляє книгу з БД та її обкладинку.
     */
    @FXML
    private void handleDeleteButton() {
        if (currentBook == null) {
            log.warn("Спроба видалити неіснуючу книгу (currentBook is null).");
            return;
        }
        log.info("Ініціювання видалення книги ID={}, Назва='{}'", currentBook.getId(), currentBook.getTitle());

        Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationAlert.initOwner(getOwnerWindow());
        confirmationAlert.setTitle("Підтвердження видалення");
        confirmationAlert.setHeaderText("Видалити книгу \"" + currentBook.getTitle() + "\" повністю?");
        confirmationAlert.setContentText("Цю дію неможливо буде скасувати. Всі дані про книгу, включаючи коментарі та оцінки, будуть втрачені.");

        Optional<ButtonType> result = confirmationAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            log.info("Користувач підтвердив видалення книги ID={}", currentBook.getId());
            try {
                deleteCoverFile(currentBook.getCoverImagePath()); // Спочатку видаляємо файл обкладинки
                bookDao.deleteBook(currentBook.getId()); // Потім видаляємо запис з БД
                log.info("Книгу ID={} та її обкладинку (якщо була) успішно видалено.", currentBook.getId());
                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.initOwner(getOwnerWindow());
                successAlert.setTitle("Видалення успішне");
                successAlert.setHeaderText("Книгу \"" + currentBook.getTitle() + "\" було успішно видалено.");
                successAlert.showAndWait();
                handleBackButton(); // Повернення до попереднього списку
            } catch (DataAccessException e) { // Обробка власного винятку
                log.error("Помилка під час видалення книги ID={} з БД.", currentBook.getId(), e);
                showErrorAlertHelper("Помилка Видалення", "Не вдалося видалити книгу з бази даних.", e.getMessage());
            } catch (Exception e) { // Обробка інших можливих помилок (наприклад, при видаленні файлу)
                log.error("Непередбачена помилка під час повного видалення книги ID={}", currentBook.getId(), e);
                showErrorAlertHelper("Помилка Видалення", "Сталася непередбачена помилка під час видалення книги.", e.getMessage());
            }
        } else {
            log.debug("Видалення книги ID={} скасовано користувачем.", currentBook.getId());
        }
    }

    /**
     * Обробляє натискання кнопки "Улюблене".
     * Змінює статус "favorite" для поточної книги та оновлює запис у БД та вигляд кнопки.
     * Доступно тільки для прочитаних книг.
     */
    @FXML
    private void handleFavoriteButton() {
        if (currentBook == null) {
            log.warn("Спроба змінити статус улюбленого для неіснуючої книги.");
            return;
        }
        if (currentBook.getStatus() != ReadingStatus.READ) {
            log.warn("Спроба змінити статус 'Улюблене' для непрочитаної книги ID={}. Дію скасовано.", currentBook.getId());
            // Кнопка не повинна бути видимою, але ця перевірка є додатковим захистом.
            return;
        }

        boolean newFavoriteStatus = !currentBook.isFavorite();
        log.info("Зміна статусу 'Улюблене' для книги ID={} на: {}", currentBook.getId(), newFavoriteStatus);
        try {
            currentBook.setFavorite(newFavoriteStatus);
            bookDao.updateBook(currentBook);
            log.info("Статус 'Улюбленe' для книги ID={} успішно оновлено в БД на {}.", currentBook.getId(), newFavoriteStatus);
            updateFavoriteButtonState(); // Оновлення тексту/стилю кнопки
        } catch (DataAccessException e) {
            log.error("Помилка оновлення статусу 'Улюблене' для книги ID={} в БД. Відкат зміни в об'єкті.", currentBook.getId(), e);
            currentBook.setFavorite(!newFavoriteStatus); // Відкат зміни в об'єкті, якщо БД не оновилася
            showErrorAlertHelper("Помилка Оновлення", "Не вдалося змінити статус 'Улюблене'.", e.getMessage());
        }
    }

    // --- Допоміжні приватні методи ---

    /**
     * Видаляє файл обкладинки з директорії, визначеної в {@link App#COVERS_DIRECTORY_NAME}.
     * @param filename Ім'я файлу для видалення. Якщо null або порожнє, нічого не робить.
     */
    private void deleteCoverFile(String filename) {
        if (filename == null || filename.isBlank()) {
            log.trace("Ім'я файлу для видалення обкладинки не вказано або порожнє.");
            return;
        }
        try {
            Path filePath = Paths.get(App.COVERS_DIRECTORY_NAME, filename);
            if (Files.deleteIfExists(filePath)) {
                log.info("Файл обкладинки '{}' успішно видалено.", filePath.toAbsolutePath());
            } else {
                log.warn("Файл обкладинки '{}' не знайдено для видалення.", filePath.toAbsolutePath());
            }
        } catch (IOException e) {
            log.error("Помилка під час видалення файлу обкладинки '{}'", filename, e);
            // Тут можна вирішити, чи потрібно повідомляти користувача.
            // Зазвичай, якщо це частина видалення книги, основне повідомлення про помилку буде від видалення з БД.
        }
    }

    /**
     * Допоміжний метод для відображення діалогового вікна з повідомленням про помилку.
     * @param title Заголовок вікна.
     * @param header Текст заголовка повідомлення.
     * @param content Текст основного вмісту повідомлення.
     */
    private void showErrorAlertHelper(String title, String header, String content) {
        if (primaryController != null) { // Перевірка, чи головний контролер доступний
            primaryController.showErrorAlert(title, header, content);
        } else {
            // Резервний варіант, якщо primaryController недоступний
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.initOwner(getOwnerWindow());
            errorAlert.setTitle(title);
            errorAlert.setHeaderText(header);
            errorAlert.setContentText(content != null ? content : "Деталі не вказані.");
            errorAlert.showAndWait();
        }
    }

    /**
     * Допоміжний метод для отримання поточного вікна (Stage), якому належить сцена цього контролера.
     * @return Об'єкт {@link Stage} поточного вікна, або {@code null}, якщо вікно не знайдено.
     */
    private Window getOwnerWindow() {
        // Спроба отримати вікно через будь-який гарантовано існуючий елемент на формі
        if (backButton != null && backButton.getScene() != null) {
            return backButton.getScene().getWindow();
        }
        log.warn("Не вдалося визначити батьківське вікно для Alert у BookDetailController. Alert може бути немодальним.");
        return null; // Якщо вікно не знайдено
    }
}