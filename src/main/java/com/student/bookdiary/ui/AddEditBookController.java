package com.student.bookdiary.ui;

import com.student.bookdiary.model.Book;
import com.student.bookdiary.model.ReadingStatus;
import com.student.bookdiary.persistence.BookDao;
import com.student.bookdiary.persistence.DataAccessException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream; // Потрібно для getResourceAsStream
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

/**
 * Контролер для діалогового вікна додавання або редагування книги.
 * Керує введенням даних про книгу, вибором обкладинки та збереженням змін.
 */
public class AddEditBookController {

    private static final Logger log = LoggerFactory.getLogger(AddEditBookController.class);

    // --- Поля, ін'єктовані з FXML ---
    @FXML private TextField titleField;
    @FXML private TextField authorField;
    @FXML private ComboBox<String> genreComboBox;
    @FXML private ComboBox<ReadingStatus> statusComboBox;
    @FXML private Label dateReadLabel;
    @FXML private DatePicker dateReadPicker;
    @FXML private Label ratingLabel;
    @FXML private ComboBox<Integer> ratingComboBox;
    @FXML private Label commentLabel;
    @FXML private TextArea commentArea;
    @FXML private Button selectCoverButton;
    @FXML private Label coverPathLabel;
    @FXML private ImageView coverImageView;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;
    @FXML private Button deleteCoverButton;

    // --- Внутрішні поля класу ---
    private BookDao bookDao;
    private Book bookToEdit; // Книга для редагування; null, якщо додається нова
    private Stage dialogStage; // Посилання на вікно (Stage) цього діалогу
    private Runnable refreshCallback; // Callback для оновлення основного списку книг
    private String targetCoverFilename; // Ім'я файлу обкладинки, яке буде збережено в БД
    private String originalCoverSourcePath; // Повний шлях до оригінального файлу нової обкладинки на диску

    /**
     * Метод ініціалізації контролера. Викликається JavaFX після завантаження FXML.
     * Налаштовує елементи керування: заповнює випадаючі списки,
     * встановлює слухачі подій та початкову видимість елементів.
     */
    @FXML
    private void initialize() {
        log.debug("Ініціалізація AddEditBookController...");

        // Налаштування випадаючого списку для статусів читання
        statusComboBox.setItems(FXCollections.observableArrayList(ReadingStatus.values()));
        statusComboBox.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(ReadingStatus status) {
                return status == null ? "" : status.getDisplayName();
            }
            @Override public ReadingStatus fromString(String string) {
                // Конвертація з рядка не потрібна для нередагованого ComboBox
                return null;
            }
        });

        // Налаштування випадаючого списку для оцінок
        ObservableList<Integer> ratings = FXCollections.observableArrayList(0); // 0 - "Без оцінки"
        ratings.addAll(IntStream.rangeClosed(1, 5).boxed().toList()); // Оцінки від 1 до 5
        ratingComboBox.setItems(ratings);
        ratingComboBox.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Integer rating) {
                return (rating == null || rating == 0) ? "Без оцінки" : rating + " ★";
            }
            @Override public Integer fromString(String string) {
                return null;
            }
        });

        genreComboBox.setEditable(true); // Дозволяє користувачу вводити власний жанр

        // Слухач для зміни видимості полів "Дата прочитання" та "Оцінка"
        // залежно від обраного статусу книги.
        statusComboBox.valueProperty().addListener((_, _, newStatus) -> {
            updateFieldsVisibility(newStatus == ReadingStatus.READ);
        });

        updateFieldsVisibility(false); // Початково приховати поля для прочитаної книги
        // Видимість кнопки deleteCoverButton буде встановлена в setBookToEdit
    }

    /**
     * Оновлює видимість та стан кнопки видалення обкладинки.
     * Кнопка видима, якщо для книги встановлено ім'я файлу обкладинки (targetCoverFilename).
     */
    private void updateDeleteCoverButtonVisibility() {
        boolean isCoverEffectivelySelected = (targetCoverFilename != null && !targetCoverFilename.isBlank());
        log.debug("Оновлення видимості кнопки видалення обкладинки. targetCoverFilename: '{}', видима: {}",
                targetCoverFilename, isCoverEffectivelySelected);

        if (deleteCoverButton != null) {
            deleteCoverButton.setVisible(isCoverEffectivelySelected);
            deleteCoverButton.setManaged(isCoverEffectivelySelected);
        } else {
            log.warn("deleteCoverButton не ініціалізовано (null) під час спроби оновити видимість.");
        }
    }

    /**
     * Оновлює видимість та доступність полів, специфічних для статусу "Прочитана".
     * @param isRead true, якщо обрано статус "Прочитана", false в іншому випадку.
     */
    private void updateFieldsVisibility(boolean isRead) {
        dateReadLabel.setVisible(isRead);
        dateReadPicker.setVisible(isRead);
        dateReadPicker.setManaged(isRead);
        dateReadPicker.setDisable(!isRead);

        ratingLabel.setVisible(isRead);
        ratingComboBox.setVisible(isRead);
        ratingComboBox.setManaged(isRead);
        ratingComboBox.setDisable(!isRead);

        if (!isRead) {
            dateReadPicker.setValue(null);
            ratingComboBox.setValue(0); // "Без оцінки"
        }
    }

    /**
     * Встановлює посилання на вікно (Stage) цього діалогу.
     * @param dialogStage Вікно діалогу.
     */
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    /**
     * Встановлює об'єкт доступу до даних (DAO) для книг.
     * @param bookDao Реалізація {@link BookDao}.
     */
    public void setBookDao(BookDao bookDao) {
        this.bookDao = bookDao;
    }

    /**
     * Встановлює callback-функцію, яка буде викликана після успішного збереження книги.
     * Зазвичай використовується для оновлення списку книг у головному вікні.
     * @param refreshCallback Функція для оновлення.
     */
    public void setRefreshCallback(Runnable refreshCallback) {
        this.refreshCallback = refreshCallback;
    }

    /**
     * Встановлює книгу для редагування або ініціалізує форму для додавання нової книги.
     * Якщо {@code book} дорівнює null, форма налаштовується для створення нової книги.
     * В іншому випадку, поля форми заповнюються даними з {@code book}.
     *
     * @param book Книга для редагування, або null для додавання нової.
     */
    public void setBookToEdit(Book book) {
        this.bookToEdit = book;
        this.originalCoverSourcePath = null; // Скидаємо шлях до нового файлу

        loadGenresIntoComboBox(); // Завантажуємо доступні жанри

        if (book != null) { // Режим редагування або позначення як прочитаної
            dialogStage.setTitle(book.getStatus() == ReadingStatus.WANT_TO_READ ?
                    "Позначити книгу як прочитану" : "Редагувати книгу");
            titleField.setText(book.getTitle());
            authorField.setText(book.getAuthor());
            genreComboBox.setValue(book.getGenre());
            commentArea.setText(book.getComment());
            this.targetCoverFilename = book.getCoverImagePath(); // Зберігаємо поточне ім'я файлу обкладинки

            if (book.getStatus() == ReadingStatus.WANT_TO_READ) {
                statusComboBox.setValue(ReadingStatus.READ); // Автоматично встановлюємо "Прочитана"
                dateReadPicker.setValue(LocalDate.now());    // Поточна дата
                ratingComboBox.setValue(0);                  // Без оцінки за замовчуванням
            } else {
                statusComboBox.setValue(book.getStatus());
                dateReadPicker.setValue(book.getDateRead());
                ratingComboBox.setValue(book.getRating());
            }
            updateFieldsVisibility(statusComboBox.getValue() == ReadingStatus.READ);
            loadAndDisplayImage(this.targetCoverFilename); // Завантажуємо існуючу обкладинку
            coverPathLabel.setText(this.targetCoverFilename != null && !this.targetCoverFilename.isBlank() ?
                    this.targetCoverFilename : "(не обрано)");

        } else { // Режим додавання нової книги
            dialogStage.setTitle("Додати нову книгу");
            titleField.clear();
            authorField.clear();
            genreComboBox.setValue("");
            statusComboBox.getSelectionModel().selectFirst(); // За замовчуванням перший статус (наприклад, "Хочу прочитати")
            dateReadPicker.setValue(null);
            ratingComboBox.setValue(0);
            commentArea.clear();
            coverPathLabel.setText("(не обрано)");
            coverImageView.setImage(loadDefaultImage()); // Встановлюємо заглушку
            this.targetCoverFilename = null;

            updateFieldsVisibility(statusComboBox.getValue() == ReadingStatus.READ);
        }
        updateDeleteCoverButtonVisibility(); // Оновлюємо видимість кнопки видалення обкладинки
    }

    /**
     * Обробляє натискання кнопки "Зберегти".
     * Валідує введені дані, обробляє файл обкладинки,
     * створює новий або оновлює існуючий об'єкт {@link Book} та зберігає його в БД.
     *
     * @param event Подія натискання кнопки.
     */
    @FXML
    private void handleSave(ActionEvent event) {
        if (!isInputValid()) {
            return;
        }

        String oldCoverFilenameInDb = (bookToEdit != null) ? bookToEdit.getCoverImagePath() : null;
        String finalCoverImageNameToSaveInDb = oldCoverFilenameInDb;

        // Обробка файлу обкладинки
        if (originalCoverSourcePath != null && targetCoverFilename != null) {
            // Новий файл обрано для завантаження
            Path source = Paths.get(originalCoverSourcePath);
            Path targetDir = Paths.get(App.COVERS_DIRECTORY_NAME);
            try {
                if (!Files.exists(targetDir)) {
                    Files.createDirectories(targetDir);
                    log.info("Створено директорію для обкладинок: {}", targetDir.toAbsolutePath());
                }
                Path target = targetDir.resolve(targetCoverFilename);
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                log.info("Файл обкладинки '{}' скопійовано до '{}'", originalCoverSourcePath, target);
                finalCoverImageNameToSaveInDb = targetCoverFilename;

                if (oldCoverFilenameInDb != null && !oldCoverFilenameInDb.equals(targetCoverFilename)) {
                    deleteCoverFile(oldCoverFilenameInDb); // Видаляємо стару обкладинку, якщо вона змінилася
                }
            } catch (IOException e) {
                log.error("Помилка копіювання файлу обкладинки з '{}' до '{}'", source, targetDir.resolve(targetCoverFilename), e);
                showErrorAlert("Помилка файлу", "Не вдалося зберегти файл обкладинки.", e.getMessage());
                return; // Перериваємо збереження, якщо не вдалося обробити обкладинку
            }
        } else if (targetCoverFilename == null && oldCoverFilenameInDb != null) {
            // Обкладинку було видалено користувачем
            deleteCoverFile(oldCoverFilenameInDb);
            finalCoverImageNameToSaveInDb = null;
        }
        // Якщо обкладинка не змінювалася, finalCoverImageNameToSaveInDb залишається oldCoverFilenameInDb

        // Створення або оновлення об'єкта Book
        Book bookToPersist = (bookToEdit == null) ? new Book() : bookToEdit;

        bookToPersist.setTitle(titleField.getText().trim());
        bookToPersist.setAuthor(authorField.getText().trim());
        String genreValue = genreComboBox.getEditor().getText(); // Отримуємо текст з редактора ComboBox
        bookToPersist.setGenre(genreValue != null && !genreValue.trim().isEmpty() ? genreValue.trim() : null);
        bookToPersist.setStatus(statusComboBox.getValue());
        bookToPersist.setComment(commentArea.getText() != null ? commentArea.getText().trim() : null);
        bookToPersist.setCoverImagePath(finalCoverImageNameToSaveInDb);

        if (statusComboBox.getValue() == ReadingStatus.READ) {
            bookToPersist.setDateRead(dateReadPicker.getValue());
            bookToPersist.setRating(ratingComboBox.getValue() != null ? ratingComboBox.getValue() : 0);
        } else {
            bookToPersist.setDateRead(null);
            bookToPersist.setRating(0);
        }

        if (bookToEdit == null) { // Якщо це нова книга
            bookToPersist.setDateAdded(LocalDate.now());
            bookToPersist.setFavorite(false); // Нові книги за замовчуванням не улюблені
        }
        // Поле 'favorite' для існуючої книги тут не змінюється, це робиться в іншому місці UI.

        // Збереження в БД
        try {
            if (bookToEdit == null) {
                bookDao.addBook(bookToPersist);
                log.info("Нову книгу '{}' успішно додано.", bookToPersist.getTitle());
            } else {
                bookDao.updateBook(bookToPersist);
                log.info("Книгу '{}' (ID={}) успішно оновлено.", bookToPersist.getTitle(), bookToPersist.getId());
            }

            if (refreshCallback != null) {
                refreshCallback.run();
            }
            dialogStage.close();

        } catch (DataAccessException e) { // Використовуємо власний виняток
            log.error("Помилка збереження книги в БД: {}", bookToPersist.getTitle(), e);
            showErrorAlert("Помилка Бази Даних", "Не вдалося зберегти книгу.", e.getMessage());
            // Якщо файл було скопійовано, але сталася помилка БД, можливо, варто розглянути відкат копіювання файлу.
            // Наприклад, якщо finalCoverImageNameToSaveInDb відрізняється від oldCoverFilenameInDb і є новим.
            if (originalCoverSourcePath != null && finalCoverImageNameToSaveInDb != null && finalCoverImageNameToSaveInDb.equals(targetCoverFilename)) {
                log.warn("Спроба видалити нещодавно скопійований файл обкладинки через помилку збереження в БД: {}", targetCoverFilename);
                deleteCoverFile(targetCoverFilename); // Видаляємо щойно скопійований файл
            }
        }
    }


    /**
     * Обробляє натискання кнопки "Скасувати". Закриває діалогове вікно.
     * @param event Подія натискання кнопки.
     */
    @FXML
    private void handleCancel(ActionEvent event) {
        log.debug("Натиснуто кнопку 'Скасувати'. Закриття діалогового вікна.");
        dialogStage.close();
    }

    /**
     * Обробляє натискання кнопки "Обрати файл..." для обкладинки.
     * Відкриває стандартний діалог вибору файлу зображення.
     *
     * @param event Подія натискання кнопки.
     */
    @FXML
    private void handleSelectCover(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Обрати обкладинку книги");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Зображення", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"),
                new FileChooser.ExtensionFilter("Всі файли", "*.*")
        );
        File selectedFile = fileChooser.showOpenDialog(dialogStage);

        if (selectedFile != null) {
            log.info("Користувач обрав файл обкладинки: {}", selectedFile.getAbsolutePath());
            this.originalCoverSourcePath = selectedFile.getAbsolutePath();
            String originalFileName = selectedFile.getName();
            String extension = getFileExtension(originalFileName);
            this.targetCoverFilename = UUID.randomUUID().toString() + extension; // Генеруємо унікальне ім'я файлу

            coverPathLabel.setText(originalFileName); // Показуємо користувачу оригінальне ім'я
            loadAndDisplayImage(this.originalCoverSourcePath); // Завантажуємо прев'ю
        } else {
            log.debug("Вибір файлу обкладинки скасовано користувачем.");
        }
        updateDeleteCoverButtonVisibility(); // Оновлюємо видимість кнопки після будь-якої дії з вибором файлу
    }

    /**
     * Обробляє натискання кнопки "Видалити" для обкладинки.
     * Скидає інформацію про обрану обкладинку та оновлює UI.
     *
     * @param event Подія натискання кнопки.
     */
    @FXML
    private void handleDeleteCover(ActionEvent event) {
        log.info("Натиснуто кнопку 'Видалити обкладинку'.");
        this.originalCoverSourcePath = null;
        this.targetCoverFilename = null;
        coverImageView.setImage(loadDefaultImage()); // Встановлюємо зображення-заглушку
        coverPathLabel.setText("(не обрано)");
        updateDeleteCoverButtonVisibility();
    }


    /**
     * Отримує розширення файлу з його імені.
     * @param filename Ім'я файлу.
     * @return Розширення файлу з крапкою (наприклад, ".jpg") або порожній рядок.
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        return (lastDotIndex > 0 && lastDotIndex < filename.length() - 1)
                ? filename.substring(lastDotIndex).toLowerCase()
                : "";
    }

    /**
     * Завантажує зображення-заглушку для обкладинки з ресурсів.
     * @return Об'єкт {@link Image} заглушки або null, якщо не вдалося завантажити.
     */
    private Image loadDefaultImage() {
        String defaultCoverPath = "/com/student/bookdiary/ui/icons/default_book_cover.png";
        try (InputStream is = getClass().getResourceAsStream(defaultCoverPath)) {
            if (is == null) {
                log.error("Не вдалося знайти ресурс зображення-заглушки: {}", defaultCoverPath);
                return null;
            }
            return new Image(is);
        } catch (IOException | NullPointerException e) {
            log.error("Помилка завантаження зображення-заглушки з ресурсів: {}", defaultCoverPath, e);
            return null;
        }
    }

    /**
     * Завантажує та відображає зображення обкладинки в {@link ImageView}.
     * Якщо шлях не вказано або файл не знайдено, встановлює зображення-заглушку.
     *
     * @param imagePathOrFilename Повний шлях до файлу або ім'я файлу в папці 'covers'.
     */
    private void loadAndDisplayImage(String imagePathOrFilename) {
        if (coverImageView == null) {
            log.error("ImageView для обкладинки (coverImageView) не ініціалізовано (null).");
            return;
        }

        if (imagePathOrFilename == null || imagePathOrFilename.isBlank()) {
            log.debug("Шлях до файлу обкладинки не вказано, встановлюю зображення-заглушку.");
            coverImageView.setImage(loadDefaultImage());
            return;
        }

        File imageFile;
        Path path = Paths.get(imagePathOrFilename);

        // Визначаємо, чи це абсолютний шлях (новий файл) чи відносний (файл з папки covers)
        if (path.isAbsolute() || imagePathOrFilename.contains(File.separator) || imagePathOrFilename.contains(File.pathSeparator)) {
            imageFile = path.toFile();
            log.debug("Спроба завантажити обкладинку з вказаного шляху: {}", imagePathOrFilename);
        } else {
            // Припускаємо, що це ім'я файлу в директорії App.COVERS_DIRECTORY_NAME
            Path coverPath = Paths.get(App.COVERS_DIRECTORY_NAME, imagePathOrFilename);
            imageFile = coverPath.toFile();
            log.debug("Спроба завантажити обкладинку з директорії додатку: {}", coverPath);
        }

        if (imageFile.exists() && imageFile.isFile()) {
            try {
                Image image = new Image(imageFile.toURI().toString());
                if (!image.isError()) {
                    coverImageView.setImage(image);
                    log.debug("Зображення '{}' успішно завантажено та відображено.", imageFile.getName());
                } else {
                    log.error("Помилка під час завантаження зображення з файлу '{}': {}", imageFile.getPath(), image.getException().getMessage());
                    coverImageView.setImage(loadDefaultImage());
                }
            } catch (Exception e) {
                log.error("Не вдалося створити об'єкт Image з файлу '{}'", imageFile.getPath(), e);
                coverImageView.setImage(loadDefaultImage());
            }
        } else {
            log.warn("Файл зображення '{}' не знайдено або не є файлом. Встановлюю зображення-заглушку.", imageFile.getPath());
            coverImageView.setImage(loadDefaultImage());
        }
    }


    /**
     * Перевіряє валідність введених даних у формі.
     * @return true, якщо дані валідні, false - в іншому випадку (з показом Alert).
     */
    private boolean isInputValid() {
        StringBuilder errorMessage = new StringBuilder();

        if (titleField.getText() == null || titleField.getText().trim().isEmpty()) {
            errorMessage.append("Назва книги є обов'язковим полем!\n");
        }
        if (statusComboBox.getValue() == null) {
            errorMessage.append("Не обрано статус книги!\n");
        }
        if (statusComboBox.getValue() == ReadingStatus.READ && dateReadPicker.getValue() == null) {
            errorMessage.append("Дата прочитання є обов'язковою для статусу 'Прочитана'!\n");
        }
        // Можна додати інші перевірки, наприклад, для формату року, оцінки тощо.

        if (errorMessage.isEmpty()) {
            return true;
        } else {
            log.warn("Помилка валідації введених даних: {}", errorMessage.toString().replace("\n", " "));
            showErrorAlert("Некоректні Дані", "Будь ласка, виправте наступні помилки:", errorMessage.toString());
            return false;
        }
    }

    /**
     * Відображає діалогове вікно з повідомленням про помилку.
     * @param title Заголовок вікна.
     * @param header Текст заголовка повідомлення.
     * @param content Текст основного вмісту повідомлення.
     */
    private void showErrorAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initOwner(dialogStage);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Завантажує список унікальних жанрів з БД та заповнює ними ComboBox.
     */
    private void loadGenresIntoComboBox() {
        if (bookDao == null) {
            log.warn("bookDao не ініціалізовано, неможливо завантажити жанри.");
            genreComboBox.setItems(FXCollections.emptyObservableList());
            return;
        }
        try {
            List<String> genres = bookDao.getDistinctGenres();
            genres.sort(String.CASE_INSENSITIVE_ORDER);
            String currentEditorText = genreComboBox.getEditor().getText(); // Зберігаємо поточний введений текст
            genreComboBox.setItems(FXCollections.observableArrayList(genres));
            if (currentEditorText != null && !currentEditorText.isBlank()) {
                genreComboBox.getEditor().setText(currentEditorText); // Відновлюємо текст, якщо він був
            }
            log.debug("Завантажено {} унікальних жанрів до ComboBox.", genres.size());
        } catch (DataAccessException e) {
            log.error("Не вдалося завантажити список жанрів для діалогу.", e);
            genreComboBox.setItems(FXCollections.emptyObservableList());
        }
    }

    /**
     * Видаляє файл обкладинки з директорії 'covers'.
     * @param filename Ім'я файлу для видалення.
     */
    private void deleteCoverFile(String filename) {
        if (filename == null || filename.isBlank()) {
            log.debug("Ім'я файлу для видалення обкладинки не вказано.");
            return;
        }
        try {
            Path coverPath = Paths.get(App.COVERS_DIRECTORY_NAME, filename); // Використовуємо константу з App
            if (Files.deleteIfExists(coverPath)) {
                log.info("Файл обкладинки '{}' успішно видалено.", coverPath);
            } else {
                log.warn("Файл обкладинки '{}' не знайдено для видалення.", coverPath);
            }
        } catch (IOException e) {
            log.error("Помилка під час видалення файлу обкладинки '{}'", filename, e);
        }
    }
}