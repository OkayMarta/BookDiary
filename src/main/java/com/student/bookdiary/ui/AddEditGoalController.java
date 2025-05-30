package com.student.bookdiary.ui;

import com.student.bookdiary.model.Goal;
import com.student.bookdiary.model.GoalType;
import com.student.bookdiary.persistence.DataAccessException; // Припускаємо, що цей виняток існує
import com.student.bookdiary.persistence.GoalDao;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

/**
 * Контролер для діалогового вікна додавання або редагування цілі читання.
 * Керує введенням даних про ціль, вибором її типу, періоду та збереженням.
 */
public class AddEditGoalController {

    private static final Logger log = LoggerFactory.getLogger(AddEditGoalController.class);
    private static final Locale UKRAINIAN_LOCALE = Locale.of("uk", "UA");

    // --- Поля, ін'єктовані з FXML ---
    @FXML private Label dialogTitleLabel;
    @FXML private TextField descriptionField;
    @FXML private ComboBox<GoalType> typeComboBox;
    @FXML private Spinner<Integer> targetValueSpinner;
    @FXML private Label yearLabel;
    @FXML private ComboBox<Integer> yearComboBox;
    @FXML private Label monthLabel;
    @FXML private ComboBox<String> monthComboBox; // Зберігає назви місяців для відображення
    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    // Обмеження для рядків GridPane, що містять поля року та місяця.
    // Використовуються для динамічної зміни висоти рядків.
    @FXML private RowConstraints yearRow;
    @FXML private RowConstraints monthRow;

    // --- Внутрішні поля класу ---
    private Stage dialogStage; // Посилання на вікно (Stage) цього діалогу
    private GoalDao goalDao;   // Об'єкт доступу до даних для цілей
    private Goal goalToEdit;   // Ціль для редагування; null, якщо додається нова
    private Runnable refreshCallback; // Callback для оновлення основного списку цілей

    // Списки для зберігання назв місяців та відповідних їм значень enum Month.
    // Це потрібно для коректного перетворення обраної назви місяця в його числовий еквівалент.
    private final List<String> monthDisplayNames = new ArrayList<>();
    private final List<Month> monthEnumValues = new ArrayList<>();

    /**
     * Метод ініціалізації контролера. Викликається JavaFX після завантаження FXML.
     * Налаштовує елементи керування: заповнює випадаючі списки для типу цілі, року, місяця,
     * встановлює фабрику значень для Spinner та слухачі подій.
     */
    @FXML
    private void initialize() {
        log.debug("Ініціалізація AddEditGoalController...");

        // Налаштування ComboBox для вибору типу цілі
        typeComboBox.setItems(FXCollections.observableArrayList(GoalType.values()));
        typeComboBox.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(GoalType type) {
                return type == null ? "" : type.getDisplayName();
            }
            @Override public GoalType fromString(String string) {
                // Пошук GoalType за його відображуваною назвою
                for (GoalType type : GoalType.values()) {
                    if (type.getDisplayName().equals(string)) {
                        return type;
                    }
                }
                return null; // Якщо не знайдено
            }
        });

        // Налаштування ComboBox для вибору року
        int currentYear = LocalDate.now().getYear();
        List<Integer> years = IntStream.rangeClosed(currentYear - 5, currentYear + 5) // Діапазон років: +/- 5 від поточного
                .boxed()
                .sorted(Comparator.reverseOrder()) // Сортування у зворотному порядку (новіші роки зверху)
                .toList(); // Використання toList() з Java 16+
        yearComboBox.setItems(FXCollections.observableArrayList(years));
        yearComboBox.setValue(currentYear); // Встановлення поточного року за замовчуванням

        // Налаштування ComboBox для вибору місяця
        for (Month month : Month.values()) {
            monthDisplayNames.add(month.getDisplayName(TextStyle.FULL_STANDALONE, UKRAINIAN_LOCALE));
            monthEnumValues.add(month);
        }
        monthComboBox.setItems(FXCollections.observableArrayList(monthDisplayNames));
        monthComboBox.setValue(LocalDate.now().getMonth().getDisplayName(TextStyle.FULL_STANDALONE, UKRAINIAN_LOCALE));

        // Налаштування Spinner для вибору цільової кількості книг
        SpinnerValueFactory<Integer> valueFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1000, 5); // min, max, initialValue
        targetValueSpinner.setValueFactory(valueFactory);

        // Слухач для зміни типу цілі, який оновлює видимість полів року та місяця
        typeComboBox.valueProperty().addListener((_, _, newType) -> updateFieldsVisibility(newType));

        updateFieldsVisibility(null); // Початкове приховування полів року/місяця
        log.debug("AddEditGoalController успішно ініціалізовано.");
    }

    /**
     * Оновлює видимість полів вибору року та місяця залежно від обраного типу цілі.
     * Також керує висотою відповідних рядків у GridPane.
     *
     * @param type Обраний тип цілі ({@link GoalType}).
     */
    private void updateFieldsVisibility(GoalType type) {
        boolean yearElementsVisible = false;
        boolean monthElementsVisible = false;

        if (type != null) {
            switch (type) {
                case MONTHLY:
                    yearElementsVisible = true;
                    monthElementsVisible = true;
                    break;
                case YEARLY:
                    yearElementsVisible = true;
                    // monthElementsVisible залишається false
                    break;
                case TOTAL:
                    // yearElementsVisible та monthElementsVisible залишаються false
                    break;
                default:
                    log.warn("Обробка невідомого типу цілі: {}", type);
            }
        }

        // Оновлення видимості та керованості для групи "Рік"
        updateRowVisibility(yearLabel, yearComboBox, yearRow, yearElementsVisible);
        // Оновлення видимості та керованості для групи "Місяць"
        updateRowVisibility(monthLabel, monthComboBox, monthRow, monthElementsVisible);

        log.debug("Оновлено видимість полів: рік={}, місяць={} для типу цілі: {}",
                yearElementsVisible, monthElementsVisible, type);
    }

    /**
     * Допоміжний метод для оновлення видимості, керованості та висоти рядка в GridPane.
     *
     * @param label Мітка, пов'язана з полем.
     * @param comboBox Випадаючий список у рядку.
     * @param row Обмеження для рядка GridPane.
     * @param isVisible {@code true}, якщо рядок має бути видимим, {@code false} - в іншому випадку.
     */
    private void updateRowVisibility(Label label, ComboBox<?> comboBox, RowConstraints row, boolean isVisible) {
        label.setVisible(isVisible);
        label.setManaged(isVisible); // Якщо невидимий, не займає місце в компонуванні
        comboBox.setVisible(isVisible);
        comboBox.setManaged(isVisible);

        if (row != null) {
            if (isVisible) {
                row.setPercentHeight(-1); // Дозволити GridPane обчислювати висоту
                row.setMinHeight(Region.USE_COMPUTED_SIZE);
                row.setPrefHeight(Region.USE_COMPUTED_SIZE);
            } else {
                // Сховати рядок, встановивши його висоту в 0
                row.setPercentHeight(0);
                row.setMinHeight(0);
                row.setPrefHeight(0);
            }
        } else {
            log.warn("RowConstraints для мітки '{}' не було ін'єктовано (null).", label.getText());
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
     * Встановлює об'єкт доступу до даних (DAO) для цілей.
     * @param goalDao Реалізація {@link GoalDao}.
     */
    public void setGoalDao(GoalDao goalDao) {
        this.goalDao = goalDao;
    }

    /**
     * Встановлює callback-функцію, яка буде викликана після успішного збереження цілі.
     * @param refreshCallback Функція для оновлення.
     */
    public void setRefreshCallback(Runnable refreshCallback) {
        this.refreshCallback = refreshCallback;
    }

    /**
     * Встановлює ціль для редагування або ініціалізує форму для додавання нової цілі.
     * Якщо {@code goal} дорівнює null, форма налаштовується для створення нової цілі.
     * В іншому випадку, поля форми заповнюються даними з {@code goal}.
     *
     * @param goal Ціль для редагування, або null для додавання нової.
     */
    public void setGoalToEdit(Goal goal) {
        this.goalToEdit = goal;
        if (goal != null) { // Режим редагування
            log.debug("Встановлення даних для редагування цілі ID={}: '{}'", goal.getId(), goal.getDescription());
            dialogTitleLabel.setText("Редагувати ціль");
            descriptionField.setText(goal.getDescription());
            typeComboBox.setValue(goal.getType());
            targetValueSpinner.getValueFactory().setValue(goal.getTargetValue());

            yearComboBox.setValue(goal.getYear() != null ? goal.getYear() : LocalDate.now().getYear());
            monthComboBox.setValue(goal.getMonth() != null ?
                    Month.of(goal.getMonth()).getDisplayName(TextStyle.FULL_STANDALONE, UKRAINIAN_LOCALE) :
                    LocalDate.now().getMonth().getDisplayName(TextStyle.FULL_STANDALONE, UKRAINIAN_LOCALE));
            updateFieldsVisibility(goal.getType()); // Оновлюємо видимість на основі типу цілі, що редагується
        } else { // Режим додавання нової цілі
            log.debug("Ініціалізація форми для додавання нової цілі.");
            dialogTitleLabel.setText("Додати нову ціль");
            descriptionField.clear();
            typeComboBox.setValue(null); // Тип не обрано
            targetValueSpinner.getValueFactory().setValue(5); // Значення за замовчуванням
            yearComboBox.setValue(LocalDate.now().getYear());
            monthComboBox.setValue(LocalDate.now().getMonth().getDisplayName(TextStyle.FULL_STANDALONE, UKRAINIAN_LOCALE));
            updateFieldsVisibility(null); // Приховуємо поля року/місяця
        }
    }

    /**
     * Обробляє натискання кнопки "Зберегти".
     * Валідує введені дані, створює новий або оновлює існуючий об'єкт {@link Goal}
     * та зберігає його в БД.
     *
     * @param actionEvent Подія натискання кнопки.
     */
    @FXML
    private void handleSave(ActionEvent actionEvent) {
        if (!isInputValid()) {
            return; // Якщо дані невалідні, перериваємо збереження
        }

        GoalType selectedType = typeComboBox.getValue();
        Integer year = null;
        Integer month = null;

        if (selectedType == GoalType.YEARLY || selectedType == GoalType.MONTHLY) {
            if (yearComboBox.isVisible() && yearComboBox.getValue() != null) {
                year = yearComboBox.getValue();
            }
        }
        if (selectedType == GoalType.MONTHLY) {
            if (monthComboBox.isVisible() && monthComboBox.getValue() != null) {
                int monthIndex = monthDisplayNames.indexOf(monthComboBox.getValue());
                if (monthIndex != -1) {
                    month = monthEnumValues.get(monthIndex).getValue(); // Отримуємо числове значення місяця (1-12)
                } else {
                    log.warn("Не вдалося визначити номер місяця для назви: {}", monthComboBox.getValue());
                    // Можна додати обробку помилки, якщо місяць не розпізнано
                }
            }
        }

        String description = descriptionField.getText() != null ? descriptionField.getText().trim() : "";
        int target = targetValueSpinner.getValue();

        try {
            if (goalToEdit == null) { // Створення нової цілі
                Goal newGoal = new Goal(description, selectedType, target, year, month);
                goalDao.addGoal(newGoal);
                log.info("Нову ціль '{}' (ID={}) успішно додано.", newGoal.getDescription(), newGoal.getId());
            } else { // Оновлення існуючої цілі
                goalToEdit.setDescription(description);
                goalToEdit.setType(selectedType);
                goalToEdit.setTargetValue(target);
                goalToEdit.setYear(year);
                goalToEdit.setMonth(month);
                // dateAdded не оновлюється при редагуванні, залишається дата створення
                goalDao.updateGoal(goalToEdit);
                log.info("Ціль '{}' (ID={}) успішно оновлено.", goalToEdit.getDescription(), goalToEdit.getId());
            }

            if (refreshCallback != null) {
                refreshCallback.run(); // Виклик callback для оновлення основного UI
            }
            dialogStage.close(); // Закриття діалогового вікна

        } catch (DataAccessException e) { // Обробка власного винятку
            log.error("Помилка збереження цілі в БД: {}", e.getMessage(), e);
            showErrorAlert("Помилка Збереження", "Не вдалося зберегти дані цілі.", e.getMessage());
        }
    }

    /**
     * Перевіряє валідність введених даних у формі.
     * @return true, якщо дані валідні, false - в іншому випадку (з показом Alert).
     */
    private boolean isInputValid() {
        StringBuilder errorMessage = new StringBuilder();

        if (typeComboBox.getValue() == null) {
            errorMessage.append("Не обрано тип цілі!\n");
        }

        GoalType selectedType = typeComboBox.getValue();
        if (selectedType != null) {
            if ((selectedType == GoalType.YEARLY || selectedType == GoalType.MONTHLY) &&
                    (yearComboBox.getValue() == null && yearComboBox.isVisible())) {
                errorMessage.append("Не обрано рік для річної/місячної цілі!\n");
            }
            if (selectedType == GoalType.MONTHLY &&
                    (monthComboBox.getValue() == null && monthComboBox.isVisible())) {
                errorMessage.append("Не обрано місяць для місячної цілі!\n");
            }
        }

        if (targetValueSpinner.getValue() == null || targetValueSpinner.getValue() < 1) {
            errorMessage.append("Кількість книг має бути позитивним числом (більше 0)!\n");
        }
        // Можна додати перевірку на максимальну довжину опису, якщо потрібно

        if (errorMessage.isEmpty()) {
            return true;
        } else {
            log.warn("Помилка валідації даних при збереженні цілі: {}", errorMessage.toString().replace("\n", " "));
            showErrorAlert("Некоректні Дані", "Будь ласка, виправте наступні помилки:", errorMessage.toString());
            return false;
        }
    }

    /**
     * Обробляє натискання кнопки "Скасувати". Закриває діалогове вікно.
     * @param actionEvent Подія натискання кнопки.
     */
    @FXML
    private void handleCancel(ActionEvent actionEvent) {
        log.debug("Натиснуто кнопку 'Скасувати'. Закриття діалогового вікна додавання/редагування цілі.");
        dialogStage.close();
    }

    /**
     * Відображає діалогове вікно з повідомленням про помилку.
     * @param title Заголовок вікна.
     * @param header Текст заголовка повідомлення.
     * @param content Текст основного вмісту повідомлення.
     */
    private void showErrorAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initOwner(dialogStage); // Встановлення батьківського вікна
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}