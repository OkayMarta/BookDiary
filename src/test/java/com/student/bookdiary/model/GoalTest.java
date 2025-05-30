package com.student.bookdiary.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Тестовий клас для {@link Goal}.
 * Цей клас містить набір тестів для перевірки коректності роботи
 * конструкторів, гетерів, сетерів та методів equals, hashCode, toString класу Goal.
 */
class GoalTest {
    private Goal goal1_id0; // Місячна ціль з id=0, ініціалізується в setUp()
    private Goal goal2_id1; // Річна ціль з id=1L, ініціалізується в setUp()
    private Goal goal1;     // Місячна ціль, ініціалізується в setUp()
    private Goal goal2;     // Річна ціль, ініціалізується в setUp()
    private Goal goal3;     // Загальна ціль, ініціалізується в setUp()
    private final LocalDate TODAY = LocalDate.now(); // Поточна дата для використання в тестах
    private final LocalDate YESTERDAY = LocalDate.now().minusDays(1); // Вчорашня дата для використання в тестах

    /**
     * Налаштовує тестове середовище перед кожним тестом.
     * Ініціалізує об'єкти цілей (goal1_id0, goal2_id1, goal1, goal2, goal3)
     * з різними параметрами для тестування.
     */
    @BeforeEach
    void setUp() {
        goal1_id0 = new Goal("Прочитати 10 книг у липні", GoalType.MONTHLY, 10, 2024, 7);
        goal1_id0.setDateAdded(TODAY); // id залишається 0

        // Створюємо goal2_id1 з явно вказаним id
        goal2_id1 = new Goal(1L, "Річна ціль на 2024", GoalType.YEARLY, 50, 2024, null, YESTERDAY);

        // Ініціалізуємо змінні goal1, goal2, goal3
        goal1 = new Goal("Прочитати 10 книг у липні", GoalType.MONTHLY, 10, 2024, 7);
        // Конструктор Goal(description, type, targetValue, year, month) встановлює dateAdded = LocalDate.now().
        // Для консистентності тестів, ми явно встановлюємо дату.
        goal1.setDateAdded(TODAY);

        goal2 = new Goal("Річна ціль на 2024", GoalType.YEARLY, 50, 2024, null);
        goal2.setDateAdded(YESTERDAY); // Аналогічно, фіксуємо дату
        goal2.setId(1L); // Встановлюємо id=1 для goal2

        goal3 = new Goal("Загальна ціль 100 книг", GoalType.TOTAL, 100, null, null);
        goal3.setDateAdded(TODAY); // Аналогічно, фіксуємо дату
    }

    /**
     * Тестує конструктор за замовчуванням класу {@link Goal}.
     * Перевіряє, що новостворений об'єкт не є null, має id=0,
     * а інші поля ініціалізовані значеннями за замовчуванням (null або 0).
     */
    @Test
    void testDefaultConstructor() {
        Goal goal = new Goal();
        assertNotNull(goal, "Об'єкт цілі не повинен бути null після створення конструктором за замовчуванням.");
        assertEquals(0, goal.getId(), "ID цілі за замовчуванням має бути 0."); // Перевірка, що id за замовчуванням 0
        // Перевірка інших полів, які мають бути null або 0
        assertNull(goal.getDescription(), "Опис має бути null за замовчуванням.");
        assertNull(goal.getType(), "Тип має бути null за замовчуванням.");
        assertEquals(0, goal.getTargetValue(), "Цільове значення має бути 0 за замовчуванням.");
        assertNull(goal.getYear(), "Рік має бути null за замовчуванням.");
        assertNull(goal.getMonth(), "Місяць має бути null за замовчуванням.");
        assertNull(goal.getDateAdded(), "Дата додавання має бути null, оскільки конструктор за замовчуванням її не встановлює.");
    }

    /**
     * Тестує "простий" конструктор класу {@link Goal}, який приймає основні параметри.
     * Перевіряє коректність ініціалізації всіх полів цілі, включаючи автоматичне
     * встановлення дати додавання на поточну.
     */
    @Test
    void testSimpleConstructor() {
        // Конструктор типу Goal(description, type, targetValue, year, month)
        Goal newGoal = new Goal("Щомісячна ціль", GoalType.MONTHLY, 5, 2025, 1);
        assertEquals(0, newGoal.getId(), "ID має бути 0 для нової цілі."); // ID за замовчуванням для нових цілей
        assertEquals("Щомісячна ціль", newGoal.getDescription(), "Опис цілі має бути коректно встановлений.");
        assertEquals(GoalType.MONTHLY, newGoal.getType(), "Тип цілі має бути коректно встановлений.");
        assertEquals(5, newGoal.getTargetValue(), "Цільове значення має бути коректно встановлене.");
        assertEquals(2025, newGoal.getYear(), "Рік цілі має бути коректно встановлений.");
        assertEquals(1, newGoal.getMonth(), "Місяць цілі має бути коректно встановлений.");
        // Цей конструктор встановлює dateAdded на поточну дату
        assertEquals(LocalDate.now(), newGoal.getDateAdded(), "Дата додавання має автоматично встановлюватися на поточну дату.");
    }

    /**
     * Тестує "повний" конструктор класу {@link Goal}, який ініціалізує всі поля.
     * Перевіряє, що всі передані значення коректно присвоюються відповідним полям.
     */
    @Test
    void testFullConstructor() {
        // Конструктор типу Goal(id, description, type, targetValue, year, month, dateAdded)
        Goal fullGoal = new Goal(1L, "Річна ціль", GoalType.YEARLY, 50, 2024, null, TODAY);
        assertEquals(1L, fullGoal.getId(), "ID цілі має бути коректно встановлений.");
        assertEquals("Річна ціль", fullGoal.getDescription(), "Опис цілі має бути коректно встановлений.");
        assertEquals(GoalType.YEARLY, fullGoal.getType(), "Тип цілі має бути коректно встановлений.");
        assertEquals(50, fullGoal.getTargetValue(), "Цільове значення має бути коректно встановлене.");
        assertEquals(2024, fullGoal.getYear(), "Рік цілі має бути коректно встановлений.");
        assertNull(fullGoal.getMonth(), "Місяць має бути null для річної цілі, якщо передано null.");
        assertEquals(TODAY, fullGoal.getDateAdded(), "Дата додавання має бути коректно встановлена.");
    }

    /**
     * Тестує роботу гетерів та сетерів класу {@link Goal}.
     * Створює новий об'єкт, встановлює значення за допомогою сетерів
     * та перевіряє їх за допомогою гетерів.
     */
    @Test
    void testSettersAndGetters() {
        Goal goal = new Goal();
        LocalDate dateAdded = LocalDate.now();

        goal.setId(1L);
        goal.setDescription("Нова ціль");
        goal.setType(GoalType.TOTAL);
        goal.setTargetValue(100);
        goal.setYear(null); // Для загальної цілі рік може бути не вказаний
        goal.setMonth(null); // Для загальної цілі місяць може бути не вказаний
        goal.setDateAdded(dateAdded);

        assertAll("Перевірка гетерів після використання сетерів",
                () -> assertEquals(1L, goal.getId()),
                () -> assertEquals("Нова ціль", goal.getDescription()),
                () -> assertEquals(GoalType.TOTAL, goal.getType()),
                () -> assertEquals(100, goal.getTargetValue()),
                () -> assertNull(goal.getYear()),
                () -> assertNull(goal.getMonth()),
                () -> assertEquals(dateAdded, goal.getDateAdded())
        );
    }

    /**
     * Тестує метод {@link Goal#equals(Object)} в різних сценаріях:
     * <ul>
     *     <li>Рефлексивність (об'єкт рівний сам собі).</li>
     *     <li>Рівність об'єктів з однаковими ненульовими ID.</li>
     *     <li>Нерівність об'єктів, де один ID нульовий, а інший ні (і поля різні).</li>
     *     <li>Порівняння з null.</li>
     *     <li>Порівняння з об'єктом іншого типу.</li>
     *     <li>Рівність двох нових об'єктів (id=0) з однаковими полями (всі null або default).</li>
     * </ul>
     */
    @Test
    void testEquals() {
        Goal goalSameAsGoal2 = new Goal(1L, "Річна ціль на 2024", GoalType.YEARLY, 50, 2024, null, YESTERDAY);

        assertAll("Тестування методу equals",
                () -> assertTrue(goal2.equals(goal2), "Об'єкт має бути рівний самому собі (рефлексивність)."), // Рефлексивність: об'єкт має бути рівний самому собі (goal2.id = 1L)
                () -> assertTrue(goal2.equals(goalSameAsGoal2), "Об'єкти з однаковими ненульовими ID мають бути рівними."), // Об'єкти з однаковими ненульовими id мають бути рівними
                // goal1.id = 0, goal2.id = 1L.
                // Якщо один id=0, а інший ні, порівняння йде за полями. Поля у goal1 та goal2 різні.
                () -> assertFalse(goal1.equals(goal2), "Об'єкти мають бути нерівними, якщо один ID нульовий, інший ні, і поля різні."),
                () -> assertFalse(goal2.equals(null), "Порівняння з null має повертати false."), // Порівняння з null
                () -> assertFalse(goal2.equals("Not a goal"), "Порівняння з об'єктом іншого типу має повертати false."), // Порівняння з іншим типом
                // Порівняння двох нових об'єктів (id=0) з однаковими полями (всі null або default).
                () -> assertTrue(new Goal().equals(new Goal()), "Два нових об'єкти (id=0, поля за замовчуванням) мають бути рівними.")
        );
    }

    /**
     * Тестує метод {@link Goal#hashCode()} в різних сценаріях:
     * <ul>
     *     <li>Консистентність (повторний виклик hashCode для того ж об'єкта повертає те саме значення).</li>
     *     <li>Рівність хеш-кодів для об'єктів, які є рівними за методом equals (з однаковими ненульовими ID).</li>
     *     <li>Нерівність хеш-кодів для об'єктів, які не є рівними за методом equals (різні ID, один з яких може бути 0).</li>
     * </ul>
     */
    @Test
    void testHashCode() {
        Goal goalSameIdAsGoal2 = new Goal(1L, "Інша ціль з тим самим ID", GoalType.MONTHLY, 20, 2024, 8, TODAY);

        assertAll("Тестування методу hashCode",
                () -> assertEquals(goal2.hashCode(), goal2.hashCode(), "Хеш-код має бути консистентним."), // Консистентність: хеш-код має бути однаковим при повторних викликах
                // goal2.id = 1L, goalSameIdAsGoal2.id = 1L. Якщо equals true (бо id однакові), hashCode має бути однаковим.
                () -> assertEquals(goal2.hashCode(), goalSameIdAsGoal2.hashCode(), "Рівні об'єкти (за ненульовим ID) повинні мати однаковий хеш-код."),
                // goal1.id = 0, goal2.id = 1L. Якщо equals false, hashCode може бути різним.
                () -> assertNotEquals(goal1.hashCode(), goal2.hashCode(), "Нерівні об'єкти (різні ID, один може бути 0) повинні мати різні хеш-коди.")
        );
    }

    /**
     * Тестує метод {@link Goal#toString()} для цілі з визначеними полями.
     * Перевіряє, що рядкове представлення містить очікувані значення полів,
     * включаючи коректне відображення полів, що можуть бути null (наприклад, month).
     */
    @Test
    void testToString() {
        // goal2: "Річна ціль на 2024", YEARLY, id=1, year=2024, month=null, dateAdded=YESTERDAY
        String goalString = goal2.toString();
        assertAll("Тестування toString для цілі з визначеними полями",
                () -> assertNotNull(goalString, "toString() не повинен повертати null."),
                () -> assertTrue(goalString.contains("id=1"), "Рядок повинен містити ID."),
                () -> assertTrue(goalString.contains("description='Річна ціль на 2024'"), "Рядок повинен містити опис."),
                () -> assertTrue(goalString.contains("type=YEARLY"), "Рядок повинен містити тип."),
                () -> assertTrue(goalString.contains("targetValue=50"), "Рядок повинен містити цільове значення."),
                () -> assertTrue(goalString.contains("year=2024"), "Рядок повинен містити рік."),
                () -> assertFalse(goalString.contains(", month="), "Рядок не повинен містити 'month=', якщо місяць null."), // Місяць null, тому не має бути у виводі
                () -> assertTrue(goalString.contains("dateAdded=" + YESTERDAY.toString()), "Рядок повинен містити дату додавання.")
        );
    }

    /**
     * Тестує метод {@link Goal#equals(Object)} для нових цілей (з id = 0)
     * при зміні різних полів. Коли id обох об'єктів дорівнює 0,
     * порівняння має відбуватися за всіма полями.
     */
    @Test
    void testEquals_newGoals_differentFields() {
        // Тестування методу equals для об'єктів з id=0, коли змінюються інші поля.
        // Порівнюються всі поля: description, type, targetValue, year, month, dateAdded.

        Goal g1 = new Goal("Desc", GoalType.MONTHLY, 10, 2024, 1);
        g1.setDateAdded(TODAY); // Фіксуємо dateAdded, оскільки конструктор встановлює LocalDate.now()
        Goal g2 = new Goal("Desc", GoalType.MONTHLY, 10, 2024, 1);
        g2.setDateAdded(TODAY); // Фіксуємо dateAdded
        assertTrue(g1.equals(g2), "Об'єкти з id=0 та однаковими полями мають бути рівними.");

        // Перевірка поля description
        g2.setDescription("Diff Desc");
        assertFalse(g1.equals(g2), "Має бути false, якщо описи відрізняються.");
        g1.setDescription(null); // g1.description = null, g2.description = "Diff Desc"
        assertFalse(g1.equals(g2), "Має бути false, якщо один опис null, а інший ні.");
        g2.setDescription(null); // g1.description = null, g2.description = null
        assertTrue(g1.equals(g2), "Має бути true, якщо обидва описи null.");
        g1.setDescription("Desc"); // Скидання значення
        g2.setDescription("Desc"); // Скидання значення
        assertTrue(g1.equals(g2), "Об'єкти мають бути рівними після скидання опису.");


        // Перевірка поля type
        g2.setType(GoalType.YEARLY);
        assertFalse(g1.equals(g2), "Має бути false, якщо типи відрізняються.");
        g2.setType(GoalType.MONTHLY); // Скидання значення

        // Перевірка поля targetValue
        g2.setTargetValue(11);
        assertFalse(g1.equals(g2), "Має бути false, якщо цільові значення відрізняються.");
        g2.setTargetValue(10); // Скидання значення

        // Перевірка поля year
        g2.setYear(2025);
        assertFalse(g1.equals(g2), "Має бути false, якщо роки відрізняються.");
        g1.setYear(null); // g1.year = null, g2.year = 2025
        assertFalse(g1.equals(g2), "Має бути false, якщо один рік null, а інший ні.");
        g2.setYear(null); // g1.year = null, g2.year = null
        assertTrue(g1.equals(g2), "Має бути true, якщо обидва роки null.");
        g1.setYear(2024); // Скидання значення
        g2.setYear(2024); // Скидання значення
        assertTrue(g1.equals(g2), "Об'єкти мають бути рівними після скидання року.");

        // Перевірка поля month
        g2.setMonth(2);
        assertFalse(g1.equals(g2), "Має бути false, якщо місяці відрізняються.");
        g1.setMonth(null); // g1.month = null, g2.month = 2
        assertFalse(g1.equals(g2), "Має бути false, якщо один місяць null, а інший ні.");
        g2.setMonth(null); // g1.month = null, g2.month = null
        assertTrue(g1.equals(g2), "Має бути true, якщо обидва місяці null.");
        g1.setMonth(1); // Скидання значення
        g2.setMonth(1); // Скидання значення
        assertTrue(g1.equals(g2), "Об'єкти мають бути рівними після скидання місяця.");

        // Перевірка поля dateAdded
        g2.setDateAdded(YESTERDAY);
        assertFalse(g1.equals(g2), "Має бути false, якщо дати додавання відрізняються.");
        g2.setDateAdded(TODAY); // Скидання значення
    }

    /**
     * Тестує метод {@link Goal#toString()} для різних комбінацій значень полів year та month.
     * Перевіряє, що поля year та month коректно включаються або виключаються з рядкового
     * представлення залежно від їх значень (null або не null).
     */
    @Test
    void testToString_variousYearMonthCombinations() {
        // 1. Місячна ціль (goal1: year != null, month != null)
        String goal1String = goal1.toString();
        assertAll("ToString для місячної цілі (рік та місяць визначені)",
                () -> assertTrue(goal1String.contains("id=0")),
                () -> assertTrue(goal1String.contains("description='Прочитати 10 книг у липні'")),
                () -> assertTrue(goal1String.contains("type=MONTHLY")),
                () -> assertTrue(goal1String.contains("targetValue=10")),
                () -> assertTrue(goal1String.contains("year=2024")),   // Рік має бути присутній
                () -> assertTrue(goal1String.contains("month=7")),     // Місяць має бути присутній
                () -> assertTrue(goal1String.contains("dateAdded=" + TODAY.toString()))
        );

        // 2. Річна ціль (goal2: year != null, month == null)
        String goal2String = goal2.toString();
        assertAll("ToString для річної цілі (рік визначений, місяць null)",
                () -> assertTrue(goal2String.contains("id=1")),
                () -> assertTrue(goal2String.contains("description='Річна ціль на 2024'")),
                () -> assertTrue(goal2String.contains("type=YEARLY")),
                () -> assertTrue(goal2String.contains("targetValue=50")),
                () -> assertTrue(goal2String.contains("year=2024")),   // Рік має бути присутній
                // Перевірка, що рядок "month=" відсутній, оскільки місяць = null
                () -> assertFalse(goal2String.contains(", month="), "Поле 'month' не повинно бути у виводі для річної цілі, якщо місяць null."),
                () -> assertTrue(goal2String.contains("dateAdded=" + YESTERDAY.toString()))
        );


        // 3. Загальна ціль (goal3: year == null, month == null)
        String goal3String = goal3.toString();
        assertAll("ToString для загальної цілі (рік та місяць null)",
                () -> assertTrue(goal3String.contains("id=0")),
                () -> assertTrue(goal3String.contains("description='Загальна ціль 100 книг'")),
                () -> assertTrue(goal3String.contains("type=TOTAL")),
                () -> assertTrue(goal3String.contains("targetValue=100")),
                // Перевірка, що рядок "year=" відсутній, оскільки рік = null
                () -> assertFalse(goal3String.contains(", year="), "Поле 'year' не повинно бути у виводі для загальної цілі, якщо рік null."),
                // Перевірка, що рядок "month=" відсутній, оскільки місяць = null
                () -> assertFalse(goal3String.contains(", month="), "Поле 'month' не повинно бути у виводі для загальної цілі, якщо місяць null."),
                () -> assertTrue(goal3String.contains("dateAdded=" + TODAY.toString()))
        );

        // 4. Ціль з null типом та іншими null полями (створена конструктором за замовчуванням)
        Goal goalWithNulls = new Goal(); // id=0, targetValue=0, description=null, type=null, year=null, month=null, dateAdded=null
        goalWithNulls.setDescription("Test Description with Nulls"); // Встановлюємо опис

        String nullGoalString = goalWithNulls.toString();
        assertAll("ToString для цілі з переважно null полями (створеної конструктором за замовчуванням)",
                () -> assertTrue(nullGoalString.contains("id=0")),
                () -> assertTrue(nullGoalString.contains("description='Test Description with Nulls'")),
                () -> assertTrue(nullGoalString.contains("type=null")), // type=null, оскільки поле type є null
                () -> assertTrue(nullGoalString.contains("targetValue=0")),
                () -> assertFalse(nullGoalString.contains(", year="), "Поле 'year' не повинно бути у виводі, якщо null."),
                () -> assertFalse(nullGoalString.contains(", month="), "Поле 'month' не повинно бути у виводі, якщо null."),
                () -> assertTrue(nullGoalString.contains("dateAdded=null")) // dateAdded=null, оскільки не було ініціалізовано
        );
    }

    /**
     * Тестує метод {@link Goal#hashCode()}, коли ID об'єктів ненульові та однакові.
     * Хеш-коди повинні бути однаковими.
     */
    @Test
    void testHashCode_whenIdIsNonZeroAndEqual() {
        // goal2_id1 має id = 1L
        Goal anotherGoalWithId1 = new Goal(1L, "Інша ціль", GoalType.MONTHLY, 20, 2024, 8, TODAY);
        assertEquals(goal2_id1.hashCode(), anotherGoalWithId1.hashCode(), "Хеш-коди мають бути однаковими для об'єктів з однаковими ненульовими ID.");
    }

    /**
     * Тестує метод {@link Goal#hashCode()}, коли ID об'єктів ненульові, але різні.
     * Хеш-коди, як правило, повинні бути різними.
     */
    @Test
    void testHashCode_whenIdIsNonZeroAndDifferent() {
        // goal2_id1 має id = 1L
        Goal goalWithDifferentId = new Goal(2L, "Річна ціль на 2024", GoalType.YEARLY, 50, 2024, null, YESTERDAY);
        assertNotEquals(goal2_id1.hashCode(), goalWithDifferentId.hashCode(), "Хеш-коди, як правило, мають бути різними для об'єктів з різними ненульовими ID.");
    }

    /**
     * Тестує метод {@link Goal#hashCode()}, коли ID одного об'єкта нульовий, а іншого - ні.
     * Хеш-коди, як правило, повинні бути різними, оскільки логіка їх обчислення відрізняється.
     */
    @Test
    void testHashCode_whenOneIdIsZero() {
        // goal1_id0 має id = 0
        // goal2_id1 має id = 1L
        // Якщо один id = 0, а інший ні, хеш-коди зазвичай різні,
        // оскільки логіка генерації хеш-коду відрізняється (за id або за полями).
        assertNotEquals(goal1_id0.hashCode(), goal2_id1.hashCode(), "Хеш-коди, як правило, мають бути різними, якщо ID одного об'єкта нульовий, а іншого ні.");
    }

    /**
     * Тестує метод {@link Goal#hashCode()}, коли ID обох об'єктів нульові, а інші поля однакові.
     * Хеш-коди повинні бути однаковими.
     */
    @Test
    void testHashCode_whenBothIdsAreZeroAndFieldsAreEqual() {
        Goal g1 = new Goal("Desc", GoalType.MONTHLY, 10, 2024, 1);
        // Важливо для консистентності, якщо dateAdded враховується і встановлюється конструктором
        g1.setDateAdded(TODAY);
        Goal g2 = new Goal("Desc", GoalType.MONTHLY, 10, 2024, 1);
        g2.setDateAdded(TODAY); // Важливо для консистентності
        assertEquals(g1.hashCode(), g2.hashCode(), "Хеш-коди мають бути однаковими для об'єктів з id=0 та однаковими полями.");
    }

    /**
     * Тестує метод {@link Goal#hashCode()}, коли ID обох об'єктів нульові, а інші поля різні.
     * Хеш-коди, як правило, повинні бути різними.
     */
    @Test
    void testHashCode_whenBothIdsAreZeroAndFieldsAreDifferent() {
        Goal g1 = new Goal("Desc1", GoalType.MONTHLY, 10, 2024, 1);
        g1.setDateAdded(TODAY);
        Goal g2 = new Goal("Desc2", GoalType.YEARLY, 12, 2025, 2); // Різні поля
        g2.setDateAdded(YESTERDAY);
        assertNotEquals(g1.hashCode(), g2.hashCode(), "Хеш-коди, як правило, мають бути різними для об'єктів з id=0 та різними полями.");
    }
}