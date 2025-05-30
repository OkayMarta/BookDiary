package com.student.bookdiary.ui;

import com.student.bookdiary.model.Goal;
import com.student.bookdiary.model.GoalType;
import com.student.bookdiary.persistence.BookDao;
import com.student.bookdiary.persistence.GoalDao;
import com.student.bookdiary.persistence.SqliteBookDao;
import com.student.bookdiary.persistence.SqliteGoalDao;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
// Імпорти Font, FontPosture, FontWeight залишені, оскільки вони можуть використовуватися в FXML файлі,
// пов'язаному з цим контролером, або були заплановані для використання.
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Month;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class GoalsController {

    private static final Logger log = LoggerFactory.getLogger(GoalsController.class);

    @FXML
    private Button addGoalButton; // Кнопка для додавання нової цілі
    @FXML
    private FlowPane goalsFlowPane; // Панель для відображення карток цілей
    @FXML
    private ComboBox<String> filterComboBox; // Випадаючий список для фільтрації цілей

    private GoalDao goalDao; // Об'єкт для доступу до даних цілей
    private BookDao bookDao; // Об'єкт для доступу до даних книг (для розрахунку прогресу)
    private PrimaryController primaryController; // Контролер головного вікна

    // Константи для фільтрації
    private static final String FILTER_ALL = "Всі цілі";
    private static final String FILTER_ACTIVE = "Активні цілі";
    private static final String FILTER_COMPLETED = "Виконані цілі";


    /**
     * Метод ініціалізації, викликається після завантаження FXML.
     * Налаштовує DAO, фільтр та завантажує цілі.
     */
    @FXML
    private void initialize() {
        log.info("Ініціалізація GoalsController...");
        this.goalDao = new SqliteGoalDao();
        this.bookDao = new SqliteBookDao();

        setupFilterComboBox(); // Налаштування випадаючого списку фільтрації
        loadAndDisplayGoals(); // Завантаження та відображення цілей

        log.info("GoalsController успішно ініціалізовано.");
    }

    /**
     * Налаштовує випадаючий список для фільтрації цілей.
     */
    private void setupFilterComboBox() {
        filterComboBox.setItems(FXCollections.observableArrayList(
                FILTER_ALL,
                FILTER_ACTIVE,
                FILTER_COMPLETED
        ));
        filterComboBox.setValue(FILTER_ALL); // Значення за замовчуванням
        // Слухач для оновлення списку при зміні фільтра
        filterComboBox.valueProperty().addListener((_, _, newValue) -> {
            if (newValue != null) {
                log.debug("Змінено фільтр цілей на: {}", newValue);
                loadAndDisplayGoals();
            }
        });
        log.debug("Фільтр цілей налаштовано.");
    }

    /**
     * Встановлює посилання на головний контролер.
     * @param primaryController головний контролер програми.
     */
    public void setPrimaryController(PrimaryController primaryController) {
        this.primaryController = primaryController;
    }

    /**
     * Обробник натискання кнопки "Додати нову ціль".
     * Відкриває діалогове вікно для створення нової цілі.
     */
    @FXML
    private void handleAddGoalButton() {
        log.debug("Натиснуто кнопку 'Додати нову ціль'");
        if (primaryController != null) {
            primaryController.showAddEditGoalDialog(null); // null означає створення нової цілі
        } else {
            log.error("PrimaryController не встановлено. Неможливо відкрити діалог додавання цілі.");
            showErrorAlertHelper("Помилка", "Неможливо виконати дію.", "Зв'язок з головним вікном втрачено.");
        }
    }

    /**
     * Завантажує цілі з бази даних, фільтрує та сортує їх,
     * а потім відображає у вигляді карток на FlowPane.
     */
    public void loadAndDisplayGoals() {
        log.debug("Завантаження та відображення цілей...");
        goalsFlowPane.getChildren().clear(); // Очищення попередніх карток

        try {
            List<Goal> goals = goalDao.getAllGoals();
            // Сортування: спочатку активні, потім виконані.
            // В межах кожної групи сортування за датою додавання (новіші спочатку).
            goals.sort(Comparator
                    .comparing((Goal g) -> calculateProgress(g) >= g.getTargetValue()) // true (виконані) йдуть пізніше
                    .thenComparing(Goal::getDateAdded, Comparator.nullsLast(Comparator.reverseOrder()))); // Обробка можливого null для dateAdded

            String selectedFilter = filterComboBox.getValue();
            log.debug("Застосовується фільтр: {}", selectedFilter);

            int displayedGoalsCount = 0;
            for (Goal goal : goals) {
                int currentProgress = calculateProgress(goal);
                boolean isAchieved = goal.getTargetValue() > 0 && currentProgress >= goal.getTargetValue();

                boolean shouldDisplay = false;
                if (FILTER_ALL.equals(selectedFilter)) {
                    shouldDisplay = true;
                } else if (FILTER_ACTIVE.equals(selectedFilter) && !isAchieved) {
                    shouldDisplay = true;
                } else if (FILTER_COMPLETED.equals(selectedFilter) && isAchieved) {
                    shouldDisplay = true;
                }

                if (shouldDisplay) {
                    Node goalCardNode = createGoalCardNode(goal, currentProgress, isAchieved);
                    goalsFlowPane.getChildren().add(goalCardNode);
                    displayedGoalsCount++;
                }
            }
            log.info("Цілі завантажено та відображено. Кількість показаних карток: {}", displayedGoalsCount);
        } catch (Exception e) {
            log.error("Помилка при завантаженні або відображенні цілей", e);
            Label errorLabel = new Label("Не вдалося завантажити цілі: " + e.getMessage());
            errorLabel.setStyle("-fx-text-fill: red; -fx-padding: 10px;"); // Додано стиль для кращої видимості
            goalsFlowPane.getChildren().add(errorLabel);
            showErrorAlertHelper("Помилка завантаження", "Не вдалося завантажити список цілей.", e.getMessage());
        }
    }

    /**
     * Розраховує поточний прогрес для вказаної цілі.
     * @param goal Ціль, для якої розраховується прогрес.
     * @return Кількість прочитаних книг, що відповідають критеріям цілі.
     */
    private int calculateProgress(Goal goal) {
        if (goal == null || goal.getType() == null) {
            log.warn("Спроба розрахувати прогрес для некоректної цілі (null або тип null): {}", goal != null ? goal.getId() : "ID невідомий");
            return 0;
        }

        int currentProgress = 0;
        switch (goal.getType()) {
            case MONTHLY:
                if (goal.getYear() != null && goal.getMonth() != null) {
                    currentProgress = bookDao.getBooksReadCountByMonthAndYear(goal.getYear(), goal.getMonth());
                } else {
                    log.warn("Для місячної цілі ID {} не вказано рік або місяць.", goal.getId());
                }
                break;
            case YEARLY:
                if (goal.getYear() != null) {
                    currentProgress = bookDao.getBooksReadCountByYear(goal.getYear());
                } else {
                    log.warn("Для річної цілі ID {} не вказано рік.", goal.getId());
                }
                break;
            case TOTAL:
                currentProgress = bookDao.getTotalBooksReadCount();
                break;
            default:
                // Цей випадок не повинен виникати, якщо GoalType є enum
                log.warn("Невідомий тип цілі: {} для цілі ID {}", goal.getType(), goal.getId());
        }
        log.trace("Розрахований прогрес для цілі ID {}: {}", goal.getId(), currentProgress);
        return currentProgress;
    }

    /**
     * Створює вузол (Node) для відображення картки цілі.
     * @param goal Об'єкт цілі.
     * @param currentProgress Поточний прогрес цілі.
     * @param isAchieved Чи досягнута ціль.
     * @return Вузол картки цілі.
     */
    private Node createGoalCardNode(Goal goal, int currentProgress, boolean isAchieved) {
        VBox card = new VBox(10); // Вертикальний контейнер з відступом 10px
        card.getStyleClass().add("goal-card");
        if (isAchieved) {
            card.getStyleClass().add("goal-card-achieved"); // Стиль для досягнутої цілі
        }
        card.setPadding(new Insets(15)); // Внутрішні відступи картки
        card.setPrefWidth(280); // Бажана ширина картки
        card.setMinWidth(250);  // Мінімальна ширина картки

        // 1. Опис цілі
        String descriptionText = goal.getDescription();
        if (descriptionText == null || descriptionText.trim().isEmpty()) {
            descriptionText = generateDefaultDescription(goal); // Генерація опису, якщо він відсутній
        }
        Label descriptionLabel = new Label(descriptionText);
        descriptionLabel.setWrapText(true); // Дозволити перенос тексту
        descriptionLabel.setMaxWidth(Double.MAX_VALUE); // Максимальна ширина для переносу
        descriptionLabel.getStyleClass().add("goal-card-description");

        // 2. Тип цілі
        String typeName = (goal.getType() != null) ? goal.getType().getDisplayName() : "Невідомий тип";
        Label typeLabel = new Label("Тип: " + typeName);
        typeLabel.getStyleClass().add("goal-card-details-label");

        // 3. Цільове значення
        Label targetLabel = new Label("Ціль: " + goal.getTargetValue() + " книг(и)");
        targetLabel.getStyleClass().add("goal-card-details-label");

        // 4. Текст прогресу
        Label progressTextLabel = new Label("Прочитано: " + currentProgress + " / " + goal.getTargetValue());
        progressTextLabel.getStyleClass().add("goal-card-progress-text");

        // 5. Індикатор прогресу (ProgressBar)
        ProgressBar progressBar = new ProgressBar();
        double progressValue = (goal.getTargetValue() > 0) ? (double) currentProgress / goal.getTargetValue() : 0;
        progressBar.setProgress(Math.min(progressValue, 1.0)); // Значення прогресу від 0.0 до 1.0
        progressBar.setMaxWidth(Double.MAX_VALUE); // Розтягнути на всю ширину
        // Стиль для ProgressBar досягнутої цілі керується через CSS .goal-card-achieved .progress-bar

        // 6. Дата додавання цілі
        String dateAddedStr = (goal.getDateAdded() != null)
                ? goal.getDateAdded().format(AppUtil.USER_FRIENDLY_DATE_FORMATTER)
                : "Невідома дата";
        Label dateAddedLabel = new Label("Додано: " + dateAddedStr);
        dateAddedLabel.getStyleClass().add("goal-card-date-added");

        // 7. Кнопки керування (Редагувати, Видалити)
        Button editButton = new Button("Редагувати");
        editButton.getStyleClass().setAll("goal-card-button", "goal-card-edit-button"); // Встановлення стилів
        editButton.setOnAction(event -> {
            log.debug("Натиснуто 'Редагувати' для цілі ID: {}", goal.getId());
            if (primaryController != null) {
                primaryController.showAddEditGoalDialog(goal);
            }
        });

        Button deleteButton = new Button("Видалити");
        deleteButton.getStyleClass().setAll("goal-card-button", "goal-card-delete-button"); // Встановлення стилів
        deleteButton.setOnAction(event -> {
            log.debug("Натиснуто 'Видалити' для цілі ID: {}", goal.getId());
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
            confirmation.initOwner(getOwnerWindow());
            confirmation.setTitle("Підтвердження видалення");
            confirmation.setHeaderText("Видалити ціль: \"" + descriptionLabel.getText() + "\"?");
            confirmation.setContentText("Цю дію неможливо буде скасувати.");

            Optional<ButtonType> result = confirmation.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                try {
                    goalDao.deleteGoal(goal.getId());
                    log.info("Ціль ID={} успішно видалено.", goal.getId());
                    loadAndDisplayGoals(); // Оновити відображення списку цілей
                } catch (Exception e) {
                    log.error("Помилка видалення цілі ID={}", goal.getId(), e);
                    showErrorAlertHelper("Помилка видалення", "Не вдалося видалити ціль.", e.getMessage());
                }
            } else {
                log.debug("Видалення цілі ID={} скасовано користувачем.", goal.getId());
            }
        });

        HBox buttonBox = new HBox(10, editButton, deleteButton); // Горизонтальний контейнер для кнопок
        buttonBox.setAlignment(Pos.CENTER_RIGHT); // Вирівнювання кнопок праворуч

        // Додавання всіх елементів на картку
        card.getChildren().addAll(descriptionLabel, typeLabel, targetLabel, progressTextLabel, progressBar, dateAddedLabel, buttonBox);
        // Налаштування розтягування елементів
        VBox.setVgrow(descriptionLabel, Priority.NEVER);
        VBox.setVgrow(progressBar, Priority.NEVER);
        card.setAlignment(Pos.TOP_LEFT); // Вирівнювання вмісту картки

        return card;
    }

    /**
     * Генерує опис цілі за замовчуванням, якщо користувач його не вказав.
     * @param goal Об'єкт цілі.
     * @return Рядок згенерованого опису.
     */
    private String generateDefaultDescription(Goal goal) {
        if (goal == null) return "Ціль не визначена";

        Locale ukrainian = Locale.of("uk", "UA"); // Локаль для українських назв місяців
        String period = "";
        GoalType type = goal.getType();

        if (type == null) {
            log.warn("Тип цілі не визначений для ID: {}", goal.getId());
            return "Прочитати " + goal.getTargetValue() + " книг(и)"; // Базовий опис
        }

        switch (type) {
            case MONTHLY:
                if (goal.getYear() != null && goal.getMonth() != null) {
                    try {
                        period = "за " + Month.of(goal.getMonth()).getDisplayName(TextStyle.FULL_STANDALONE, ukrainian).toLowerCase() +
                                " " + goal.getYear() + "р.";
                    } catch (Exception e) {
                        log.warn("Некоректний місяць ({}) для цілі ID: {}. Помилка: {}", goal.getMonth(), goal.getId(), e.getMessage());
                        period = "за " + (goal.getMonth() != null ? goal.getMonth() : "невідомий місяць") + " " +
                                (goal.getYear() != null ? goal.getYear() + "р." : "невідомого року");
                    }
                } else {
                    period = "(місячна, період не вказано)";
                }
                break;
            case YEARLY:
                if (goal.getYear() != null) {
                    period = "за " + goal.getYear() + " рік";
                } else {
                    period = "(річна, рік не вказано)";
                }
                break;
            case TOTAL:
                period = "всього"; // Або можна залишити порожнім
                break;
            default:
                log.warn("Необроблюваний тип цілі: {} для цілі ID: {}", type, goal.getId());
                break;
        }
        return "Прочитати " + goal.getTargetValue() + " книг(и) " + period;
    }

    /**
     * Оновлює відображення списку цілей.
     * Викликається, наприклад, після додавання або редагування цілі з іншого діалогу.
     */
    public void refreshGoalsDisplay() {
        log.debug("Виклик refreshGoalsDisplay(), оновлення цілей...");
        loadAndDisplayGoals();
    }

    // --- Допоміжні методи для Alert ---

    /**
     * Отримує батьківське вікно для діалогових вікон (Alert).
     * @return Батьківське вікно або null, якщо не вдалося визначити.
     */
    private Window getOwnerWindow() {
        if (goalsFlowPane != null && goalsFlowPane.getScene() != null) {
            return goalsFlowPane.getScene().getWindow();
        }
        log.warn("Не вдалося визначити батьківське вікно для Alert у GoalsController.");
        return null;
    }

    /**
     * Показує діалогове вікно з повідомленням про помилку.
     * @param title Заголовок вікна.
     * @param header Заголовок повідомлення.
     * @param content Текст повідомлення про помилку.
     */
    private void showErrorAlertHelper(String title, String header, String content) {
        Alert errorAlert = new Alert(Alert.AlertType.ERROR);
        errorAlert.initOwner(getOwnerWindow());
        errorAlert.setTitle(title);
        errorAlert.setHeaderText(header);
        errorAlert.setContentText(content != null ? content : "Деталі відсутні.");
        errorAlert.showAndWait();
    }

    /**
     * Внутрішній статичний клас або константа для форматування дати.
     * Може бути винесений в окремий утилітний клас AppUtil, якщо використовується в багатьох місцях.
     */
    public static class AppUtil {
        public static final java.time.format.DateTimeFormatter USER_FRIENDLY_DATE_FORMATTER =
                java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy");
    }
}