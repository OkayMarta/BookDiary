package com.student.bookdiary.persistence;

import com.student.bookdiary.model.Book;
import com.student.bookdiary.model.ReadingStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator; // Added for sorting
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors; // Added for stream operations

/**
 * Клас {@code SqliteBookDao} є реалізацією інтерфейсу {@link BookDao}
 * для роботи з базою даних SQLite. Він відповідає за виконання операцій
 * CRUD (Create, Read, Update, Delete) та інших запитів до таблиці книг.
 */
public class SqliteBookDao implements BookDao {

    private static final Logger log = LoggerFactory.getLogger(SqliteBookDao.class);

    /**
     * Виконує операцію з базою даних, використовуючи надане з'єднання.
     * Автоматично закриває з'єднання, якщо воно не є in-memory з'єднанням.
     *
     * @param operation Операція для виконання
     * @param <T> Тип результату
     * @return Результат операції
     * @throws DataAccessException якщо виникла помилка при роботі з БД
     */
    private <T> T executeWithConnection(SqlOperation<T> operation) {
        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            return operation.execute(conn);
        } catch (SQLException e) {
            log.error("Помилка SQL під час виконання операції: {}", e.getMessage(), e);
            throw new DataAccessException("Помилка при роботі з базою даних: " + e.getMessage(), e);
        } finally {
            if (conn != null && !DatabaseManager.isInMemoryConnection(conn)) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    log.warn("Помилка під час закриття з'єднання: {}", e.getMessage(), e);
                }
            }
        }
    }

    @FunctionalInterface
    private interface SqlOperation<T> {
        T execute(Connection conn) throws SQLException;
    }

    /**
     * Додає нову книгу до бази даних.
     * Встановлює згенерований ID для об'єкта книги після успішного додавання.
     *
     * @param book Об'єкт {@link Book} для додавання.
     * @throws DataAccessException Якщо виникає помилка SQL під час виконання операції.
     */
    @Override
    public void addBook(Book book) {
        log.debug("Спроба додати нову книгу: '{}', автор: '{}'", book.getTitle(), book.getAuthor());
        String sql = "INSERT INTO books(title, author, genre, status, dateAdded, dateRead, rating, comment, coverImagePath, favorite) VALUES(?,?,?,?,?,?,?,?,?,?)";

        executeWithConnection(conn -> {
            try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                setBookParameters(pstmt, book);
                int affectedRows = pstmt.executeUpdate();

                if (affectedRows == 0) {
                    String errorMessage = String.format("Додавання книги '%s' не змінило жодного рядка в БД.", book.getTitle());
                    log.error(errorMessage);
                    throw new DataAccessException(errorMessage);
                }

                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        book.setId(generatedKeys.getLong(1));
                        log.info("Книгу '{}' успішно додано до БД з ID={}", book.getTitle(), book.getId());
                    } else {
                        String errorMessage = String.format("Не вдалося отримати згенерований ID для книги '%s' після додавання.", book.getTitle());
                        log.error(errorMessage);
                        throw new DataAccessException(errorMessage);
                    }
                }
            }
            return null;
        });
    }

    /**
     * Оновлює дані існуючої книги в базі даних.
     *
     * @param book Об'єкт {@link Book} з оновленими даними та існуючим ID.
     * @throws DataAccessException Якщо виникає помилка SQL під час виконання операції.
     */
    @Override
    public void updateBook(Book book) {
        log.debug("Спроба оновити книгу з ID={}", book.getId());
        String sql = "UPDATE books SET title = ?, author = ?, genre = ?, status = ?, dateAdded = ?, " +
                "dateRead = ?, rating = ?, comment = ?, coverImagePath = ?, favorite = ? WHERE id = ?";

        executeWithConnection(conn -> {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                setBookParameters(pstmt, book);
                pstmt.setLong(11, book.getId());

                int affectedRows = pstmt.executeUpdate();
                if (affectedRows > 0) {
                    log.info("Книгу з ID={} успішно оновлено в БД. Назва: '{}'", book.getId(), book.getTitle());
                } else {
                    log.warn("Оновлення книги з ID={} не змінило жодного рядка. Можливо, книга з таким ID не знайдена.", book.getId());
                }
            }
            return null;
        });
    }

    /**
     * Видаляє книгу з бази даних за її ID.
     *
     * @param bookId ID книги для видалення.
     * @throws DataAccessException Якщо виникає помилка SQL під час виконання операції.
     */
    @Override
    public void deleteBook(long bookId) {
        log.debug("Спроба видалити книгу з ID={}", bookId);
        String sql = "DELETE FROM books WHERE id = ?";

        executeWithConnection(conn -> {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setLong(1, bookId);
                int affectedRows = pstmt.executeUpdate();

                if (affectedRows > 0) {
                    log.info("Книгу з ID={} успішно видалено з БД.", bookId);
                } else {
                    log.warn("Видалення книги з ID={} не змінило жодного рядка. Можливо, книга з таким ID не знайдена.", bookId);
                }
            }
            return null;
        });
    }

    /**
     * Знаходить книгу в базі даних за її ID.
     *
     * @param bookId ID книги для пошуку.
     * @return {@link Optional} з об'єктом {@link Book}, якщо знайдено, або порожній Optional.
     * @throws DataAccessException Якщо виникає помилка SQL під час виконання операції.
     */
    @Override
    public Optional<Book> getBookById(long bookId) {
        log.debug("Спроба знайти книгу за ID={}", bookId);
        String sql = "SELECT * FROM books WHERE id = ?";
        final Holder<Optional<Book>> bookHolder = new Holder<>(Optional.empty());

        executeWithConnection(conn -> {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setLong(1, bookId);

                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        bookHolder.value = Optional.of(mapResultSetToBook(rs));
                        log.debug("Книгу з ID={} знайдено: '{}'", bookId, bookHolder.value.get().getTitle());
                    } else {
                        log.debug("Книгу з ID={} не знайдено.", bookId);
                    }
                }
            }
            return null;
        });
        return bookHolder.value;
    }

    /**
     * Повертає список всіх книг з бази даних, відсортованих за датою додавання у зворотному порядку.
     *
     * @return {@link List} об'єктів {@link Book}.
     * @throws DataAccessException Якщо виникає помилка SQL під час виконання операції.
     */
    @Override
    public List<Book> getAllBooks() {
        log.debug("Спроба отримати список всіх книг.");
        String sql = "SELECT * FROM books ORDER BY dateAdded DESC";
        final List<Book> books = new ArrayList<>();

        executeWithConnection(conn -> {
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                while (rs.next()) {
                    books.add(mapResultSetToBook(rs));
                }
                log.debug("Отримано {} книг(и) з БД.", books.size());
            }
            return null;
        });
        return books;
    }

    /**
     * Повертає список книг з бази даних за вказаним статусом читання.
     *
     * @param status Статус читання {@link ReadingStatus}.
     * @return {@link List} книг з відповідним статусом.
     * @throws DataAccessException Якщо виникає помилка SQL під час виконання операції.
     */
    @Override
    public List<Book> getBooksByStatus(ReadingStatus status) {
        log.debug("Спроба отримати книги зі статусом: {}", status);
        String sql = "SELECT * FROM books WHERE status = ? ORDER BY dateAdded DESC";
        final List<Book> books = new ArrayList<>();

        executeWithConnection(conn -> {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, status.name());

                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        books.add(mapResultSetToBook(rs));
                    }
                }
                log.debug("Отримано {} книг(и) зі статусом {}.", books.size(), status);
            }
            return null;
        });
        return books;
    }

    /**
     * Повертає список улюблених книг з бази даних.
     *
     * @return {@link List} улюблених книг.
     * @throws DataAccessException Якщо виникає помилка SQL під час виконання операції.
     */
    @Override
    public List<Book> getFavoriteBooks() {
        log.debug("Спроба отримати список улюблених книг.");
        String sql = "SELECT * FROM books WHERE favorite = 1 ORDER BY dateAdded DESC";
        final List<Book> books = new ArrayList<>();

        executeWithConnection(conn -> {
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                while (rs.next()) {
                    books.add(mapResultSetToBook(rs));
                }
                log.debug("Отримано {} улюблених книг(и).", books.size());
            }
            return null;
        });
        return books;
    }

    /**
     * Здійснює пошук книг за частковим збігом у назві або імені автора (фільтрація на боці Java).
     *
     * @param searchTerm Рядок для пошуку.
     * @return {@link List} знайдених книг, відсортованих за назвою (без урахування регістру).
     * @throws DataAccessException Якщо виникає помилка SQL під час отримання всіх книг.
     */
    @Override
    public List<Book> searchBooks(String searchTerm) {
        log.debug("Спроба пошуку книг за запитом: '{}' (фільтрація на боці Java)", searchTerm);
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            // Якщо пошуковий термін порожній, можна повернути всі книги
            // або порожній список, залежно від бажаної логіки.
            // Повернення getAllBooks() може бути логічним, якщо користувач очистив поле пошуку.
            return getAllBooks();
        }

        String lowerCaseSearchTerm = searchTerm.toLowerCase();

        // Отримуємо всі книги, а потім фільтруємо та сортуємо в Java
        List<Book> allBooks = getAllBooks(); // Цей метод вже сортує за dateAdded DESC

        List<Book> foundBooks = allBooks.stream()
                .filter(book -> {
                    boolean titleMatches = false;
                    if (book.getTitle() != null) {
                        titleMatches = book.getTitle().toLowerCase().contains(lowerCaseSearchTerm);
                    }
                    boolean authorMatches = false;
                    if (book.getAuthor() != null) {
                        authorMatches = book.getAuthor().toLowerCase().contains(lowerCaseSearchTerm);
                    }
                    return titleMatches || authorMatches;
                })
                // Сортуємо результат за назвою, без урахування регістру, для відповідності попередній логіці SQL ORDER BY title ASC
                .sorted(Comparator.comparing(
                        book -> book.getTitle() == null ? "" : book.getTitle(), // Обробка null назв
                        String.CASE_INSENSITIVE_ORDER
                ))
                .collect(Collectors.toList());

        log.debug("Знайдено {} книг(и) за запитом '{}' (фільтрація на боці Java).", foundBooks.size(), searchTerm);
        return foundBooks;
    }

    /**
     * Допоміжний приватний метод для перетворення поточного рядка {@link ResultSet} на об'єкт {@link Book}.
     * Інкапсулює логіку мапінгу даних з ResultSet на поля об'єкта Book.
     *
     * @param rs {@link ResultSet}, курсор якого встановлено на рядок з даними книги.
     * @return Створений об'єкт {@link Book}.
     * @throws SQLException Якщо виникає помилка доступу до даних у {@link ResultSet}.
     */
    private Book mapResultSetToBook(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        String title = rs.getString("title");
        String author = rs.getString("author");
        String genre = rs.getString("genre");
        ReadingStatus status = ReadingStatus.valueOf(rs.getString("status"));

        LocalDate dateAdded = LocalDate.parse(rs.getString("dateAdded"));
        String dateReadStr = rs.getString("dateRead");
        LocalDate dateRead = (dateReadStr != null && !dateReadStr.isEmpty()) ? LocalDate.parse(dateReadStr) : null;

        int rating = rs.getInt("rating");
        String comment = rs.getString("comment");
        String coverImagePath = rs.getString("coverImagePath");
        boolean favorite = rs.getInt("favorite") == 1;

        return new Book(id, title, author, genre, status, dateAdded, dateRead, rating, comment, coverImagePath, favorite);
    }

    /**
     * Допоміжний приватний метод для встановлення параметрів {@link PreparedStatement} з полів об'єкта {@link Book}.
     * Використовується для уникнення дублювання коду в методах {@code addBook} та {@code updateBook}.
     *
     * @param pstmt Об'єкт {@link PreparedStatement}, для якого встановлюються параметри.
     * @param book Об'єкт {@link Book}, з якого беруться значення.
     * @throws SQLException Якщо виникає помилка під час встановлення параметрів.
     */
    private void setBookParameters(PreparedStatement pstmt, Book book) throws SQLException {
        pstmt.setString(1, book.getTitle());
        pstmt.setString(2, book.getAuthor());
        pstmt.setString(3, book.getGenre());
        pstmt.setString(4, book.getStatus().name());
        pstmt.setString(5, book.getDateAdded().toString());
        if (book.getDateRead() != null) {
            pstmt.setString(6, book.getDateRead().toString());
        } else {
            pstmt.setNull(6, Types.VARCHAR);
        }
        pstmt.setInt(7, book.getRating());
        pstmt.setString(8, book.getComment());
        pstmt.setString(9, book.getCoverImagePath());
        pstmt.setInt(10, book.isFavorite() ? 1 : 0);
    }

    /**
     * Повертає список унікальних жанрів книг з бази даних.
     *
     * @return {@link List} рядків з назвами унікальних жанрів.
     *         Повертає порожній список у разі помилки SQL, щоб уникнути падіння UI.
     */
    @Override
    public List<String> getDistinctGenres() {
        log.debug("Спроба отримати список унікальних жанрів.");
        String sql = "SELECT DISTINCT genre FROM books WHERE genre IS NOT NULL AND genre != '' ORDER BY genre ASC";
        final List<String> genres = new ArrayList<>();

        executeWithConnection(conn -> {
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                while (rs.next()) {
                    genres.add(rs.getString("genre"));
                }
                log.debug("Знайдено {} унікальних жанрів.", genres.size());
            }
            return null;
        });
        return genres;
    }

    // --- Реалізація методів для статистики ---

    /**
     * Повертає загальну кількість прочитаних книг.
     *
     * @return Загальна кількість книг зі статусом {@link ReadingStatus#READ}.
     *         Повертає 0 у разі помилки SQL, щоб уникнути переривання роботи статистики.
     */
    @Override
    public int getTotalBooksReadCount() {
        String sql = "SELECT COUNT(*) FROM books WHERE status = ?";
        final Holder<Integer> count = new Holder<>(0);

        executeWithConnection(conn -> {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, ReadingStatus.READ.name());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        count.value = rs.getInt(1);
                    }
                }
                log.debug("Загальна кількість прочитаних книг: {}.", count.value);
            }
            return null;
        });
        return count.value;
    }

    /**
     * Повертає кількість книг, прочитаних у вказаному році.
     *
     * @param year Рік для підрахунку.
     * @return Кількість книг зі статусом {@link ReadingStatus#READ}, прочитаних у вказаному році.
     *         Повертає 0 у разі помилки SQL.
     */
    @Override
    public int getBooksReadCountByYear(int year) {
        String sql = "SELECT COUNT(*) FROM books WHERE status = ? AND strftime('%Y', dateRead) = ?";
        final Holder<Integer> count = new Holder<>(0);

        executeWithConnection(conn -> {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, ReadingStatus.READ.name());
                pstmt.setString(2, String.valueOf(year));
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        count.value = rs.getInt(1);
                    }
                }
                log.debug("Кількість книг, прочитаних у {} році: {}.", year, count.value);
            }
            return null;
        });
        return count.value;
    }

    /**
     * Повертає кількість книг, прочитаних у вказаному місяці та році.
     *
     * @param year Рік.
     * @param month Місяць (1-12).
     * @return Кількість книг зі статусом {@link ReadingStatus#READ}, прочитаних у вказаному місяці та році.
     *         Повертає 0 у разі помилки SQL.
     */
    @Override
    public int getBooksReadCountByMonthAndYear(int year, int month) {
        String sql = "SELECT COUNT(*) FROM books WHERE status = ? AND strftime('%Y', dateRead) = ? AND strftime('%m', dateRead) = ?";
        final Holder<Integer> count = new Holder<>(0);
        String monthFormatted = String.format("%02d", month);

        executeWithConnection(conn -> {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, ReadingStatus.READ.name());
                pstmt.setString(2, String.valueOf(year));
                pstmt.setString(3, monthFormatted);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        count.value = rs.getInt(1);
                    }
                }
                log.debug("Кількість книг, прочитаних у {}-{}: {}.", year, monthFormatted, count.value);
            }
            return null;
        });
        return count.value;
    }

    /**
     * Допоміжний клас для зберігання змінюваних значень у лямбда-виразах.
     */
    private static class Holder<T> {
        T value;

        Holder(T value) {
            this.value = value;
        }
    }
}