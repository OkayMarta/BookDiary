package com.student.bookdiary.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Клас {@code DatabaseManager} відповідає за управління з'єднанням
 * з базою даних SQLite та її початкову ініціалізацію,
 * включаючи створення необхідних таблиць.
 */
public class DatabaseManager {

    private static final Logger log = LoggerFactory.getLogger(DatabaseManager.class);

    private static final String DEFAULT_DB_URL = "jdbc:sqlite:book_diary.db";
    private static final String IN_MEMORY_DB_URL = "jdbc:sqlite::memory:";
    
    private static volatile Connection dbConnection;
    private static final Object lock = new Object();

    static {
        // Це гарантує, що для тестів завжди використовується in-memory база даних
        // і з'єднання ініціалізується тільки один раз.
        String dbUrl = getDbUrl();
        if (IN_MEMORY_DB_URL.equals(dbUrl)) {
            try {
                log.debug("Ініціалізація статичного in-memory з'єднання...");
                dbConnection = DriverManager.getConnection(IN_MEMORY_DB_URL);
                initializeDatabase(dbConnection);
                log.info("Статичне in-memory з'єднання успішно ініціалізовано.");
            } catch (SQLException e) {
                log.error("Помилка під час статичної ініціалізації in-memory з'єднання: {}", e.getMessage(), e);
                throw new RuntimeException("Не вдалося ініціалізувати статичне in-memory з'єднання", e);
            }
        }
    }

    /**
     * Повертає URL для підключення до бази даних.
     * Спочатку перевіряє наявність системної властивості 'db.url',
     * якщо не знайдено - використовує значення за замовчуванням.
     *
     * @return URL для підключення до бази даних
     */
    private static String getDbUrl() {
        return System.getProperty("db.url", DEFAULT_DB_URL);
    }

    /**
     * SQL-запит для створення таблиці {@code books}, якщо вона ще не існує.
     * Таблиця зберігає інформацію про книги в читацькому щоденнику.
     * <p>
     * Структура таблиці:
     * <ul>
     *     <li>{@code id} - INTEGER, первинний ключ з автоінкрементом, унікальний ідентифікатор книги.</li>
     *     <li>{@code title} - TEXT, назва книги, не може бути NULL.</li>
     *     <li>{@code author} - TEXT, автор книги.</li>
     *     <li>{@code genre} - TEXT, жанр книги.</li>
     *     <li>{@code status} - TEXT, статус читання (наприклад, "READ", "WANT_TO_READ"), не може бути NULL.</li>
     *     <li>{@code dateAdded} - TEXT, дата додавання книги у форматі ISO ("YYYY-MM-DD"), не може бути NULL.</li>
     *     <li>{@code dateRead} - TEXT, дата прочитання книги у форматі ISO ("YYYY-MM-DD"), може бути NULL.</li>
     *     <li>{@code rating} - INTEGER, оцінка книги (наприклад, 1-5).</li>
     *     <li>{@code comment} - TEXT, коментар користувача до книги.</li>
     *     <li>{@code coverImagePath} - TEXT, шлях до файлу обкладинки книги.</li>
     *     <li>{@code favorite} - INTEGER, прапорець "улюблене" (0 - false, 1 - true), не може бути NULL, за замовчуванням 0.</li>
     * </ul>
     */
    private static final String CREATE_TABLE_BOOKS_SQL = """
        CREATE TABLE IF NOT EXISTS books (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            title TEXT NOT NULL,
            author TEXT,
            genre TEXT,
            status TEXT NOT NULL,
            dateAdded TEXT NOT NULL,
            dateRead TEXT,
            rating INTEGER,
            comment TEXT,
            coverImagePath TEXT,
            favorite INTEGER NOT NULL DEFAULT 0
        );
        """;

    /**
     * SQL-запит для створення таблиці {@code goals}, якщо вона ще не існує.
     * Таблиця зберігає інформацію про цілі читання користувача.
     * <p>
     * Структура таблиці:
     * <ul>
     *     <li>{@code id} - INTEGER, первинний ключ з автоінкрементом, унікальний ідентифікатор цілі.</li>
     *     <li>{@code description} - TEXT, опис цілі, наданий користувачем.</li>
     *     <li>{@code type} - TEXT, тип цілі (наприклад, "MONTHLY", "YEARLY", "TOTAL"), не може бути NULL.</li>
     *     <li>{@code targetValue} - INTEGER, цільова кількість книг для досягнення, не може бути NULL.</li>
     *     <li>{@code year} - INTEGER, рік, до якого відноситься ціль (може бути NULL для типу TOTAL).</li>
     *     <li>{@code month} - INTEGER, місяць (1-12), до якого відноситься ціль (може бути NULL для типів YEARLY, TOTAL).</li>
     *     <li>{@code dateAdded} - TEXT, дата створення цілі у форматі ISO ("YYYY-MM-DD"), не може бути NULL.</li>
     * </ul>
     */
    private static final String CREATE_TABLE_GOALS_SQL = """
        CREATE TABLE IF NOT EXISTS goals (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            description TEXT,
            type TEXT NOT NULL,
            targetValue INTEGER NOT NULL,
            year INTEGER,
            month INTEGER,
            dateAdded TEXT NOT NULL
        );
        """;

    /**
     * Перевіряє, чи є з'єднання активним та валідним.
     *
     * @param conn З'єднання для перевірки
     * @return true, якщо з'єднання валідне
     */
    private static boolean isConnectionValid(Connection conn) {
        if (conn == null) {
            return false;
        }
        try {
            // Використовуємо conn.isValid(timeout) для перевірки з'єднання, 
            // це більш надійний спосіб, ніж виконання запиту.
            // Встановлюємо невеликий таймаут (наприклад, 1 секунда).
            return conn.isValid(1);
        } catch (SQLException e) {
            log.warn("Помилка під час перевірки валідності з'єднання: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Встановлює та повертає з'єднання з базою даних SQLite.
     * Для in-memory бази даних повертає єдине статично ініціалізоване з'єднання.
     * Для файлової бази даних створює нове з'єднання при кожному виклику.
     *
     * @return Об'єкт {@link Connection}, що представляє з'єднання з БД.
     * @throws DataAccessException якщо сталася помилка під час спроби підключення до бази даних.
     */
    public static Connection getConnection() {
        String dbUrl = getDbUrl();
        
        if (IN_MEMORY_DB_URL.equals(dbUrl)) {
            // Для in-memory завжди повертаємо статичне з'єднання
            // яке було ініціалізовано в статичному блоці.
            // Додаткова перевірка валідності тут не потрібна, 
            // оскільки воно повинно бути вже валідним.
            if (dbConnection == null) {
                // Цей випадок не повинен відбуватися, якщо статичний блок відпрацював коректно,
                // але додамо перевірку про всяк випадок.
                log.error("Статичне in-memory з'єднання не було ініціалізовано!");
                throw new DataAccessException("Статичне in-memory з'єднання не було ініціалізовано!");
            }
            return dbConnection;
        } else {
            // Для файлової бази даних створюємо нове з'єднання
            synchronized (lock) { // Синхронізація для безпечної ініціалізації файлової БД
                try {
                    log.debug("Спроба підключення до файлової бази даних SQLite за адресою: {}", dbUrl);
                    Connection connection = DriverManager.getConnection(dbUrl);
                    log.debug("З'єднання з файловою базою даних SQLite успішно встановлено.");
                    // Ініціалізуємо таблиці, якщо це перше з'єднання з файловою БД
                    // або якщо це потрібно (наприклад, після видалення файлу БД).
                    // В даному випадку, для простоти, ініціалізуємо завжди.
                    // В реальному додатку тут може бути більш складна логіка.
                    initializeDatabase(connection);
                    return connection;
                } catch (SQLException e) {
                    log.error("Помилка підключення до файлової бази даних SQLite за адресою {}. Деталі: {}", dbUrl, e.getMessage(), e);
                    throw new DataAccessException("Не вдалося підключитися до файлової бази даних: " + e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Ініціалізує структуру бази даних (створює таблиці).
     * Цей метод викликається при створенні нового з'єднання.
     *
     * @param conn З'єднання з базою даних для ініціалізації
     * @throws SQLException якщо виникає помилка під час ініціалізації бази даних
     */
    private static void initializeDatabase(Connection conn) throws SQLException {
        log.info("Початок ініціалізації таблиць в базі даних (з'єднання: {}) ...", conn);
        try (Statement stmt = conn.createStatement()) {
            log.debug("Спроба виконати SQL-запит для створення таблиці 'books':\n{}", CREATE_TABLE_BOOKS_SQL);
            stmt.execute(CREATE_TABLE_BOOKS_SQL);
            log.info("Таблиця 'books' успішно створена або вже існувала.");

            log.debug("Спроба виконати SQL-запит для створення таблиці 'goals':\n{}", CREATE_TABLE_GOALS_SQL);
            stmt.execute(CREATE_TABLE_GOALS_SQL);
            log.info("Таблиця 'goals' успішно створена або вже існувала.");

            log.info("Ініціалізація таблиць в базі даних успішно завершена.");
        } catch (SQLException e) {
            log.error("Помилка під час ініціалізації таблиць в базі даних: {}. З'єднання: {}", e.getMessage(), conn, e);
            throw e; // Перекидаємо виняток, щоб його можна було обробити вище
        }
    }

    /**
     * Публічний метод для ініціалізації бази даних.
     * В основному використовується для файлових баз даних, щоб гарантувати створення таблиць.
     * Для in-memory баз даних ініціалізація відбувається автоматично в статичному блоці.
     */
    public static void initializeDatabase() {
        String dbUrl = getDbUrl();
        if (!IN_MEMORY_DB_URL.equals(dbUrl)) {
            log.info("Примусова ініціалізація файлової бази даних...");
            try (Connection conn = getConnection()) {
                // getConnection() для файлової БД вже викличе initializeDatabase(conn)
                log.info("Файлова база даних успішно ініціалізована (або вже була ініціалізована).");
            } catch (SQLException e) {
                log.error("Помилка під час примусової ініціалізації файлової бази даних: {}", e.getMessage(), e);
                throw new DataAccessException("Не вдалося примусово ініціалізувати файлову базу даних: " + e.getMessage(), e);
            }
        } else {
            log.debug("Для in-memory бази даних примусова ініціалізація через initializeDatabase() не потрібна.");
        }
    }

    /**
     * Допоміжний статичний метод для безпечного закриття ресурсів JDBC: {@link Statement} та {@link java.sql.ResultSet}.
     * З'єднання {@link Connection} не закривається цим методом, особливо якщо це in-memory з'єднання, 
     * яке повинно залишатися відкритим протягом життя програми (або тестів).
     * Закриттям файлових з'єднань повинен займатися код, що їх використовує (наприклад, DAO).
     *
     * @param stmt Об'єкт Statement, який потрібно закрити (може бути null).
     * @param rs Об'єкт ResultSet, який потрібно закрити (може бути null).
     */
    public static void closeResources(Statement stmt, java.sql.ResultSet rs) {
        try {
            if (rs != null && !rs.isClosed()) {
                rs.close();
                log.trace("ResultSet успішно закрито.");
            }
        } catch (SQLException e) {
            log.warn("Помилка під час закриття ResultSet: {}", e.getMessage(), e);
        }
        try {
            if (stmt != null && !stmt.isClosed()) {
                stmt.close();
                log.trace("Statement успішно закрито.");
            }
        } catch (SQLException e) {
            log.warn("Помилка під час закриття Statement: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Закриває з'єднання з базою даних, якщо воно не є in-memory.
     * Цей метод слід викликати при завершенні роботи програми для файлових БД.
     * Для in-memory з'єднання нічого не робить, оскільки воно закривається автоматично при завершенні JVM.
     */
    public static void closeConnection() {
        String dbUrl = getDbUrl();
        if (!IN_MEMORY_DB_URL.equals(dbUrl)) {
            synchronized (lock) {
                if (dbConnection != null) {
                    try {
                        if (!dbConnection.isClosed()) {
                            log.info("Закриття файлового з'єднання з БД...");
                            dbConnection.close();
                            dbConnection = null; // Важливо скинути, щоб наступний getConnection() створив нове
                            log.info("Файлове з'єднання з БД успішно закрито.");
                        }
                    } catch (SQLException e) {
                        log.warn("Помилка під час закриття файлового з'єднання з БД: {}", e.getMessage(), e);
                    }
                }
            }
        }
    }

    /**
     * Перевіряє, чи поточне з'єднання є in-memory з'єднанням.
     *
     * @param conn Передане з'єднання (на даний момент не використовується, перевірка йде по dbUrl)
     * @return true, якщо використовується in-memory база даних.
     */
    public static boolean isInMemoryConnection(Connection conn) {
        // Параметр conn наразі не використовується, оскільки логіка залежить від dbUrl
        // Це зроблено для узгодженості з попередньою реалізацією та для потенційного майбутнього використання.
        return IN_MEMORY_DB_URL.equals(getDbUrl());
    }
}