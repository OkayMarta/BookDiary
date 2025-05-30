package com.student.bookdiary.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Клас, що представляє ціль читання в читацькому щоденнику.
 * Містить інформацію про опис цілі, її тип (місячна, річна, загальна),
 * цільову кількість книг, період (рік/місяць) та дату створення.
 */
public class Goal {

    // --- Поля класу ---

    /**
     * Унікальний ідентифікатор цілі з бази даних.
     * Значення 0 вказує, що ціль ще не була збережена в БД.
     */
    private long id;

    /**
     * Опис цілі, наданий користувачем.
     * Наприклад, "Прочитати 10 книг за липень" або "Річна ціль на 2024 рік".
     */
    private String description;

    /**
     * Тип цілі, що визначає її періодичність або характер.
     * Використовує перелік {@link GoalType} (наприклад, MONTHLY, YEARLY, TOTAL).
     */
    private GoalType type;

    /**
     * Цільова кількість книг, яку користувач планує прочитати для досягнення цієї цілі.
     */
    private int targetValue;

    /**
     * Рік, до якого відноситься ціль.
     * Має значення null для цілей типу TOTAL (загалом).
     * Для MONTHLY та YEARLY цілей це поле вказує конкретний рік.
     */
    private Integer year;

    /**
     * Місяць, до якого відноситься ціль (значення від 1 до 12).
     * Має значення null для цілей типу YEARLY (річна) та TOTAL (загалом).
     * Для MONTHLY цілей це поле вказує конкретний місяць.
     */
    private Integer month;

    /**
     * Дата створення (додавання) цілі до щоденника.
     */
    private LocalDate dateAdded;

    // --- Конструктори ---

    /**
     * Порожній конструктор.
     * Дозволяє створювати об'єкт Goal без початкової ініціалізації полів.
     * Може використовуватися бібліотеками для десеріалізації або для ручного заповнення полів.
     */
    public Goal() {
    }

    /**
     * Конструктор для створення нового запису про ціль перед збереженням у базу даних.
     * Автоматично встановлює поточну дату створення цілі.
     * Поле `id` буде встановлено після збереження цілі в базу даних.
     *
     * @param description Опис цілі.
     * @param type Тип цілі (MONTHLY, YEARLY, TOTAL).
     * @param targetValue Цільова кількість книг для досягнення.
     * @param year Рік, до якого відноситься ціль (може бути null, залежно від типу).
     * @param month Місяць, до якого відноситься ціль (1-12, може бути null, залежно від типу).
     */
    public Goal(String description, GoalType type, int targetValue, Integer year, Integer month) {
        this.description = description;
        this.type = type;
        this.targetValue = targetValue;
        this.year = year;
        this.month = month;
        this.dateAdded = LocalDate.now(); // Автоматичне встановлення поточної дати створення
    }

    /**
     * Повний конструктор для створення об'єкта Goal з усіма полями.
     * Зазвичай використовується для завантаження існуючого запису про ціль з бази даних.
     *
     * @param id Унікальний ідентифікатор цілі з БД.
     * @param description Опис цілі.
     * @param type Тип цілі.
     * @param targetValue Цільова кількість книг.
     * @param year Рік, до якого відноситься ціль.
     * @param month Місяць, до якого відноситься ціль (1-12).
     * @param dateAdded Дата створення цілі.
     */
    public Goal(long id, String description, GoalType type, int targetValue,
                Integer year, Integer month, LocalDate dateAdded) {
        this.id = id;
        this.description = description;
        this.type = type;
        this.targetValue = targetValue;
        this.year = year;
        this.month = month;
        this.dateAdded = dateAdded;
    }

    // --- Гетери ---

    public long getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public GoalType getType() {
        return type;
    }

    public int getTargetValue() {
        return targetValue;
    }

    public Integer getYear() {
        return year;
    }

    public Integer getMonth() {
        return month;
    }

    public LocalDate getDateAdded() {
        return dateAdded;
    }

    // --- Сетери ---

    /**
     * Встановлює унікальний ідентифікатор цілі.
     * Зазвичай використовується об'єктами доступу до даних (DAO)
     * після збереження цілі в базу даних та отримання згенерованого ID.
     *
     * @param id Новий ідентифікатор цілі.
     */
    public void setId(long id) {
        this.id = id;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setType(GoalType type) {
        this.type = type;
    }

    public void setTargetValue(int targetValue) {
        this.targetValue = targetValue;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }

    public void setDateAdded(LocalDate dateAdded) {
        this.dateAdded = dateAdded;
    }

    // --- Методи equals, hashCode, toString ---

    /**
     * Порівнює цей об'єкт Goal з іншим об'єктом на рівність.
     * Два об'єкти Goal вважаються рівними, якщо їхні поля `id` збігаються
     * і не дорівнюють 0 (тобто обидві цілі вже збережені в БД і мають унікальні ідентифікатори).
     * Якщо `id` однієї або обох цілей дорівнює 0, порівняння відбувається за посиланням (ідентичність об'єктів).
     *
     * @param o Об'єкт для порівняння.
     * @return true, якщо об'єкти рівні, false - в іншому випадку.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Goal goal = (Goal) o;
        
        // Якщо обидва об'єкти мають id=0, порівнюємо всі поля
        if (id == 0 && goal.id == 0) {
            return Objects.equals(description, goal.description) &&
                   type == goal.type &&
                   targetValue == goal.targetValue &&
                   Objects.equals(year, goal.year) &&
                   Objects.equals(month, goal.month) &&
                   Objects.equals(dateAdded, goal.dateAdded);
        }
        // Інакше порівнюємо за id, якщо вони не нульові
        return id != 0 && id == goal.id;
    }

    /**
     * Генерує хеш-код для об'єкта Goal.
     * Хеш-код базується на полі `id`, щоб забезпечити узгодженість з методом `equals`.
     * Якщо `id` дорівнює 0, хеш-код буде розрахований стандартним чином на основі посилання на об'єкт.
     *
     * @return Хеш-код об'єкта.
     */
    @Override
    public int hashCode() {
        // Якщо id = 0, використовуємо всі поля для обчислення хеш-коду
        if (id == 0) {
            return Objects.hash(description, type, targetValue, year, month, dateAdded);
        }
        // Інакше використовуємо тільки id
        return Objects.hash(id);
    }

    /**
     * Повертає рядкове представлення об'єкта Goal.
     * Використовується переважно для цілей відлагодження та логування.
     *
     * @return Рядкове представлення, що містить значення всіх полів об'єкта.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Goal{");
        sb.append("id=").append(id)
          .append(", description='").append(description).append('\'')
          .append(", type=").append(type != null ? type.name() : "null")
          .append(", targetValue=").append(targetValue);
        
        if (year != null) {
            sb.append(", year=").append(year);
        }
        if (month != null) {
            sb.append(", month=").append(month);
        }
        
        sb.append(", dateAdded=").append(dateAdded)
          .append('}');
        
        return sb.toString();
    }
}