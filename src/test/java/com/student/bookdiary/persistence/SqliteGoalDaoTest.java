package com.student.bookdiary.persistence;

import com.student.bookdiary.model.Goal;
import com.student.bookdiary.model.GoalType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Клас для тестування реалізації {@link SqliteGoalDao}.
 * Розширює {@link BaseDaoTest} для використання спільної логіки налаштування та очищення тестового середовища бази даних.
 */
class SqliteGoalDaoTest extends BaseDaoTest {

    // DAO об'єкт для взаємодії з таблицею цілей в базі даних.
    private SqliteGoalDao goalDao;
    // Тестовий об'єкт цілі, що використовується в більшості тестів.
    private Goal testGoal;
    // Поточна дата. Може бути використана для створення тестових даних з актуальними датами.
    // У поточній реалізації тестів ця змінна не використовується.
    private final LocalDate TODAY = LocalDate.now();

    /**
     * Метод налаштування, що виконується перед кожним тестовим методом.
     * Ініціалізує {@link SqliteGoalDao} та очищає таблицю "goals".
     * Створює стандартний тестовий об'єкт {@link Goal}.
     * @throws Exception якщо виникає помилка під час налаштування.
     */
    @BeforeEach
    void setUpGoalDao() throws Exception {
        goalDao = new SqliteGoalDao();
        clearTable("goals"); // Очищення таблиці "goals" перед кожним тестом для забезпечення їх незалежності.

        // Створюємо тестову ціль для використання в тестах
        testGoal = new Goal(
                "Прочитати 10 книг", // Опис цілі
                GoalType.MONTHLY,     // Тип цілі: місячна
                10,                   // Цільове значення (кількість книг)
                2024,                 // Рік, на який встановлена ціль
                7                     // Місяць, на який встановлена ціль (Липень)
        );
    }

    /**
     * Тест для перевірки додавання нової цілі до бази даних.
     * Перевіряє, що ціль успішно зберігається, їй присвоюється ID,
     * та всі поля зберігаються коректно.
     * @throws Exception якщо виникає помилка під час взаємодії з БД.
     */
    @Test
    void testAddGoal() throws Exception {
        // When: Додаємо тестову ціль до бази даних
        goalDao.addGoal(testGoal);

        // Then: Перевіряємо, що ціль була успішно додана (в таблиці тепер один запис)
        assertEquals(1, getTableRowCount("goals"), "Кількість цілей в БД має бути 1 після додавання.");

        // And: Отримуємо збережену ціль з бази даних та перевіряємо її поля
        List<Goal> goals = goalDao.getAllGoals();
        assertEquals(1, goals.size(), "Список цілей з БД має містити 1 ціль.");

        Goal savedGoal = goals.get(0);
        assertNotEquals(0, savedGoal.getId(), "ID збереженої цілі не має бути 0 (має бути автозгенерований базою даних).");
        assertEquals(testGoal.getDescription(), savedGoal.getDescription(), "Опис збереженої цілі не відповідає очікуваному.");
        assertEquals(testGoal.getType(), savedGoal.getType(), "Тип збереженої цілі не відповідає очікуваному.");
        assertEquals(testGoal.getTargetValue(), savedGoal.getTargetValue(), "Цільове значення збереженої цілі не відповідає очікуваному.");
        assertEquals(testGoal.getYear(), savedGoal.getYear(), "Рік збереженої цілі не відповідає очікуваному.");
        assertEquals(testGoal.getMonth(), savedGoal.getMonth(), "Місяць збереженої цілі не відповідає очікуваному.");
        assertNotNull(savedGoal.getDateAdded(), "Дата додавання збереженої цілі не має бути null.");
    }

    /**
     * Тест для перевірки отримання цілі за її унікальним ідентифікатором (ID).
     * Перевіряє, що ціль знаходиться, якщо вона існує, та не знаходиться, якщо ID невірний.
     * @throws Exception якщо виникає помилка під час взаємодії з БД.
     */
    @Test
    void testGetGoalById() throws Exception {
        // Given: Додаємо тестову ціль до бази даних
        goalDao.addGoal(testGoal);
        List<Goal> goals = goalDao.getAllGoals(); // Отримуємо список всіх цілей (має бути одна)
        Goal addedGoal = goals.get(0);            // Беремо додану ціль для отримання її ID

        // When: Намагаємося отримати ціль за її ID
        Optional<Goal> foundGoalOptional = goalDao.getGoalById(addedGoal.getId());

        // Then: Перевіряємо, що ціль була знайдена та її поля відповідають доданій цілі
        assertTrue(foundGoalOptional.isPresent(), "Ціль з ID " + addedGoal.getId() + " має бути знайдена.");
        Goal foundGoal = foundGoalOptional.get();
        assertEquals(addedGoal.getId(), foundGoal.getId(), "ID знайденої цілі не відповідає очікуваному.");
        assertEquals(addedGoal.getDescription(), foundGoal.getDescription(), "Опис знайденої цілі не відповідає очікуваному.");

        // When: Намагаємося отримати ціль за неіснуючим ID
        long nonExistentId = 999L; // Припускаємо, що такого ID немає в базі
        Optional<Goal> notFoundOptional = goalDao.getGoalById(nonExistentId);

        // Then: Перевіряємо, що результат пошуку порожній (Optional.empty())
        assertTrue(notFoundOptional.isEmpty(), "Пошук цілі за неіснуючим ID (" + nonExistentId + ") має повернути порожній Optional.");
    }

    /**
     * Тест для перевірки оновлення існуючої цілі в базі даних.
     * Перевіряє, що зміни в полях цілі коректно зберігаються.
     * @throws Exception якщо виникає помилка під час взаємодії з БД.
     */
    @Test
    void testUpdateGoal() throws Exception {
        // Given: Додаємо тестову ціль до бази даних
        goalDao.addGoal(testGoal);
        List<Goal> goals = goalDao.getAllGoals();
        Goal addedGoal = goals.get(0); // Отримуємо додану ціль для подальшого оновлення

        // When: Змінюємо дані цілі
        String updatedDescription = "Оновлена ціль на прочитання 50 книг у 2025 році";
        addedGoal.setDescription(updatedDescription); // Новий опис
        addedGoal.setType(GoalType.YEARLY);           // Новий тип - річна
        addedGoal.setTargetValue(50);                 // Нове цільове значення
        addedGoal.setYear(2025);                      // Новий рік
        addedGoal.setMonth(null);    // Для річної цілі місяць не вказується (має бути null)
        goalDao.updateGoal(addedGoal); // Оновлюємо ціль в базі даних

        // Then: Отримуємо оновлену ціль з бази даних та перевіряємо, що зміни застосувалися
        Optional<Goal> updatedGoalOptional = goalDao.getGoalById(addedGoal.getId());
        assertTrue(updatedGoalOptional.isPresent(), "Оновлена ціль має бути знайдена в БД після оновлення.");

        Goal updatedGoal = updatedGoalOptional.get();
        assertEquals(updatedDescription, updatedGoal.getDescription(), "Опис цілі не оновився належним чином.");
        assertEquals(GoalType.YEARLY, updatedGoal.getType(), "Тип цілі не оновився належним чином.");
        assertEquals(50, updatedGoal.getTargetValue(), "Цільове значення не оновилося належним чином.");
        assertEquals(2025, updatedGoal.getYear(), "Рік цілі не оновився належним чином.");
        assertNull(updatedGoal.getMonth(), "Місяць для річної цілі має бути null після оновлення.");
    }

    /**
     * Тест для перевірки видалення цілі з бази даних за її ID.
     * Перевіряє, що ціль більше не доступна після видалення, і кількість записів у таблиці зменшується.
     * @throws Exception якщо виникає помилка під час взаємодії з БД.
     */
    @Test
    void testDeleteGoal() throws Exception {
        // Given: Додаємо тестову ціль до бази даних
        goalDao.addGoal(testGoal);
        List<Goal> goals = goalDao.getAllGoals();
        Goal addedGoal = goals.get(0); // Отримуємо додану ціль для подальшого видалення

        // When: Видаляємо ціль за її ID
        goalDao.deleteGoal(addedGoal.getId());

        // Then: Перевіряємо, що ціль була видалена з бази даних
        Optional<Goal> deletedGoalOptional = goalDao.getGoalById(addedGoal.getId());
        assertTrue(deletedGoalOptional.isEmpty(), "Ціль не має бути знайдена в БД після операції видалення.");
        assertEquals(0, getTableRowCount("goals"), "Таблиця 'goals' має бути порожньою після видалення єдиної цілі.");
    }

    /**
     * Тест для перевірки отримання списку всіх цілей з бази даних.
     * Перевіряє коректність кількості отриманих цілей та наявність різних типів цілей.
     * Також перевіряє повернення порожнього списку, якщо таблиця порожня.
     * @throws Exception якщо виникає помилка під час взаємодії з БД.
     */
    @Test
    void testGetAllGoals() throws Exception {
        // Given: Додаємо декілька різних цілей до бази даних для перевірки отримання всіх записів
        goalDao.addGoal(testGoal); // Перша ціль (місячна, з testGoal)

        // Друга ціль (річна)
        Goal yearlyGoal = new Goal(
                "Річна ціль на 50 книг", // Опис
                GoalType.YEARLY,          // Тип
                50,                       // Цільове значення
                2024,                     // Рік
                null                      // Місяць (null для річної цілі)
        );
        goalDao.addGoal(yearlyGoal);

        // Третя ціль (загальна)
        Goal totalGoal = new Goal(
                "Загальна ціль на 100 книг", // Опис
                GoalType.TOTAL,              // Тип
                100,                         // Цільове значення
                null,                        // Рік (null для загальної цілі, не прив'язана до конкретного року)
                null                         // Місяць (null для загальної цілі)
        );
        goalDao.addGoal(totalGoal);

        // When: Отримуємо всі цілі з бази даних
        List<Goal> allGoals = goalDao.getAllGoals();

        // Then: Перевіряємо, що список містить правильну кількість цілей та всі типи цілей присутні
        assertEquals(3, allGoals.size(), "Неправильна кількість цілей отримана з БД (очікується 3).");
        assertTrue(allGoals.stream().anyMatch(g -> g.getType() == GoalType.MONTHLY), "Місячна ціль має бути у списку всіх цілей.");
        assertTrue(allGoals.stream().anyMatch(g -> g.getType() == GoalType.YEARLY), "Річна ціль має бути у списку всіх цілей.");
        assertTrue(allGoals.stream().anyMatch(g -> g.getType() == GoalType.TOTAL), "Загальна ціль має бути у списку всіх цілей.");

        // When: Очищаємо таблицю "goals"
        clearTable("goals");

        // Then: Перевіряємо, що після очищення таблиці метод getAllGoals повертає порожній список
        assertTrue(goalDao.getAllGoals().isEmpty(), "Список цілей має бути порожнім після очищення таблиці 'goals'.");
    }

    /**
     * Тест для перевірки генерації {@link DataAccessException} у випадках некоректних операцій з даними.
     * Зокрема, при спробі додати ціль з невалідними даними (наприклад, null для NOT NULL полів),
     * оновити неіснуючу ціль, або додати ціль без обов'язкового типу.
     */
    @Test
    void testDataAccessException() {
        // Given: Створюємо об'єкт цілі з null значеннями для полів, які визначені як NOT NULL у схемі бази даних
        // (наприклад, description, type, target_value).
        Goal invalidGoal = new Goal(); // Поля за замовчуванням будуть null або 0, що може порушити обмеження БД.

        // When & Then: Перевіряємо, що спроба додати таку невалідну ціль викликає DataAccessException.
        // Це очікується через обмеження NOT NULL у схемі бази даних або валідацію в DAO.
        assertThrows(DataAccessException.class, () -> goalDao.addGoal(invalidGoal),
                "Має бути викинуто DataAccessException при спробі додати ціль з обов'язковими полями null/невалідними значеннями.");

        // Given: Створюємо ціль з валідними даними, але встановлюємо ID, якого гарантовано немає в базі даних,
        // для тестування операції оновлення.
        Goal nonExistentGoal = new Goal("Неіснуюча для оновлення", GoalType.MONTHLY, 5, 2024, 1);
        long nonExistentIdForUpdate = 999L;
        nonExistentGoal.setId(nonExistentIdForUpdate); // Встановлюємо ID, якого, як очікується, немає в БД.

        // When & Then: Перевіряємо, що спроба оновити ціль з неіснуючим ID викликає DataAccessException.
        // Це очікується, якщо метод update перевіряє кількість оновлених рядків і кидає виняток, якщо 0.
        assertThrows(DataAccessException.class, () -> goalDao.updateGoal(nonExistentGoal),
                "Має бути викинуто DataAccessException при спробі оновити ціль, яка не існує в БД (ID=" + nonExistentIdForUpdate + ").");

        // Given: Створюємо ціль, у якої поле 'type' є null. Це поле є обов'язковим згідно з логікою додатка.
        Goal goalWithoutType = new Goal();
        goalWithoutType.setDescription("Тест без типу");
        goalWithoutType.setTargetValue(5);
        // Поле type залишається null, якщо конструктор за замовчуванням або сеттери це дозволяють.

        // When & Then: Перевіряємо, що спроба додати ціль без обов'язкового типу викликає DataAccessException
        // з відповідним повідомленням, що вказує на причину помилки.
        DataAccessException exception = assertThrows(DataAccessException.class,
                () -> goalDao.addGoal(goalWithoutType),
                "Має бути викинуто DataAccessException при спробі додати ціль з типом null.");
        assertEquals("Тип цілі не може бути null", exception.getMessage(), "Повідомлення про помилку не відповідає очікуваному, коли тип цілі є null.");
    }
}