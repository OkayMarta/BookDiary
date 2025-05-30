package com.student.bookdiary.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop; // Необхідний для відкриття URL-адрес у системному браузері
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Year;

/**
 * Контролер для вікна "Про програму".
 * Відображає інформацію про версію програми, автора, авторські права
 * та надає посилання на GitHub профіль автора.
 */
public class AboutController {

    private static final Logger log = LoggerFactory.getLogger(AboutController.class);

    // Константи для значень, що відображаються, та повідомлень для діалогових вікон
    private static final String GITHUB_URL = "https://github.com/OkayMarta/BookDiary";
    private static final String VERSION = "1.0-SNAPSHOT"; // Рекомендується завантажувати версію з властивостей збірки (pom.xml)
    private static final String AUTHOR = "Окілка Марта Юріївна";
    private static final String COPYRIGHT_NOTICE_PREFIX = "© ";
    private static final String COPYRIGHT_NOTICE_SUFFIX = ". Всі права захищено.";

    private static final String PLATFORM_NOT_SUPPORTED_TITLE = "Дія не підтримується";
    private static final String PLATFORM_NOT_SUPPORTED_HEADER = "Неможливо відкрити посилання";
    private static final String PLATFORM_NOT_SUPPORTED_CONTENT =
            "На жаль, ваша система не підтримує автоматичне відкриття веб-посилань з програми.";

    private static final String ERROR_OPENING_LINK_TITLE = "Помилка відкриття посилання";
    private static final String ERROR_OPENING_LINK_HEADER_PREFIX = "Не вдалося відкрити URL: ";
    private static final String ERROR_OPENING_LINK_CONTENT_PREFIX =
            "Будь ласка, спробуйте скопіювати та відкрити посилання вручну у вашому браузері.\nДеталі помилки: ";


    // --- FXML Ін'єкції ---

    /**
     * Мітка для відображення версії програми.
     * Значення встановлюється в методі {@code initialize()}.
     */
    @FXML
    private Label versionLabel;

    /**
     * Мітка для відображення імені автора програми.
     * Значення встановлюється в методі {@code initialize()}.
     */
    @FXML
    private Label authorLabel;

    /**
     * Гіперпосилання на профіль GitHub автора.
     * Обробник події натискання {@code handleGitHubLinkClick} зазвичай прив'язується в FXML через атрибут {@code onAction}.
     * Це поле дозволяє отримати доступ до гіперпосилання з коду, якщо це необхідно для інших цілей.
     */
    @FXML
    private Hyperlink githubLink; // Не використовується напряму в коді, якщо onAction в FXML

    /**
     * Мітка для відображення інформації про авторські права.
     * Значення формується та встановлюється в методі {@code initialize()}.
     */
    @FXML
    private Label copyrightLabel;

    /**
     * Метод ініціалізації контролера. Викликається JavaFX після повного завантаження FXML-файлу
     * та ін'єкції всіх полів, позначених анотацією {@code @FXML}.
     * Встановлює текстові значення для міток, використовуючи визначені константи та поточний рік.
     */
    @FXML
    private void initialize() {
        log.info("Ініціалізація контролера 'Про програму' (AboutController)...");

        versionLabel.setText(VERSION);
        authorLabel.setText(AUTHOR);

        String currentYearCopyright = COPYRIGHT_NOTICE_PREFIX +
                Year.now().getValue() + " " +
                AUTHOR +
                COPYRIGHT_NOTICE_SUFFIX;
        copyrightLabel.setText(currentYearCopyright);

        // Прив'язка обробника до гіперпосилання, якщо це не зроблено в FXML.
        // Зазвичай, onAction="#handleGitHubLinkClick" в FXML є кращим підходом.
        // if (githubLink != null) {
        //     githubLink.setOnAction(event -> handleGitHubLinkClick());
        // }
        log.info("Контролер 'Про програму' успішно ініціалізовано. Інформаційні мітки заповнені.");
    }

    /**
     * Обробляє подію натискання на гіперпосилання GitHub.
     * Намагається відкрити URL-адресу {@link #GITHUB_URL} у системному браузері за замовчуванням.
     * Якщо операція відкриття браузера не підтримується поточною системою або виникає помилка
     * (наприклад, неправильний формат URL або помилка вводу-виводу),
     * відображає відповідне діалогове вікно з повідомленням для користувача.
     *
     * Цей метод може бути прив'язаний до гіперпосилання в FXML через атрибут {@code onAction}.
     * Якщо метод не приймає {@code ActionEvent}, сигнатура в FXML має бути {@code onAction="#handleGitHubLinkClick"}.
     */
    @FXML
    private void handleGitHubLinkClick() { // Параметр ActionEvent не є обов'язковим, якщо не використовується
        log.info("Користувач натиснув на посилання GitHub. Спроба відкрити URL: {}", GITHUB_URL);

        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(new URI(GITHUB_URL));
                log.info("URL {} успішно передано системному браузеру для відкриття.", GITHUB_URL);
            } catch (IOException | URISyntaxException e) {
                log.error("Не вдалося відкрити посилання '{}' через помилку: {}", GITHUB_URL, e.getMessage(), e);
                showErrorOpeningLinkAlert(e.getMessage());
            }
        } else {
            log.warn("Клас Desktop або дія Desktop.Action.BROWSE не підтримується на поточній платформі. Неможливо відкрити URL: {}", GITHUB_URL);
            showPlatformNotSupportedAlert();
        }
    }

    // --- Допоміжні приватні методи для відображення діалогових вікон (Alerts) ---

    /**
     * Відображає діалогове вікно з попередженням (Warning) про те,
     * що автоматичне відкриття посилань не підтримується поточною системою.
     */
    private void showPlatformNotSupportedAlert() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(PLATFORM_NOT_SUPPORTED_TITLE);
        alert.setHeaderText(PLATFORM_NOT_SUPPORTED_HEADER);
        alert.setContentText(PLATFORM_NOT_SUPPORTED_CONTENT);
        alert.initOwner(getStage());
        alert.showAndWait();
    }

    /**
     * Відображає діалогове вікно з повідомленням про помилку (Error),
     * що сталася під час спроби відкрити URL-адресу в браузері.
     *
     * @param errorMessage Текст повідомлення про помилку, який буде додано до вмісту діалогового вікна.
     */
    private void showErrorOpeningLinkAlert(String errorMessage) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(ERROR_OPENING_LINK_TITLE);
        alert.setHeaderText(ERROR_OPENING_LINK_HEADER_PREFIX + GITHUB_URL);
        alert.setContentText(ERROR_OPENING_LINK_CONTENT_PREFIX + errorMessage);
        alert.initOwner(getStage());
        alert.showAndWait();
    }

    /**
     * Допоміжний метод для отримання поточного вікна (Stage), якому належить сцена цього контролера.
     * Використовується для встановлення батьківського вікна (owner) для діалогових вікон (Alerts),
     * що робить їх модальними відносно вікна "Про програму".
     *
     * @return Об'єкт {@link Stage} поточного вікна, або {@code null},
     *         якщо вікно або сцена ще не доступні (наприклад, на дуже ранніх етапах ініціалізації).
     */
    private Stage getStage() {
        // Масив усіх елементів FXML, які можуть мати сцену
        javafx.scene.Node[] nodes = {versionLabel, authorLabel, githubLink, copyrightLabel};
        
        // Перевіряємо кожен елемент на наявність сцени та вікна
        for (javafx.scene.Node node : nodes) {
            if (node != null && node.getScene() != null && node.getScene().getWindow() instanceof Stage) {
                return (Stage) node.getScene().getWindow();
            }
        }
        
        // Якщо жоден із елементів не має сцени чи вікна, логуємо попередження
        log.warn("Не вдалося визначити батьківське вікно (Stage) для Alert у AboutController. Alert буде немодальним.");
        return null;
    }
}