package com.student.bookdiary.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Тестовий клас для {@link DatabaseManager}.
 * Цей клас перевіряє основні функціональні можливості {@code DatabaseManager},
 * такі як отримання з'єднання з базою даних та ініціалізація структури таблиць.
 * Тести використовують базу даних SQLite в пам'яті для забезпечення ізольованості
 * та швидкості виконання.
 */
class DatabaseManagerTest {

    /**
     * Метод, що виконується перед кожним тестовим методом.
     * Встановлює системну властивість {@code db.url} для використання бази даних SQLite в пам'яті.
     * Також очищає таблиці {@code books} та {@code goals} перед кожним тестом,
     * щоб забезпечити чисте середовище для тестування.
     * Якщо виникає помилка під час очищення таблиць, тест позначається як невдалий.
     */
    @BeforeEach
    void setUp() {
        // Встановлюємо URL для тестової бази даних SQLite в пам'яті.
        System.setProperty("db.url", "jdbc:sqlite::memory:");

        // Очищаємо таблиці перед кожним тестом.
        // Це необхідно, оскільки база даних в пам'яті зберігає свій стан між викликами getConnection()
        // в межах одного тестового класу (або навіть запуску JVM, залежно від реалізації DatabaseManager).
        try {
            Connection connection = DatabaseManager.getConnection();
            // Використовуємо try-with-resources для автоматичного закриття Statement.
            try (Statement stmtBooks = connection.createStatement();
                 Statement stmtGoals = connection.createStatement()) {
                // Очищення таблиці books
                stmtBooks.executeUpdate("DELETE FROM books");
                // Очищення таблиці goals
                stmtGoals.executeUpdate("DELETE FROM goals");
                // Додатково можна скинути лічильники автоінкремента, якщо це важливо для тестів:
                // stmtBooks.executeUpdate("DELETE FROM SQLITE_SEQUENCE WHERE name='books'");
                // stmtGoals.executeUpdate("DELETE FROM SQLITE_SEQUENCE WHERE name='goals'");
            }
        } catch (SQLException e) {
            // Якщо виникає помилка під час очищення, тест повинен завершитися невдачею,
            // оскільки це може вплинути на результати інших тестів.
            fail("Помилка при очищенні таблиць перед тестом: " + e.getMessage());
        }
    }

    /**
     * Тестує метод {@link DatabaseManager#getConnection()}.
     * Перевіряє, чи метод успішно повертає об'єкт {@link Connection},
     * чи з'єднання не закрите, та чи використовується очікувана база даних SQLite в пам'яті.
     * Якщо виникає помилка під час отримання з'єднання, тест позначається як невдалий.
     */
    @Test
    void testGetConnection() {
        try {
            Connection connection = DatabaseManager.getConnection();
            assertNotNull(connection, "З'єднання не повинно бути null.");
            assertFalse(connection.isClosed(), "З'єднання не повинно бути закритим після отримання.");
            // Перевіряємо, що системна властивість db.url вказує на in-memory базу даних.
            // Це підтверджує, що тести налаштовані на використання правильної БД.
            assertEquals("jdbc:sqlite::memory:", System.getProperty("db.url"),
                    "Тести повинні використовувати базу даних SQLite в пам'яті.");
        } catch (SQLException e) {
            fail("Не вдалося встановити з'єднання з БД: " + e.getMessage());
        }
    }

    /**
     * Тестує процес ініціалізації бази даних, який виконується при першому отриманні з'єднання.
     * Перевіряє наявність таблиць {@code books} та {@code goals} та їх основних колонок
     * після того, як {@link DatabaseManager#getConnection()} був викликаний.
     * Якщо виникає помилка під час перевірки, тест позначається як невдалий.
     */
    @Test
    void testInitializeDatabase() {
        try {
            Connection connection = DatabaseManager.getConnection(); // Ініціалізація БД відбувається тут (якщо ще не відбулася)

            // Перевіряємо структуру таблиці books
            try (ResultSet rs = connection.createStatement()
                    .executeQuery("PRAGMA table_info(books)")) { // PRAGMA table_info повертає інформацію про колонки таблиці
                assertTrue(rs.next(), "Таблиця 'books' не існує або порожня після ініціалізації.");

                // Лічильники для перевірки наявності ключових колонок
                boolean hasId = false;
                boolean hasTitle = false;
                boolean hasAuthor = false;

                // Проходимо по всіх колонках таблиці
                do {
                    String columnName = rs.getString("name");
                    switch (columnName) {
                        case "id":
                            hasId = true;
                            break;
                        case "title":
                            hasTitle = true;
                            break;
                        case "author":
                            hasAuthor = true;
                            break;
                    }
                } while (rs.next());

                assertTrue(hasId, "Відсутня колонка 'id' в таблиці 'books'.");
                assertTrue(hasTitle, "Відсутня колонка 'title' в таблиці 'books'.");
                assertTrue(hasAuthor, "Відсутня колонка 'author' в таблиці 'books'.");
            }

            // Перевіряємо структуру таблиці goals
            try (ResultSet rs = connection.createStatement()
                    .executeQuery("PRAGMA table_info(goals)")) {
                assertTrue(rs.next(), "Таблиця 'goals' не існує або порожня після ініціалізації.");

                boolean hasId = false;
                boolean hasDescription = false;
                boolean hasType = false;

                do {
                    String columnName = rs.getString("name");
                    switch (columnName) {
                        case "id":
                            hasId = true;
                            break;
                        case "description":
                            hasDescription = true;
                            break;
                        case "type":
                            hasType = true;
                            break;
                    }
                } while (rs.next());

                assertTrue(hasId, "Відсутня колонка 'id' в таблиці 'goals'.");
                assertTrue(hasDescription, "Відсутня колонка 'description' в таблиці 'goals'.");
                assertTrue(hasType, "Відсутня колонка 'type' в таблиці 'goals'.");
            }
        } catch (SQLException e) {
            fail("Помилка при перевірці ініціалізації БД: " + e.getMessage());
        }
    }

    /**
     * Тестує існування таблиць та можливість виконання операцій вставки та вибірки даних
     * після ініціалізації бази даних.
     * Вставляє тестові записи в таблиці {@code books} та {@code goals}, а потім перевіряє,
     * чи ці записи були успішно додані та чи їх можна прочитати.
     * Якщо виникає помилка SQL під час цих операцій, тест позначається як невдалий.
     */
    @Test
    void testTableExistenceAfterInitialization() {
        try {
            Connection connection = DatabaseManager.getConnection();

            // Перевіряємо можливість вставки даних в таблицю books
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate(
                        "INSERT INTO books (title, author, genre, status, dateAdded) " +
                                "VALUES ('Test Book', 'Test Author', 'Test Genre', 'READ', date('now'))"
                );
                // Перевіряємо можливість вставки даних в таблицю goals
                stmt.executeUpdate(
                        "INSERT INTO goals (description, type, targetValue, dateAdded) " +
                                "VALUES ('Test Goal', 'MONTHLY', 5, date('now'))"
                );
            }

            // Перевіряємо можливість вибірки даних з таблиці books
            try (Statement stmtSelectBooks = connection.createStatement();
                 ResultSet rsBooks = stmtSelectBooks.executeQuery("SELECT * FROM books WHERE title = 'Test Book'")) {
                assertTrue(rsBooks.next(), "Дані не були вставлені або не можуть бути вибрані з таблиці 'books'.");
                assertEquals("Test Book", rsBooks.getString("title"), "Назва книги не відповідає вставленій.");
                assertNotNull(rsBooks.getString("dateAdded"), "Дата додавання для книги не повинна бути null.");
            }

            // Перевіряємо можливість вибірки даних з таблиці goals
            try (Statement stmtSelectGoals = connection.createStatement();
                 ResultSet rsGoals = stmtSelectGoals.executeQuery("SELECT * FROM goals WHERE description = 'Test Goal'")) {
                assertTrue(rsGoals.next(), "Дані не були вставлені або не можуть бути вибрані з таблиці 'goals'.");
                assertEquals("Test Goal", rsGoals.getString("description"), "Опис цілі не відповідає вставленому.");
                assertEquals(5, rsGoals.getInt("targetValue"), "Значення 'targetValue' для цілі не відповідає вставленому.");
                assertNotNull(rsGoals.getString("dateAdded"), "Дата додавання для цілі не повинна бути null.");
            }
        } catch (SQLException e) {
            fail("Помилка під час тестування існування таблиць та операцій вставки/вибірки даних: " + e.getMessage());
        }
    }
}