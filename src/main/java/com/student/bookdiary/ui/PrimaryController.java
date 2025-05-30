package com.student.bookdiary.ui;

import com.student.bookdiary.model.Book;
import com.student.bookdiary.model.Goal;
import com.student.bookdiary.persistence.BookDao;
import com.student.bookdiary.persistence.GoalDao;
import com.student.bookdiary.persistence.SqliteBookDao;
import com.student.bookdiary.persistence.SqliteGoalDao;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

public class PrimaryController {

    private static final Logger log = LoggerFactory.getLogger(PrimaryController.class);
    private Object currentViewController = null; // Поточний активний контролер подання (view)
    private BookDao bookDao; // Об'єкт доступу до даних (DAO) для роботи з книгами
    private GoalDao goalDao; // Об'єкт доступу до даних (DAO) для роботи з цілями

    // --- FXML Поля ---
    @FXML private AnchorPane contentPane; // Головна панель для відображення вмісту
    @FXML private Button readBooksButton; // Кнопка навігації "Прочитані книги"
    @FXML private Button wishlistButton;  // Кнопка навігації "Хочу прочитати"
    @FXML private Button favoritesButton; // Кнопка навігації "Улюблені"
    @FXML private Button statsButton;     // Кнопка навігації "Статистика"
    @FXML private Button goalsButton;     // Кнопка навігації "Цілі"
    @FXML private Button importExportButton; // Кнопка навігації "Імпорт/Експорт"
    @FXML private Button aboutButton;     // Кнопка навігації "Про програму"
    @FXML private Button addBookButton;   // Кнопка "Додати книгу"
    @FXML private ToggleButton themeToggleButton; // Перемикач теми (світла/темна)

    // Ключі для збереження налаштувань в Preferences API
    private static final String PREF_NODE_NAME = "com/student/bookdiary"; // Унікальний шлях вузла для налаштувань програми
    private static final String PREF_THEME_KEY = "uiTheme"; // Ключ для збереження обраної теми
    static final String THEME_DARK = "dark"; // Значення для темної теми
    private static final String THEME_LIGHT = "light"; // Значення для світлої теми

    // Шляхи до базових CSS файлів (відносно кореневої папки ресурсів, починаються з "/")
    private static final String CSS_BASE_VARIABLES = "/com/student/bookdiary/ui/css/base/core-variables.css";
    private static final String CSS_BASE_STYLES = "/com/student/bookdiary/ui/css/base/core-styles.css";

    // Шляхи до компонентних CSS файлів
    private static final String CSS_COMPONENT_NAVIGATION = "/com/student/bookdiary/ui/css/components/navigation.css";
    private static final String CSS_COMPONENT_BOOK_TILE = "/com/student/bookdiary/ui/css/components/book-tile.css";
    private static final String CSS_COMPONENT_BOOK_LIST_CONTROLS = "/com/student/bookdiary/ui/css/components/book-list-controls.css";
    private static final String CSS_COMPONENT_BOOK_DETAIL = "/com/student/bookdiary/ui/css/components/book-detail.css";
    private static final String CSS_COMPONENT_DIALOGS = "/com/student/bookdiary/ui/css/components/dialogs.css";
    private static final String CSS_COMPONENT_GOALS_VIEW = "/com/student/bookdiary/ui/css/components/goals-view.css";
    private static final String CSS_COMPONENT_STATS_VIEW = "/com/student/bookdiary/ui/css/components/stats-view.css";
    private static final String CSS_COMPONENT_IMPORT_EXPORT_VIEW = "/com/student/bookdiary/ui/css/components/import-export-view.css";

    // Шляхи до CSS файлів для тем
    private static final String CSS_THEME_DARK = "/com/student/bookdiary/ui/css/dark-theme.css";
    private static final String CSS_THEME_LIGHT = "/com/student/bookdiary/ui/css/light-theme.css";

    // Зберігає останню активну кнопку навігації (окрім кнопки "Про програму")
    private Button lastActiveMainButton;

    /**
     * Метод ініціалізації контролера. Викликається автоматично після завантаження FXML-файлу.
     * Ініціалізує DAO, налаштовує тему інтерфейсу та завантажує початкове подання.
     */
    @FXML
    private void initialize() {
        log.info("PrimaryController ініціалізація...");
        this.bookDao = new SqliteBookDao();
        this.goalDao = new SqliteGoalDao();

        // Налаштування теми перед завантаженням подання, щоб стилі застосувалися коректно
        setupTheme();
        themeToggleButton.setOnAction(event -> handleThemeToggle());

        // Завантаження початкового подання ("Прочитані книги")
        Object initialController = loadView("read_books_view.fxml");
        if (initialController instanceof ReadBooksController rbc) {
            rbc.setPrimaryController(this);
            // Примусове оновлення списку книг після повного налаштування теми та сцени
            rbc.loadBooks();
        }
        setActiveNavigationButton(readBooksButton); // Встановлення активної кнопки навігації
        lastActiveMainButton = readBooksButton;     // Збереження початкової активної кнопки
        log.info("Головний контейнер готовий. Початкове подання 'read_books_view.fxml' завантажено.");
    }

    /**
     * Налаштовує тему інтерфейсу на основі збережених користувацьких налаштувань.
     * Якщо сцена вже існує, тема застосовується до неї негайно.
     */
    private void setupTheme() {
        Preferences prefs = Preferences.userRoot().node(PREF_NODE_NAME);
        String currentTheme = prefs.get(PREF_THEME_KEY, THEME_LIGHT); // За замовчуванням - світла тема

        themeToggleButton.setSelected(THEME_DARK.equals(currentTheme));
        themeToggleButton.setText(THEME_DARK.equals(currentTheme) ? "Світла тема" : "Темна тема");

        // Якщо сцена вже існує (наприклад, при повторному відкритті вікна),
        // застосовуємо тему негайно для коректного відображення.
        if (contentPane != null && contentPane.getScene() != null) {
            Scene scene = contentPane.getScene();
            applyThemeToScene(scene, currentTheme);
            log.debug("Початкова тема '{}' застосована до існуючої сцени. Класи кореневого елемента: {}",
                    currentTheme, scene.getRoot().getStyleClass());
        }
        log.info("Початкова тема встановлена на: {}", currentTheme);
    }

    /**
     * Обробляє подію натискання кнопки перемикання теми інтерфейсу.
     * Змінює поточну тему, зберігає вибір у Preferences API та оновлює поточне подання.
     */
    @FXML
    private void handleThemeToggle() {
        Scene scene = contentPane.getScene();
        if (scene == null) {
            log.error("Неможливо змінити тему: сцена не знайдена (contentPane.getScene() is null).");
            return;
        }

        Preferences prefs = Preferences.userRoot().node(PREF_NODE_NAME);
        String newTheme = themeToggleButton.isSelected() ? THEME_DARK : THEME_LIGHT;

        themeToggleButton.setText(THEME_DARK.equals(newTheme) ? "Світла тема" : "Темна тема");
        log.info("Перемикання теми на: {}", newTheme);

        applyThemeToScene(scene, newTheme); // Застосування нової теми до сцени
        prefs.put(PREF_THEME_KEY, newTheme); // Збереження вибору теми

        log.debug("Класи кореневого елемента після зміни теми: {}", scene.getRoot().getStyleClass());

        // Оновлення поточного подання для коректного застосування нових стилів
        refreshCurrentView();
    }

    /**
     * Застосовує вказану тему та всі необхідні CSS файли до переданої сцени.
     * Цей метод є централізованим місцем для управління стилями програми,
     * забезпечуючи правильний порядок завантаження CSS.
     * @param scene Сцена, до якої застосовуються стилі.
     * @param themeName Назва теми (використовуйте константи THEME_DARK або THEME_LIGHT).
     */
    public static void applyThemeToScene(Scene scene, String themeName) {
        if (scene == null) {
            log.warn("Спроба застосувати тему до неіснуючої сцени (параметр scene є null).");
            return;
        }

        // Очищення попередніх класів теми та всіх таблиць стилів зі сцени
        scene.getRoot().getStyleClass().removeAll(THEME_DARK, THEME_LIGHT);
        scene.getStylesheets().clear();
        log.debug("Попередні стилі та класи теми очищено зі сцени.");

        // Формування списку CSS файлів для завантаження у визначеному порядку
        List<String> cssFilesToLoad = new ArrayList<>();
        cssFilesToLoad.add(CSS_BASE_VARIABLES);           // Базові змінні CSS
        cssFilesToLoad.add(CSS_BASE_STYLES);              // Базові стилі ядра
        cssFilesToLoad.add(CSS_COMPONENT_NAVIGATION);     // Стилі для навігації
        cssFilesToLoad.add(CSS_COMPONENT_BOOK_TILE);      // Стилі для плиток книг
        cssFilesToLoad.add(CSS_COMPONENT_BOOK_LIST_CONTROLS); // Стилі для контролів списку книг
        cssFilesToLoad.add(CSS_COMPONENT_BOOK_DETAIL);    // Стилі для детального вигляду книги
        cssFilesToLoad.add(CSS_COMPONENT_DIALOGS);        // Стилі для діалогових вікон
        cssFilesToLoad.add(CSS_COMPONENT_GOALS_VIEW);     // Стилі для подання цілей
        cssFilesToLoad.add(CSS_COMPONENT_STATS_VIEW);     // Стилі для подання статистики
        cssFilesToLoad.add(CSS_COMPONENT_IMPORT_EXPORT_VIEW); // Стилі для подання імпорту/експорту

        // Додавання файлу конкретної теми (завантажується останнім, щоб перевизначити базові стилі)
        if (THEME_DARK.equals(themeName)) {
            cssFilesToLoad.add(CSS_THEME_DARK);
            scene.getRoot().getStyleClass().add(THEME_DARK); // Додавання CSS класу теми до кореневого елемента
        } else {
            cssFilesToLoad.add(CSS_THEME_LIGHT);
            scene.getRoot().getStyleClass().add(THEME_LIGHT); // Додавання CSS класу теми до кореневого елемента
        }

        // Завантаження всіх CSS файлів зі списку
        for (String cssPath : cssFilesToLoad) {
            safeAddStylesheet(scene, cssPath);
        }

        log.debug("Застосовано тему: {}. Класи кореневого елемента: {}",
                themeName, scene.getRoot().getStyleClass());
        log.info("Усі таблиці стилів для теми '{}' успішно застосовано до сцени.", themeName);
    }

    /**
     * Безпечно додає CSS файл до сцени.
     * Перевіряє наявність ресурсу перед додаванням та логує помилки.
     * @param scene Сцена, до якої додається CSS файл.
     * @param cssPath Шлях до CSS файлу (відносно папки ресурсів).
     */
    private static void safeAddStylesheet(Scene scene, String cssPath) {
        if (cssPath == null || cssPath.trim().isEmpty()) {
            log.warn("Спроба додати порожній або null шлях до CSS файлу.");
            return;
        }
        try {
            // Використання PrimaryController.class.getResource() для надійного пошуку ресурсів
            URL cssUrl = PrimaryController.class.getResource(cssPath);
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
                log.debug("Таблицю стилів '{}' успішно додано до сцени.", cssPath);
            } else {
                log.error("Не вдалося знайти ресурс таблиці стилів за шляхом: {}", cssPath);
            }
        } catch (Exception e) {
            log.error("Помилка під час застосування таблиці стилів '{}': {}", cssPath, e.getMessage(), e);
        }
    }

    // Стиль для активної кнопки навігації (можна винести в CSS файл для кращої організації)
    private final String activeButtonStyle = "-fx-background-color: #AA8F76; -fx-text-fill: white;";

    /**
     * Встановлює стиль активної кнопки навігації та скидає стиль для інших кнопок.
     * Також оновлює `lastActiveMainButton`, якщо активується основна кнопка навігації.
     * @param activeButton Кнопка, яку потрібно позначити як активну.
     */
    private void setActiveNavigationButton(Button activeButton) {
        // Список усіх кнопок навігації для скидання стилю
        Button[] navButtons = {
                readBooksButton, wishlistButton, favoritesButton, statsButton,
                goalsButton, importExportButton, aboutButton
        };
        for (Button btn : navButtons) {
            if (btn != null) {
                btn.setStyle(null); // Скидаємо стиль до стандартного
            }
        }

        // Встановлення активного стилю для обраної кнопки
        if (activeButton != null) {
            activeButton.setStyle(activeButtonStyle);
            // Збереження останньої активної "основної" кнопки (тобто, не "Про програму")
            if (activeButton != aboutButton) {
                this.lastActiveMainButton = activeButton;
            }
        }
        log.debug("Активна кнопка навігації: {}", activeButton != null ? activeButton.getText() : "немає");
    }

    // --- Обробники Натискання Кнопок Навігації ---
    @FXML private void handleReadBooksClick() {
        log.info("Навігація: Прочитані книги");
        setActiveNavigationButton(readBooksButton);
        loadViewAndSetController("read_books_view.fxml", ReadBooksController.class);
    }

    @FXML private void handleWishlistClick() {
        log.info("Навігація: Хочу прочитати");
        setActiveNavigationButton(wishlistButton);
        loadViewAndSetController("wishlist_view.fxml", WishlistController.class);
    }

    @FXML private void handleFavoritesClick() {
        log.info("Навігація: Улюблені книги");
        setActiveNavigationButton(favoritesButton);
        loadViewAndSetController("favorites_view.fxml", FavoritesController.class);
    }

    @FXML private void handleStatsClick() {
        log.info("Навігація: Статистика");
        setActiveNavigationButton(statsButton);
        loadView("stats_view.fxml"); // Для StatsController може не бути потреби встановлювати PrimaryController напряму тут
    }

    @FXML private void handleGoalsClick() {
        log.info("Навігація: Цілі");
        setActiveNavigationButton(goalsButton);
        loadViewAndSetController("goals_view.fxml", GoalsController.class);
    }

    @FXML private void handleImportExportClick() {
        log.info("Навігація: Імпорт / Експорт");
        setActiveNavigationButton(importExportButton);
        loadView("import_export_view.fxml"); // Аналогічно StatsController
    }

    @FXML private void handleAboutClick() {
        log.info("Навігація: Про програму");
        setActiveNavigationButton(aboutButton); // Тимчасово робимо кнопку "Про програму" активною
        showAboutDialog();
        // Відновлення попередньої активної основної кнопки після закриття діалогу "Про програму"
        if (lastActiveMainButton != null) {
            setActiveNavigationButton(lastActiveMainButton);
        } else {
            // Якщо з якоїсь причини lastActiveMainButton не встановлена, повертаємось до "Прочитані книги" як до стандартного
            log.warn("lastActiveMainButton is null after About dialog, defaulting to Read Books.");
            setActiveNavigationButton(readBooksButton);
        }
    }

    @FXML private void handleAddBookClick() {
        log.info("Натиснуто кнопку 'Додати нову книгу'");
        showAddEditBookDialog(null); // Передача null означає створення нової книги
    }

    /**
     * Завантажує FXML файл у головну панель вмісту (`contentPane`).
     * Оновлює `currentViewController` та відповідну кнопку навігації.
     * Якщо контролер завантаженого подання є екземпляром `BaseController`,
     * встановлює для нього `PrimaryController` та викликає `refreshData()`.
     * @param fxmlPath Відносний шлях до FXML файлу (наприклад, "read_books_view.fxml").
     * @return Об'єкт завантаженого контролера або `null` у разі помилки.
     */
    public Object loadView(String fxmlPath) {
        log.info("Завантаження подання: {}", fxmlPath);
        try {
            // Формування повного шляху до ресурсу
            URL fxmlUrl = getClass().getResource("/com/student/bookdiary/ui/" + fxmlPath);
            if (fxmlUrl == null) {
                log.error("Не знайдено FXML файл за шляхом: /com/student/bookdiary/ui/{}", fxmlPath);
                throw new IOException("Ресурс FXML не знайдено: " + fxmlPath);
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent viewRoot = loader.load();

            // Очищення попереднього вмісту та встановлення нового
            contentPane.getChildren().setAll(viewRoot);
            AnchorPane.setTopAnchor(viewRoot, 0.0);
            AnchorPane.setBottomAnchor(viewRoot, 0.0);
            AnchorPane.setLeftAnchor(viewRoot, 0.0);
            AnchorPane.setRightAnchor(viewRoot, 0.0);

            // Оновлення посилання на поточний активний контролер подання
            Object controller = loader.getController();
            currentViewController = controller;

            // Оновлення активної кнопки навігації відповідно до завантаженого подання
            updateNavigationButtonForView(fxmlPath);

            // Ініціалізація контролера (якщо він є BaseController) та оновлення даних
            if (controller instanceof BaseController baseCtrl) {
                baseCtrl.setPrimaryController(this);
                baseCtrl.refreshData(); // Виклик методу для оновлення даних у новому поданні
                log.debug("Для контролера {} встановлено PrimaryController та викликано refreshData().", controller.getClass().getSimpleName());
            }

            log.info("Подання '{}' успішно завантажено та відображено.", fxmlPath);
            return controller;
        } catch (IOException e) {
            log.error("Помилка завантаження FXML файлу '{}': {}", fxmlPath, e.getMessage(), e);
            showErrorAlert("Помилка завантаження", "Не вдалося завантажити подання: " + fxmlPath, e.getMessage());
            return null;
        }
    }

    /**
     * Оновлює виділення активної кнопки навігації на основі завантаженого FXML файлу.
     * @param fxmlPath Шлях до FXML файлу подання, яке було щойно завантажено.
     */
    private void updateNavigationButtonForView(String fxmlPath) {
        switch (fxmlPath) {
            case "read_books_view.fxml" -> setActiveNavigationButton(readBooksButton);
            case "wishlist_view.fxml" -> setActiveNavigationButton(wishlistButton);
            case "favorites_view.fxml" -> setActiveNavigationButton(favoritesButton);
            case "stats_view.fxml" -> setActiveNavigationButton(statsButton);
            case "goals_view.fxml" -> setActiveNavigationButton(goalsButton);
            case "import_export_view.fxml" -> setActiveNavigationButton(importExportButton);
            case "about_view.fxml" -> setActiveNavigationButton(aboutButton); // Для діалогу "Про програму"
            default -> log.warn("Невідомий FXML шлях '{}' для оновлення кнопки навігації.", fxmlPath);
        }
    }

    /**
     * Узагальнений метод для завантаження подання та встановлення `PrimaryController` для його контролера,
     * якщо контролер підтримує цей механізм (має метод `setPrimaryController`).
     * Також викликає `loadBooks()` для `ReadBooksController` після налаштування.
     * @param fxmlPath Шлях до FXML файлу подання.
     * @param controllerClass Клас очікуваного контролера.
     * @param <T> Тип контролера.
     */
    private <T> void loadViewAndSetController(String fxmlPath, Class<T> controllerClass) {
        Object controller = loadView(fxmlPath); // loadView вже обробляє BaseController
        if (controller == null) {
            log.error("Не вдалося завантажити контролер для подання '{}'. Подання не буде налаштовано.", fxmlPath);
            return;
        }

        if (!controllerClass.isInstance(controller)) {
            log.error("Завантажений контролер ({}) не є екземпляром очікуваного класу {}.",
                    controller.getClass().getName(), controllerClass.getName());
            return;
        }

        // Якщо контролер не є BaseController, але потребує PrimaryController,
        // можна додати специфічну логіку тут або переконатися, що loadView обробляє це.
        // Поточна реалізація loadView вже викликає setPrimaryController для BaseController.
        // Цей блок може бути для контролерів, що не є BaseController, але мають setPrimaryController.
        // Однак, якщо всі контролери, яким потрібен PrimaryController, успадковують BaseController,
        // то ця частина може бути спрощена або видалена.

        // Додаткова перевірка, чи потрібно викликати setPrimaryController, якщо controller не є BaseController
        // (хоча зазвичай це робиться через спільний інтерфейс або базовий клас).
        if (!(controller instanceof BaseController)) {
            try {
                // Спроба викликати setPrimaryController за допомогою рефлексії, якщо контролер не BaseController
                controller.getClass().getMethod("setPrimaryController", PrimaryController.class).invoke(controller, this);
                log.debug("PrimaryController успішно встановлено для {} (не BaseController) за допомогою рефлексії.", controllerClass.getSimpleName());
            } catch (NoSuchMethodException nsme) {
                // Це не помилка, якщо контролер не BaseController і не має цього методу.
                log.debug("Контролер {} (не BaseController) не має методу setPrimaryController.", controllerClass.getSimpleName());
            } catch (Exception e) {
                log.error("Помилка при спробі встановити PrimaryController для {} (не BaseController) через рефлексію: {}",
                        controllerClass.getSimpleName(), e.getMessage(), e);
                showErrorAlert("Помилка ініціалізації контролера",
                        "Не вдалося повністю налаштувати контролер " + controllerClass.getSimpleName(),
                        e.getMessage());
            }
        }


        // Специфічне оновлення для ReadBooksController (якщо він не був оброблений як BaseController з refreshData)
        // або якщо loadBooks має бути викликаний окремо.
        if (controller instanceof ReadBooksController rbc && !(controller instanceof BaseController)) {
            // Якщо ReadBooksController є BaseController, loadBooks може бути частиною refreshData.
            // Цей блок потрібен, якщо loadBooks - це окрема дія.
            rbc.loadBooks();
            log.debug("Викликано loadBooks() для ReadBooksController після завантаження подання '{}'.", fxmlPath);
        }
    }


    /**
     * Створює та налаштовує загальне діалогове вікно (Stage).
     * Встановлює заголовок, модальність, батьківське вікно та застосовує поточну тему.
     * @param title Заголовок діалогового вікна.
     * @param dialogRoot Кореневий вузол (Parent) вмісту діалогового вікна.
     * @return Налаштований об'єкт Stage для діалогу.
     */
    private Stage createAndConfigureDialog(String title, Parent dialogRoot) {
        Stage dialogStage = new Stage();
        dialogStage.setTitle(title);
        dialogStage.initModality(Modality.WINDOW_MODAL); // Блокує взаємодію з батьківським вікном до закриття діалогу

        // Встановлення батьківського вікна для діалогу
        Window owner = (contentPane != null && contentPane.getScene() != null) ? contentPane.getScene().getWindow() : null;
        if (owner != null) {
            dialogStage.initOwner(owner);
        } else {
            log.warn("Не вдалося визначити батьківське вікно для діалогу '{}'. Діалог може з'явитися не поверх головного вікна.", title);
        }

        Scene scene = new Scene(dialogRoot);

        // Застосування поточної теми програми до сцени діалогового вікна
        Preferences prefs = Preferences.userRoot().node(PREF_NODE_NAME);
        String currentTheme = prefs.get(PREF_THEME_KEY, THEME_LIGHT);
        applyThemeToScene(scene, currentTheme); // Використовуємо загальний метод застосування теми

        dialogStage.setScene(scene);
        dialogStage.setResizable(false); // Заборона зміни розміру діалогового вікна
        log.debug("Діалогове вікно '{}' створено та налаштовано.", title);
        return dialogStage;
    }

    /**
     * Показує діалогове вікно для додавання нової або редагування існуючої книги.
     * @param bookToEdit Книга для редагування. Якщо `null`, відкривається діалог для додавання нової книги.
     */
    public void showAddEditBookDialog(Book bookToEdit) {
        final String fxmlPath = "/com/student/bookdiary/ui/add_edit_book_dialog.fxml";
        log.info("Спроба відкрити діалог {} книги.", (bookToEdit == null ? "додавання нової" : "редагування існуючої"));
        try {
            URL fxmlUrl = getClass().getResource(fxmlPath);
            if (fxmlUrl == null) {
                log.error("Не знайдено FXML файл для діалогу книги: {}", fxmlPath);
                throw new IOException("FXML файл не знайдено: " + fxmlPath);
            }
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent dialogRoot = loader.load();

            Stage dialogStage = createAndConfigureDialog(
                    bookToEdit == null ? "Додати нову книгу" : "Редагувати книгу",
                    dialogRoot
            );

            AddEditBookController controller = loader.getController();
            controller.setDialogStage(dialogStage);
            controller.setBookDao(this.bookDao);
            controller.setBookToEdit(bookToEdit);
            // Встановлення callback-функції для оновлення списку книг після успішного збереження/редагування
            controller.setRefreshCallback(this::refreshCurrentView);

            dialogStage.showAndWait(); // Показуємо діалог і чекаємо на його закриття
            log.debug("Діалог роботи з книгою закрито.");
        } catch (Exception e) {
            log.error("Помилка під час завантаження або відкриття діалогового вікна книги: {}", e.getMessage(), e);
            showErrorAlert("Помилка діалогу", "Не вдалося відкрити вікно для роботи з книгою.", e.getMessage());
        }
    }

    /**
     * Показує діалогове вікно для додавання нової або редагування існуючої цілі.
     * @param goalToEdit Ціль для редагування. Якщо `null`, відкривається діалог для додавання нової цілі.
     */
    public void showAddEditGoalDialog(Goal goalToEdit) {
        final String fxmlPath = "/com/student/bookdiary/ui/add_edit_goal_dialog.fxml";
        log.info("Спроба відкрити діалог {} цілі.", (goalToEdit == null ? "додавання нової" : "редагування існуючої"));
        try {
            URL fxmlUrl = getClass().getResource(fxmlPath);
            if (fxmlUrl == null) {
                log.error("Не знайдено FXML файл для діалогу цілі: {}", fxmlPath);
                throw new IOException("FXML файл не знайдено: " + fxmlPath);
            }
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent dialogRoot = loader.load();

            Stage dialogStage = createAndConfigureDialog(
                    goalToEdit == null ? "Додати нову ціль" : "Редагувати ціль",
                    dialogRoot
            );

            AddEditGoalController controller = loader.getController();
            controller.setDialogStage(dialogStage);
            controller.setGoalDao(this.goalDao);
            controller.setGoalToEdit(goalToEdit);
            // Встановлення callback-функції для оновлення подання цілей
            controller.setRefreshCallback(this::refreshCurrentView);

            dialogStage.showAndWait();
            log.debug("Діалог роботи з ціллю закрито.");
        } catch (Exception e) {
            log.error("Помилка під час завантаження або відкриття діалогового вікна цілі: {}", e.getMessage(), e);
            showErrorAlert("Помилка діалогу", "Не вдалося відкрити вікно для роботи з ціллю.", e.getMessage());
        }
    }

    /**
     * Показує діалогове вікно "Про програму".
     */
    private void showAboutDialog() {
        final String fxmlPath = "/com/student/bookdiary/ui/about_view.fxml";
        log.info("Відкриття діалогового вікна 'Про програму'.");
        try {
            URL fxmlUrl = getClass().getResource(fxmlPath);
            if (fxmlUrl == null) {
                log.error("Не знайдено FXML файл для вікна 'Про програму': {}", fxmlPath);
                throw new IOException("FXML файл не знайдено: " + fxmlPath);
            }
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent dialogRoot = loader.load();

            Stage dialogStage = createAndConfigureDialog(
                    "Про програму Book Diary",
                    dialogRoot
            );

            // Додавання специфічного CSS класу до кореневого елемента діалогу "Про програму" для кастомізації стилю
            if (dialogRoot != null) {
                dialogRoot.getStyleClass().add("about-dialog-pane");
            }

            dialogStage.showAndWait();
            log.debug("Діалог 'Про програму' закрито.");
        } catch (Exception e) {
            log.error("Помилка під час відкриття вікна 'Про програму': {}", e.getMessage(), e);
            showErrorAlert("Помилка", "Не вдалося відкрити вікно 'Про програму'.", e.getMessage());
        }
    }

    /**
     * Оновлює дані в поточному активному поданні (наприклад, список книг, цілей).
     * Викликається після операцій додавання, редагування або видалення даних,
     * щоб відобразити зміни.
     */
    private void refreshCurrentView() {
        log.debug("Спроба оновити поточне подання. Активний контролер: {}",
                currentViewController != null ? currentViewController.getClass().getSimpleName() : "відсутній");

        if (currentViewController == null) {
            log.warn("Не вдалося оновити подання: поточний контролер не встановлено (currentViewController is null).");
            return;
        }

        // Використання instanceof для безпечного виклику методів оновлення конкретних контролерів
        if (currentViewController instanceof ReadBooksController rbc) {
            log.info("Оновлення подання 'Прочитані книги'...");
            rbc.loadBooks();
        } else if (currentViewController instanceof WishlistController wlc) {
            log.info("Оновлення подання 'Хочу прочитати'...");
            wlc.loadBooks();
        } else if (currentViewController instanceof FavoritesController fc) {
            log.info("Оновлення подання 'Улюблені'...");
            fc.loadBooks();
        } else if (currentViewController instanceof GoalsController gc) {
            log.info("Оновлення подання 'Цілі'...");
            gc.refreshGoalsDisplay();
        } else if (currentViewController instanceof StatsController sc) {
            log.info("Оновлення подання 'Статистика'...");
            // Якщо StatsController має метод для примусового оновлення, викликати його тут.
            // Наприклад: sc.refreshStats();
        } else if (currentViewController instanceof BookDetailController) {
            // BookDetailController зазвичай не потребує "оновлення" в цьому контексті,
            // оскільки він відображає одну книгу. Оновлення даних відбувається через
            // повернення до списку та його оновлення.
            log.info("Поточне подання - BookDetailController. Специфічне оновлення не потрібне; дані оновлюються при поверненні до списку.");
        } else if (currentViewController instanceof BaseController baseCtrl) {
            // Загальний випадок для всіх BaseController, якщо вони мають стандартизований метод оновлення.
            log.info("Оновлення подання для BaseController: {}", baseCtrl.getClass().getSimpleName());
            baseCtrl.refreshData();
        }
        else {
            log.warn("Не вдалося оновити подання: невідомий або непідтримуваний тип контролера ({}).",
                    currentViewController.getClass().getName());
        }
    }

    /**
     * Показує стандартне діалогове вікно з повідомленням про помилку.
     * @param title Заголовок вікна помилки.
     * @param header Заголовок повідомлення всередині вікна.
     * @param content Детальний текст повідомлення про помилку.
     */
    void showErrorAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        // Встановлення батьківського вікна для Alert, щоб він був прив'язаний до головного вікна програми
        Window owner = (contentPane != null && contentPane.getScene() != null) ? contentPane.getScene().getWindow() : null;
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content != null ? content : "Деталі помилки не вказані.");
        log.warn("Показано повідомлення про помилку: Title='{}', Header='{}'", title, header);
        alert.showAndWait();
    }

    /**
     * Показує детальне подання обраної книги.
     * @param book Книга, деталі якої потрібно відобразити.
     * @param returnViewFxml FXML файл подання, на яке потрібно повернутися після закриття деталей книги (наприклад, "read_books_view.fxml").
     */
    public void showBookDetailView(Book book, String returnViewFxml) {
        log.info("Перехід до детального перегляду книги ID={}, назва: '{}'. Шлях повернення: {}",
                book.getId(), book.getTitle(), returnViewFxml);
        currentViewController = null; // Скидаємо поточний контролер перед завантаженням нового
        final String fxmlPath = "/com/student/bookdiary/ui/book_detail_view.fxml";
        try {
            URL fxmlUrl = getClass().getResource(fxmlPath);
            if (fxmlUrl == null) {
                log.error("Не знайдено FXML файл для детального вигляду книги: {}", fxmlPath);
                throw new IOException("Не знайдено FXML файл: " + fxmlPath);
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent viewRoot = loader.load();

            // Для коректного застосування стилів, новий viewRoot потрібно додати до сцени.
            // Очищаємо contentPane і додаємо новий viewRoot.
            contentPane.getChildren().setAll(viewRoot);
            AnchorPane.setTopAnchor(viewRoot, 0.0);
            AnchorPane.setBottomAnchor(viewRoot, 0.0);
            AnchorPane.setLeftAnchor(viewRoot, 0.0);
            AnchorPane.setRightAnchor(viewRoot, 0.0);

            // Застосування поточної теми до сцени, що містить contentPane (і, відповідно, новий viewRoot)
            if (contentPane.getScene() != null) {
                Preferences prefs = Preferences.userRoot().node(PREF_NODE_NAME);
                String currentTheme = prefs.get(PREF_THEME_KEY, THEME_LIGHT);
                applyThemeToScene(contentPane.getScene(), currentTheme);
                log.debug("Тема '{}' застосована до сцени для BookDetailView.", currentTheme);
            } else {
                log.warn("Сцена для contentPane не знайдена під час показу BookDetailView, тема може бути не застосована коректно.");
            }

            BookDetailController detailController = loader.getController();
            detailController.setData(book, this, returnViewFxml); // Передача даних та посилань контролеру деталей

            currentViewController = detailController; // Оновлення посилання на поточний активний контролер
            log.info("Подання 'book_detail_view.fxml' успішно завантажено для книги '{}'.", book.getTitle());

        } catch (IOException e) {
            log.error("Помилка завантаження FXML файлу 'book_detail_view.fxml': {}", e.getMessage(), e);
            // Відображення повідомлення про помилку безпосередньо в contentPane
            contentPane.getChildren().setAll(new javafx.scene.control.Label(
                    "Помилка завантаження деталей книги. Будь ласка, спробуйте ще раз або зверніться до підтримки."
            ));
            showErrorAlert("Помилка завантаження", "Не вдалося завантажити детальний вигляд книги.", e.getMessage());
        }
    }

    /**
     * Перевіряє, чи встановлена наразі темна тема інтерфейсу.
     * @return `true`, якщо активна темна тема, інакше `false`.
     */
    public boolean isDarkTheme() {
        Preferences prefs = Preferences.userRoot().node(PREF_NODE_NAME);
        // Використовуємо THEME_LIGHT як значення за замовчуванням, якщо налаштування теми відсутнє
        String currentTheme = prefs.get(PREF_THEME_KEY, THEME_LIGHT);
        boolean isDark = THEME_DARK.equals(currentTheme);
        log.debug("Перевірка поточної теми (з Preferences): {}", isDark ? "темна" : "світла");
        return isDark;
    }
}