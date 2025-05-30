package com.student.bookdiary.ui;

import com.student.bookdiary.persistence.DataAccessException; // Потрібно додати імпорт для власного винятку
import com.student.bookdiary.persistence.DatabaseManager;
import javafx.application.Application;
import javafx.application.Platform; // Для коректного виходу з програми у випадку критичної помилки
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.prefs.Preferences;

/**
 * Головний клас запуску JavaFX додатку "Book Diary".
 * Відповідає за ініціалізацію програми, завантаження основного вікна,
 * створення необхідних директорій та обробку критичних помилок запуску.
 */
public class App extends Application {

    private static final Logger log = LoggerFactory.getLogger(App.class);

    /**
     * Назва директорії для зберігання файлів обкладинок книг.
     * Директорія буде створена в кореневій папці програми, якщо вона відсутня.
     */
    public static String COVERS_DIRECTORY_NAME = "covers";

    /**
     * Назва вузла в системних налаштуваннях (Preferences API) для зберігання параметрів програми.
     */
    private static final String PREFERENCES_NODE_NAME = "com/student/bookdiary";

    /**
     * Ключ для зберігання обраної теми оформлення в Preferences.
     */
    private static final String PREFERENCES_THEME_KEY = "uiTheme";

    /**
     * Значення для світлої теми оформлення (використовується за замовчуванням).
     */
    private static final String THEME_LIGHT = "light";
    // private static final String THEME_DARK = "dark"; // Якщо потрібна константа для темної теми тут

    /**
     * Головний метод життєвого циклу JavaFX додатку.
     * Викликається після виклику методу {@code launch()}.
     * Ініціалізує базу даних, створює директорію для обкладинок,
     * завантажує основний FXML файл, налаштовує сцену та відображає головне вікно.
     *
     * @param stage Головний контейнер верхнього рівня (вікно програми).
     */
    @Override
    public void start(Stage stage) {
        log.info("Запуск застосунку Book Diary версії X.Y.Z..."); // Рекомендується вказати версію, якщо вона є

        try {
            ensureCoversDirectoryExists(); // Перевірка та створення папки для обкладинок

            log.info("Ініціалізація бази даних...");
            DatabaseManager.initializeDatabase(); // Ініціалізація БД (створення таблиць)
            log.info("Базу даних успішно ініціалізовано.");

            log.info("Завантаження основного інтерфейсу користувача (primary.fxml)...");
            // Отримання URL до FXML файлу з ресурсів
            URL fxmlLocation = App.class.getResource("/com/student/bookdiary/ui/primary.fxml");
            if (fxmlLocation == null) {
                // Критична помилка: основний FXML файл не знайдено
                log.error("Критична помилка: не вдалося знайти FXML файл 'primary.fxml'. Перевірте шлях та наявність файлу в ресурсах.");
                throw new IOException("Не вдалося знайти FXML файл: primary.fxml. Додаток не може бути запущено.");
            }

            FXMLLoader fxmlLoader = new FXMLLoader(fxmlLocation);
            Parent root = fxmlLoader.load();
            log.info("Основний FXML файл успішно завантажено.");

            // Створення сцени з рекомендованими розмірами
            Scene scene = new Scene(root, 1100, 700);

            // Застосування збереженої теми оформлення при старті програми
            Preferences prefs = Preferences.userRoot().node(PREFERENCES_NODE_NAME);
            String savedTheme = prefs.get(PREFERENCES_THEME_KEY, THEME_LIGHT); // Світла тема за замовчуванням
            log.info("Застосування збереженої теми: '{}'", savedTheme);
            PrimaryController.applyThemeToScene(scene, savedTheme); // Виклик статичного методу для застосування CSS

            // Додавання CSS-класу теми до кореневого елемента сцени
            // Це може бути корисним для специфічних стилів, що залежать від класу теми на root.
            if (PrimaryController.THEME_DARK.equals(savedTheme)) { // Використовуємо константу з PrimaryController
                root.getStyleClass().add("dark-theme");
            } else {
                root.getStyleClass().remove("dark-theme"); // Переконаємося, що класу темної теми немає, якщо тема світла
            }
            log.debug("Початкова тема '{}' встановлена. Класи кореневого елемента сцени: {}",
                    savedTheme, root.getStyleClass());

            stage.setTitle("Book Diary");
            stage.setScene(scene);
            stage.setMinWidth(900); // Мінімальна ширина вікна
            stage.setMinHeight(600); // Мінімальна висота вікна
            stage.show();
            log.info("Головне вікно застосунку успішно відображено.");

        } catch (IOException | DataAccessException e) { // Обробка IOException та нашого DataAccessException
            log.error("Критична помилка під час запуску програми: {}", e.getMessage(), e);
            showErrorDialog("Помилка Запуску", "Не вдалося запустити програму.",
                    "Виникла критична помилка під час ініціалізації. Деталі: " + e.getMessage(), e);
            Platform.exit(); // Закриваємо додаток у випадку критичної помилки на старті
        } catch (Exception e) { // Обробка інших непередбачених винятків
            log.error("Непередбачена критична помилка під час запуску програми: {}", e.getMessage(), e);
            showErrorDialog("Критична Помилка", "Не вдалося запустити програму.",
                    "Виникла непередбачена помилка. Деталі: " + e.getMessage(), e);
            Platform.exit();
        }
    }

    /**
     * Перевіряє наявність директорії для зберігання файлів обкладинок книг.
     * Якщо директорія відсутня, метод намагається її створити.
     * У разі помилки створення або якщо існує файл з такою ж назвою,
     * буде згенеровано виняток IOException та показано діалогове вікно з помилкою.
     *
     * @throws IOException Якщо не вдалося створити директорію для обкладинок.
     */
    private void ensureCoversDirectoryExists() throws IOException {
        log.debug("Перевірка наявності директорії для обкладинок: '{}'", COVERS_DIRECTORY_NAME);
        Path coversPath = Paths.get(COVERS_DIRECTORY_NAME);

        if (Files.notExists(coversPath)) {
            log.info("Директорія для обкладинок '{}' не знайдена. Спроба створити...", coversPath.toAbsolutePath());
            try {
                Files.createDirectories(coversPath);
                log.info("Директорію для обкладинок '{}' успішно створено.", coversPath.toAbsolutePath());
            } catch (IOException e) {
                log.error("Не вдалося створити директорію для обкладинок '{}'.", coversPath.toAbsolutePath(), e);
                // Кидаємо виняток далі, щоб його можна було обробити в start()
                throw new IOException("Не вдалося створити директорію для обкладинок: " + coversPath.toAbsolutePath(), e);
            }
        } else if (!Files.isDirectory(coversPath)) {
            log.error("Шлях '{}' існує, але не є директорією. Неможливо використовувати для збереження обкладинок.", coversPath.toAbsolutePath());
            throw new IOException("Неможливо створити директорію для обкладинок: шлях '" +
                    coversPath.toAbsolutePath() + "' вже зайнятий файлом.");
        } else {
            log.debug("Директорія для обкладинок '{}' вже існує.", coversPath.toAbsolutePath());
        }
    }

    /**
     * Головний метод для запуску Java-додатку.
     * Викликає метод {@link Application#launch(String...)} для старту JavaFX додатку.
     *
     * @param args Аргументи командного рядка (не використовуються в цьому додатку).
     */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Допоміжний метод для відображення діалогового вікна з повідомленням про помилку.
     * Включає можливість відображення детальної інформації про виняток (стек-трейс).
     *
     * @param title Заголовок вікна помилки.
     * @param header Текст заголовка повідомлення про помилку.
     * @param content Текст основного вмісту повідомлення про помилку.
     * @param ex Об'єкт винятку, що спричинив помилку (може бути null).
     */
    private void showErrorDialog(String title, String header, String content, Exception ex) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);

        // Додавання розширюваного контенту зі стек-трейсом, якщо виняток передано
        if (ex != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            String exceptionText = sw.toString();

            TextArea textArea = new TextArea(exceptionText);
            textArea.setEditable(false);
            textArea.setWrapText(true);
            textArea.setMaxWidth(Double.MAX_VALUE);
            textArea.setMaxHeight(Double.MAX_VALUE);
            GridPane.setVgrow(textArea, Priority.ALWAYS);
            GridPane.setHgrow(textArea, Priority.ALWAYS);

            GridPane expContent = new GridPane();
            expContent.setMaxWidth(Double.MAX_VALUE);
            expContent.add(new Label("Детальна інформація про помилку:"), 0, 0);
            expContent.add(textArea, 0, 1);

            alert.getDialogPane().setExpandableContent(expContent);
        }
        alert.showAndWait();
    }
}