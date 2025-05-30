package com.student.bookdiary.persistence;

import org.junit.jupiter.api.BeforeEach;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;

/**
 * Базовий клас для тестів DAO (Data Access Object).
 * Цей клас надає спільну інфраструктуру та допоміжні методи
 * для тестів, які взаємодіють з базою даних.
 * Він налаштовує базу даних SQLite в пам'яті для кожного тестового запуску
 * та надає методи для очищення таблиць та отримання кількості записів.
 */
public abstract class BaseDaoTest {

    /**
     * Менеджер бази даних, використовується для отримання з'єднання.
     * Наразі не використовується безпосередньо в цьому класі,
     * але може бути корисним для дочірніх класів або майбутніх розширень.
     */
    protected DatabaseManager databaseManager; // Хоча databaseManager не використовується, його наявність може бути виправдана для майбутніх потреб або як приклад для дочірніх класів.

    /**
     * Метод, що виконується перед кожним тестовим методом.
     * Налаштовує системну властивість {@code db.url} для використання бази даних SQLite в пам'яті.
     * Отримує з'єднання з базою даних через {@link DatabaseManager#getConnection()},
     * що також ініціалізує структуру таблиць, якщо вона ще не створена.
     *
     * @throws SQLException якщо виникає помилка під час налаштування бази даних
     *                      або отримання з'єднання.
     */
    @BeforeEach
    void setUp() throws SQLException {
        // Встановлюємо URL бази даних SQLite в пам'яті для тестів.
        // Це гарантує, що кожен тест працює з чистою базою даних.
        System.setProperty("db.url", "jdbc:sqlite::memory:");
        // Отримуємо з'єднання. DatabaseManager повинен бути спроектований так,
        // щоб при першому отриманні з'єднання (або якщо база даних в пам'яті порожня)
        // створювалися необхідні таблиці.
        DatabaseManager.getConnection();
    }

    /**
     * Отримує з'єднання з тестовою базою даних.
     * Використовує {@link DatabaseManager#getConnection()} для отримання з'єднання.
     *
     * @return об'єкт {@link Connection}, що представляє з'єднання з базою даних.
     * @throws SQLException якщо виникає помилка SQL під час отримання з'єднання.
     */
    protected Connection getConnection() throws SQLException {
        return DatabaseManager.getConnection();
    }

    /**
     * Очищає всі записи з вказаної таблиці в базі даних.
     * Також намагається скинути лічильник автоінкремента для цієї таблиці
     * (специфічно для SQLite), щоб забезпечити консистентність ID в тестах.
     *
     * @param tableName назва таблиці, яку потрібно очистити.
     * @throws SQLException якщо виникає помилка SQL під час виконання операції очищення.
     */
    protected void clearTable(String tableName) throws SQLException {
        Connection connection = getConnection(); // Отримуємо поточне з'єднання.
        try (Statement stmt = connection.createStatement()) {
            // Видаляємо всі записи з таблиці.
            stmt.executeUpdate("DELETE FROM " + tableName);
            // Намагаємося скинути лічильник автоінкремента для таблиці в SQLite.
            // Це корисно для тестів, щоб ID починалися з 1 після очищення.
            // Помилка ігнорується, якщо таблиця SQLITE_SEQUENCE не існує
            // або запит на скидання послідовності не вдався (наприклад, для таблиць без автоінкремента).
            try {
                stmt.executeUpdate("DELETE FROM SQLITE_SEQUENCE WHERE name='" + tableName + "'");
            } catch (SQLException e) {
                // Ця помилка не є критичною для більшості тестів, тому її можна проігнорувати
                // або залогувати на рівні TRACE, якщо логування налаштоване.
                // System.err.println("Could not reset sequence for table " + tableName + ": " + e.getMessage());
            }
        }
        // Важливо: з'єднання НЕ закривається тут.
        // Для баз даних SQLite в пам'яті, DatabaseManager.getConnection() зазвичай повертає
        // одне й те саме статичне з'єднання протягом життя JVM (або до явного закриття).
        // Закриття цього з'єднання призведе до втрати бази даних в пам'яті.
        // Якщо б це була файлова база даних і getConnection() створював нове з'єднання кожного разу,
        // тоді його потрібно було б закривати.
    }

    /**
     * Отримує кількість записів у вказаній таблиці.
     *
     * @param tableName назва таблиці, для якої потрібно підрахувати записи.
     * @return загальна кількість записів у таблиці.
     * @throws SQLException якщо виникає помилка SQL під час виконання запиту.
     */
    protected int getTableRowCount(String tableName) throws SQLException {
        Connection connection = getConnection(); // Отримуємо поточне з'єднання.
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + tableName)) {
            // Перевіряємо, чи є результат, і повертаємо кількість.
            // Якщо результату немає (що малоймовірно для COUNT(*)), повертаємо 0.
            return rs.next() ? rs.getInt(1) : 0;
        }
        // З'єднання НЕ закривається тут з тих самих причин, що й у clearTable().
    }
}