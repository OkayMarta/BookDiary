package com.student.bookdiary.ui;

import com.student.bookdiary.persistence.BookDao;
import com.student.bookdiary.persistence.SqliteBookDao;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Контролер для відображення статистики читання книжок.
 * Відповідає за візуалізацію даних про кількість прочитаних книжок
 * за різні періоди часу.
 */
public class StatsController {

    private static final Logger log = LoggerFactory.getLogger(StatsController.class);

    /** Елементи інтерфейсу, що ін'єктуються через FXML */
    @FXML
    private ComboBox<String> monthComboBox;

    @FXML
    private ComboBox<Integer> yearComboBox;

    @FXML
    private Label totalBooksReadLabel;

    @FXML
    private Label booksReadInYearLabel;

    @FXML
    private Label booksReadInMonthLabel;

    /** Об'єкт доступу до даних про книжки */
    private BookDao bookDao;

    /** Список українських назв місяців */
    private final List<String> monthNamesUkrainian = new ArrayList<>();

    /**
     * Ініціалізує контролер після завантаження FXML.
     * Налаштовує компоненти інтерфейсу та завантажує початкові дані.
     */
    @FXML
    private void initialize() {
        this.bookDao = new SqliteBookDao();

        setupMonthComboBox();
        setupYearComboBox();

        // Налаштування обробників подій для комбобоксів
        monthComboBox.valueProperty().addListener((_, _, newVal) -> {
            if (newVal != null) {
                log.debug("Обрано новий місяць для відображення статистики: {}", newVal);
                loadAndDisplayStats();
            }
        });

        yearComboBox.valueProperty().addListener((_, _, newVal) -> {
            if (newVal != null) {
                log.debug("Обрано новий рік для відображення статистики: {}", newVal);
                loadAndDisplayStats();
            }
        });

        loadAndDisplayStats();
    }

    /**
     * Налаштовує комбобокс для вибору місяця.
     * Заповнює список українськими назвами місяців та встановлює поточний місяць.
     */
    private void setupMonthComboBox() {
        Locale ukrainianLocale = Locale.of("uk", "UA");
        for (Month month : Month.values()) {
            monthNamesUkrainian.add(month.getDisplayName(TextStyle.FULL_STANDALONE, ukrainianLocale));
        }
        monthComboBox.setItems(FXCollections.observableArrayList(monthNamesUkrainian));

        String currentMonthName = LocalDate.now().getMonth().getDisplayName(TextStyle.FULL_STANDALONE, ukrainianLocale);
        monthComboBox.setValue(currentMonthName);
    }

    /**
     * Налаштовує комбобокс для вибору року.
     * Створює список років від поточного до 100 років назад.
     */
    private void setupYearComboBox() {
        int currentYear = LocalDate.now().getYear();
        List<Integer> years = IntStream.rangeClosed(currentYear - 100, currentYear)
                .boxed()
                .sorted((a, b) -> Integer.compare(b, a))
                .collect(Collectors.toList());
        
        yearComboBox.setItems(FXCollections.observableArrayList(years));
        yearComboBox.setValue(currentYear);
    }

    /**
     * Завантажує та відображає статистику читання для обраного періоду.
     * Оновлює інформацію про загальну кількість книжок, кількість за рік та місяць.
     */
    private void loadAndDisplayStats() {
        String selectedMonthName = monthComboBox.getValue();
        Integer selectedYear = yearComboBox.getValue();

        if (selectedMonthName == null || selectedYear == null) {
            log.warn("Не вдалося завантажити статистику: місяць або рік не обрано");
            clearStats();
            return;
        }

        try {
            updateTotalStats(selectedYear, selectedMonthName);
        } catch (Exception e) {
            log.error("Помилка при завантаженні статистики: {}", e.getMessage());
            clearStats();
        }
    }

    /**
     * Оновлює статистику для всіх періодів.
     *
     * @param selectedYear обраний рік
     * @param selectedMonthName назва обраного місяця
     */
    private void updateTotalStats(int selectedYear, String selectedMonthName) {
        // Загальна статистика
        int totalBooksRead = bookDao.getTotalBooksReadCount();
        totalBooksReadLabel.setText(String.valueOf(totalBooksRead));

        // Статистика за рік
        int booksInYear = bookDao.getBooksReadCountByYear(selectedYear);
        booksReadInYearLabel.setText(String.valueOf(booksInYear));

        // Статистика за місяць
        int monthNumber = monthNamesUkrainian.indexOf(selectedMonthName) + 1;
        if (monthNumber > 0) {
            int booksInMonth = bookDao.getBooksReadCountByMonthAndYear(selectedYear, monthNumber);
            booksReadInMonthLabel.setText(String.valueOf(booksInMonth));
        } else {
            log.error("Некоректний місяць: {}", selectedMonthName);
            booksReadInMonthLabel.setText("0");
        }
    }

    /**
     * Очищає всі поля статистики.
     */
    private void clearStats() {
        totalBooksReadLabel.setText("-");
        booksReadInYearLabel.setText("-");
        booksReadInMonthLabel.setText("-");
    }
}