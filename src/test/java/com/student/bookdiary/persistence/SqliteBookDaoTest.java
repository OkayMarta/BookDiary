package com.student.bookdiary.persistence;

import com.student.bookdiary.model.Book;
import com.student.bookdiary.model.ReadingStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тестовий клас для {@link SqliteBookDao}.
 * Цей клас успадковує {@link BaseDaoTest} для спільної інфраструктури тестування бази даних.
 * Він перевіряє всі CRUD-операції (Create, Read, Update, Delete) та інші специфічні
 * методи запитів до таблиці {@code books}.
 */
class SqliteBookDaoTest extends BaseDaoTest {

    private SqliteBookDao bookDao; // Об'єкт DAO для тестування
    private Book testBook; // Тестовий об'єкт книги
    private final LocalDate TODAY = LocalDate.now(); // Поточна дата для використання в тестах

    /**
     * Метод, що виконується перед кожним тестовим методом.
     * Ініціалізує {@link SqliteBookDao}, очищає таблицю {@code books}
     * та створює тестовий об'єкт {@link Book} для використання в тестах.
     *
     * @throws Exception якщо виникає помилка під час налаштування.
     */
    @BeforeEach
    void setUpBookDao() throws Exception { // Використання Exception для покриття SQLException з clearTable
        bookDao = new SqliteBookDao();
        clearTable("books"); // Очищення таблиці перед кожним тестом

        // Створюємо тестовий об'єкт книги з усіма заповненими полями
        testBook = new Book(
                "Кобзар",
                "Тарас Шевченко",
                "Поезія",
                ReadingStatus.READ,
                "covers/kobzar.jpg"
        );
        testBook.setDateRead(TODAY);
        testBook.setRating(5);
        testBook.setComment("Чудова книга");
        testBook.setFavorite(true);
        // dateAdded встановлюється автоматично в конструкторі Book,
        // але для консистентності тестів ми можемо встановити його явно.
        testBook.setDateAdded(TODAY);
    }

    /**
     * Тестує метод {@link SqliteBookDao#addBook(Book)}.
     * Перевіряє, чи книга успішно додається до бази даних,
     * чи коректно встановлюється ID для нової книги,
     * та чи всі поля збереженої книги відповідають оригінальним.
     *
     * @throws Exception якщо виникає помилка під час виконання тесту.
     */
    @Test
    void testAddBook() throws Exception {
        // When: Додаємо книгу
        bookDao.addBook(testBook);

        // Then: Перевіряємо, що книга додана успішно
        assertEquals(1, getTableRowCount("books"), "Кількість книг в БД повинна бути 1 після додавання.");

        // And: Отримуємо збережену книгу і перевіряємо її поля
        // Використовуємо getAllBooks для отримання, оскільки ID встановлюється базою даних.
        List<Book> books = bookDao.getAllBooks();
        assertEquals(1, books.size(), "Список книг з БД повинен містити 1 книгу.");

        Book savedBook = books.get(0);
        assertNotEquals(0, savedBook.getId(), "ID збереженої книги не повинен бути 0 (має бути встановлений БД).");
        assertEquals(testBook.getTitle(), savedBook.getTitle(), "Назва книги не співпадає.");
        assertEquals(testBook.getAuthor(), savedBook.getAuthor(), "Автор книги не співпадає.");
        assertEquals(testBook.getGenre(), savedBook.getGenre(), "Жанр книги не співпадає.");
        assertEquals(testBook.getStatus(), savedBook.getStatus(), "Статус читання не співпадає.");
        assertEquals(testBook.getCoverImagePath(), savedBook.getCoverImagePath(), "Шлях до обкладинки не співпадає.");
        assertEquals(testBook.getDateRead(), savedBook.getDateRead(), "Дата прочитання не співпадає.");
        assertEquals(testBook.getRating(), savedBook.getRating(), "Рейтинг не співпадає.");
        assertEquals(testBook.getComment(), savedBook.getComment(), "Коментар не співпадає.");
        assertEquals(testBook.isFavorite(), savedBook.isFavorite(), "Статус 'улюблене' не співпадає.");
        assertEquals(testBook.getDateAdded(), savedBook.getDateAdded(), "Дата додавання не співпадає.");
    }

    /**
     * Тестує метод {@link SqliteBookDao#getBookById(long)}.
     * Перевіряє, чи книга може бути успішно знайдена за її ID,
     * а також чи повертається порожній {@link Optional} для неіснуючого ID.
     *
     * @throws Exception якщо виникає помилка під час виконання тесту.
     */
    @Test
    void testGetBookById() throws Exception {
        // Given: Додаємо книгу і отримуємо її ID
        bookDao.addBook(testBook);
        List<Book> books = bookDao.getAllBooks(); // Отримуємо книгу, щоб дізнатися її ID
        Book addedBook = books.get(0);

        // When: Отримуємо книгу за її ID
        Optional<Book> foundBookOpt = bookDao.getBookById(addedBook.getId());

        // Then: Перевіряємо, що книга знайдена і її поля коректні
        assertTrue(foundBookOpt.isPresent(), "Книга повинна бути знайдена за існуючим ID.");
        Book foundBook = foundBookOpt.get();
        assertEquals(addedBook.getId(), foundBook.getId(), "ID знайденої книги не співпадає.");
        assertEquals(addedBook.getTitle(), foundBook.getTitle(), "Назва знайденої книги не співпадає.");

        // When: Шукаємо книгу за неіснуючим ID
        Optional<Book> notFoundOpt = bookDao.getBookById(999L); // Припускаємо, що ID 999L не існує

        // Then: Перевіряємо, що результат порожній (Optional.empty())
        assertTrue(notFoundOpt.isEmpty(), "Для неіснуючого ID повинен повертатися порожній Optional.");
    }

    /**
     * Тестує метод {@link SqliteBookDao#updateBook(Book)}.
     * Перевіряє, чи дані існуючої книги успішно оновлюються в базі даних.
     *
     * @throws Exception якщо виникає помилка під час виконання тесту.
     */
    @Test
    void testUpdateBook() throws Exception {
        // Given: Додаємо книгу і отримуємо її з БД (з встановленим ID)
        bookDao.addBook(testBook);
        List<Book> books = bookDao.getAllBooks();
        Book addedBook = books.get(0);

        // When: Змінюємо дані книги
        addedBook.setTitle("Оновлена назва");
        addedBook.setAuthor("Оновлений автор");
        addedBook.setStatus(ReadingStatus.WANT_TO_READ);
        addedBook.setRating(3);
        addedBook.setComment("Оновлений коментар");
        addedBook.setFavorite(false);
        addedBook.setDateRead(null); // Припустимо, статус змінився на "хочу прочитати"
        // Оновлюємо дату додавання, хоча зазвичай вона не змінюється. Для повноти тесту.
        addedBook.setDateAdded(TODAY.plusDays(1));
        bookDao.updateBook(addedBook);

        // Then: Отримуємо оновлену книгу і перевіряємо зміни
        Optional<Book> updatedBookOpt = bookDao.getBookById(addedBook.getId());
        assertTrue(updatedBookOpt.isPresent(), "Оновлена книга повинна бути знайдена в БД.");
        Book updatedBook = updatedBookOpt.get();
        assertEquals("Оновлена назва", updatedBook.getTitle(), "Назва книги не оновлена.");
        assertEquals("Оновлений автор", updatedBook.getAuthor(), "Автор книги не оновлений.");
        assertEquals(ReadingStatus.WANT_TO_READ, updatedBook.getStatus(), "Статус читання не оновлений.");
        assertEquals(3, updatedBook.getRating(), "Рейтинг не оновлений.");
        assertEquals("Оновлений коментар", updatedBook.getComment(), "Коментар не оновлений.");
        assertFalse(updatedBook.isFavorite(), "Статус 'улюблене' не оновлений.");
        assertNull(updatedBook.getDateRead(), "Дата прочитання не оновлена (має бути null).");
        assertEquals(TODAY.plusDays(1), updatedBook.getDateAdded(), "Дата додавання не оновлена.");
    }

    /**
     * Тестує метод {@link SqliteBookDao#deleteBook(long)}.
     * Перевіряє, чи книга успішно видаляється з бази даних за її ID.
     *
     * @throws Exception якщо виникає помилка під час виконання тесту.
     */
    @Test
    void testDeleteBook() throws Exception {
        // Given: Додаємо книгу
        bookDao.addBook(testBook);
        List<Book> books = bookDao.getAllBooks();
        Book addedBook = books.get(0);
        assertEquals(1, getTableRowCount("books"), "Книга повинна бути в БД перед видаленням.");


        // When: Видаляємо книгу
        bookDao.deleteBook(addedBook.getId());

        // Then: Перевіряємо, що книга видалена
        Optional<Book> deletedBookOpt = bookDao.getBookById(addedBook.getId());
        assertTrue(deletedBookOpt.isEmpty(), "Книга не повинна бути знайдена після видалення.");
        assertEquals(0, getTableRowCount("books"), "Таблиця 'books' повинна бути порожньою після видалення єдиної книги.");
    }

    /**
     * Тестує метод {@link SqliteBookDao#getAllBooks()}.
     * Перевіряє, чи метод повертає коректний список всіх книг з бази даних,
     * а також порожній список, якщо таблиця порожня.
     *
     * @throws Exception якщо виникає помилка під час виконання тесту.
     */
    @Test
    void testGetAllBooks() throws Exception {
        // Given: Таблиця порожня
        assertTrue(bookDao.getAllBooks().isEmpty(), "Список книг повинен бути порожнім для порожньої таблиці.");

        // Given: Додаємо кілька книг
        bookDao.addBook(testBook); // "Кобзар"

        Book anotherBook = new Book(
                "Інша книга",
                "Інший автор",
                "Проза",
                ReadingStatus.WANT_TO_READ,
                "covers/other.jpg" // Додано шлях до обкладинки
        );
        anotherBook.setDateAdded(TODAY.minusDays(1)); // Явно встановлюємо дату додавання
        bookDao.addBook(anotherBook);

        // When: Отримуємо всі книги
        List<Book> allBooks = bookDao.getAllBooks();

        // Then: Перевіряємо список книг
        assertEquals(2, allBooks.size(), "Неправильна кількість книг у списку всіх книг.");
        // Перевіряємо наявність обох книг за назвами
        assertTrue(allBooks.stream()
                .anyMatch(b -> b.getTitle().equals("Кобзар")), "Книга 'Кобзар' відсутня у списку.");
        assertTrue(allBooks.stream()
                .anyMatch(b -> b.getTitle().equals("Інша книга")), "Книга 'Інша книга' відсутня у списку.");

        // When: Очищаємо таблицю (через метод базового класу)
        clearTable("books");

        // Then: Перевіряємо, що отримуємо порожній список
        assertTrue(bookDao.getAllBooks().isEmpty(), "Список книг не порожній після очищення таблиці.");
    }

    /**
     * Тестує метод {@link SqliteBookDao#getBooksByStatus(ReadingStatus)}.
     * Перевіряє, чи метод коректно фільтрує та повертає книги за вказаним статусом читання.
     *
     * @throws Exception якщо виникає помилка під час виконання тесту.
     */
    @Test
    void testGetBooksByStatus() throws Exception {
        // Given: Додаємо книги з різними статусами
        bookDao.addBook(testBook); // Статус: READ

        Book wantToReadBook = new Book(
                "Майбутня книга",
                "Майбутній автор",
                "Фантастика",
                ReadingStatus.WANT_TO_READ, // Статус: WANT_TO_READ
                "covers/future.jpg"
        );
        wantToReadBook.setDateAdded(TODAY); // Явно встановлюємо дату
        bookDao.addBook(wantToReadBook);

        // When: Отримуємо книги за статусом READ
        List<Book> readBooks = bookDao.getBooksByStatus(ReadingStatus.READ);
        // When: Отримуємо книги за статусом WANT_TO_READ
        List<Book> wantToReadBooks = bookDao.getBooksByStatus(ReadingStatus.WANT_TO_READ);

        // Then: Перевіряємо результати
        assertEquals(1, readBooks.size(), "Неправильна кількість книг зі статусом READ.");
        assertEquals("Кобзар", readBooks.get(0).getTitle(), "Назва книги зі статусом READ не співпадає.");

        assertEquals(1, wantToReadBooks.size(), "Неправильна кількість книг зі статусом WANT_TO_READ.");
        assertEquals("Майбутня книга", wantToReadBooks.get(0).getTitle(), "Назва книги зі статусом WANT_TO_READ не співпадає.");
    }

    /**
     * Тестує метод {@link SqliteBookDao#getFavoriteBooks()}.
     * Перевіряє, чи метод коректно повертає список лише улюблених книг.
     *
     * @throws Exception якщо виникає помилка під час виконання тесту.
     */
    @Test
    void testGetFavoriteBooks() throws Exception {
        // Given: Додаємо улюблені і звичайні книги
        testBook.setFavorite(true); // testBook вже налаштований як улюблений
        bookDao.addBook(testBook);

        Book nonFavoriteBook = new Book(
                "Звичайна книга",
                "Звичайний автор",
                "Проза",
                ReadingStatus.READ,
                null // Шлях до обкладинки може бути null
        );
        nonFavoriteBook.setFavorite(false); // Явно встановлюємо як не улюблену
        nonFavoriteBook.setDateAdded(TODAY); // Встановлюємо дату додавання
        bookDao.addBook(nonFavoriteBook);

        // When: Отримуємо улюблені книги
        List<Book> favoriteBooks = bookDao.getFavoriteBooks();

        // Then: Перевіряємо результати
        assertEquals(1, favoriteBooks.size(), "Неправильна кількість улюблених книг.");
        assertEquals("Кобзар", favoriteBooks.get(0).getTitle(), "Назва улюбленої книги не співпадає.");
        assertTrue(favoriteBooks.get(0).isFavorite(), "Книга у списку улюблених повинна мати прапорець favorite=true.");
    }

    /**
     * Тестує метод {@link SqliteBookDao#searchBooks(String)}.
     * Перевіряє, чи пошук коректно знаходить книги за частковим співпадінням
     * у назві або авторі, без урахування регістру.
     * Також перевіряє, чи повертається порожній список, якщо співпадінь немає.
     *
     * @throws Exception якщо виникає помилка під час виконання тесту.
     */
    @Test
    void testSearchBooks() throws Exception {
        // Given: Додаємо книги для пошуку
        Book book1 = new Book(
                "Кобзар Великий", // Змінена назва для більш точного тестування пошуку
                "Тарас Шевченко",
                "Поезія",
                ReadingStatus.READ,
                null
        );
        book1.setDateAdded(LocalDate.now()); // Важливо для NOT NULL constraint
        bookDao.addBook(book1);

        Book book2 = new Book(
                "Заповіт Кобзаря", // Змінена назва
                "Інший Тарас",    // Змінений автор
                "Поезія",
                ReadingStatus.WANT_TO_READ,
                null
        );
        book2.setDateAdded(LocalDate.now());
        bookDao.addBook(book2);

        Book book3 = new Book(
                "Інша книга",
                "Шевченко Тарас Григорович", // Повне ім'я для тестування пошуку
                "Проза",
                ReadingStatus.READ,
                null
        );
        book3.setDateAdded(LocalDate.now());
        bookDao.addBook(book3);

        // When & Then: Пошук за фрагментом назви (без урахування регістру)
        List<Book> booksByTitleFragment = bookDao.searchBooks("кобзар"); // Пошуковий запит в нижньому регістрі
        assertEquals(2, booksByTitleFragment.size(), "Неправильна кількість книг при пошуку за фрагментом 'кобзар'.");
        // Перевіряємо, що всі знайдені книги дійсно містять "кобзар" в назві (без урахування регістру)
        assertTrue(booksByTitleFragment.stream()
                        .allMatch(b -> b.getTitle().toLowerCase().contains("кобзар")),
                "Не всі знайдені книги містять 'кобзар' у назві.");

        // When & Then: Пошук за фрагментом автора (без урахування регістру)
        List<Book> booksByAuthorFragment = bookDao.searchBooks("шевченко");
        assertEquals(2, booksByAuthorFragment.size(), "Неправильна кількість книг при пошуку за фрагментом 'шевченко'.");
        // Перевіряємо, що всі знайдені книги дійсно містять "шевченко" в імені автора
        assertTrue(booksByAuthorFragment.stream()
                        .allMatch(b -> b.getAuthor().toLowerCase().contains("шевченко")),
                "Не всі знайдені книги містять 'шевченко' в імені автора.");

        // When & Then: Пошук за неіснуючим рядком
        List<Book> noResults = bookDao.searchBooks("неіснуючийпошуковийзапит123");
        assertTrue(noResults.isEmpty(), "Пошук за неіснуючим рядком повинен повертати порожній список.");
    }

    /**
     * Тестує метод {@link SqliteBookDao#getDistinctGenres()}.
     * Перевіряє, чи метод коректно повертає список унікальних жанрів з бази даних,
     * ігноруючи дублікати та значення null.
     *
     * @throws Exception якщо виникає помилка під час виконання тесту.
     */
    @Test
    void testGetDistinctGenres() throws Exception {
        // Given: Додаємо книги з різними та однаковими жанрами, а також з null жанром
        bookDao.addBook(testBook); // Жанр: Поезія

        Book bookProza = new Book("Книга Прози", "Автор Прози", "Проза", ReadingStatus.READ, null);
        bookProza.setDateAdded(TODAY);
        bookDao.addBook(bookProza); // Жанр: Проза

        Book bookPoeziyaDuplicate = new Book("Ще Поезія", "Інший Поет", "Поезія", ReadingStatus.READ, null);
        bookPoeziyaDuplicate.setDateAdded(TODAY);
        bookDao.addBook(bookPoeziyaDuplicate); // Жанр: Поезія (дублікат)

        Book bookNullGenre = new Book("Без Жанру", "Автор Невизначений", null, ReadingStatus.READ, null);
        bookNullGenre.setDateAdded(TODAY);
        bookDao.addBook(bookNullGenre); // Жанр: null

        Book bookEmptyGenre = new Book("Порожній Жанр", "Автор Порожній", "", ReadingStatus.READ, null);
        bookEmptyGenre.setDateAdded(TODAY);
        // bookDao.addBook(bookEmptyGenre); // Пустий рядок як жанр може оброблятися по-різному базою даних.
        // Краще уникати або тестувати окремо, якщо це дозволено логікою.
        // Наразі цей випадок не додається.

        // When: Отримуємо унікальні жанри
        List<String> genres = bookDao.getDistinctGenres();

        // Then: Перевіряємо результати
        // Очікуємо 2 унікальних жанри ("Поезія", "Проза"). null і порожні рядки ігноруються запитом.
        assertEquals(2, genres.size(), "Неправильна кількість унікальних жанрів. Мають бути лише 'Поезія' та 'Проза'.");
        assertTrue(genres.contains("Поезія"), "Список унікальних жанрів повинен містити 'Поезія'.");
        assertTrue(genres.contains("Проза"), "Список унікальних жанрів повинен містити 'Проза'.");
        // Перевіряємо, що null або порожні рядки не включені, якщо вони є в БД і DISTINCT їх не відфільтровує
        // (залежить від реалізації запиту - зазвичай DISTINCT для null дає один null, якщо є).
        // Наш запит "WHERE genre IS NOT NULL AND genre != ''" має їх відфільтрувати.
    }

    /**
     * Тестує статистичні методи DAO:
     * {@link SqliteBookDao#getTotalBooksReadCount()},
     * {@link SqliteBookDao#getBooksReadCountByYear(int)},
     * {@link SqliteBookDao#getBooksReadCountByMonthAndYear(int, int)}.
     * Перевіряє коректність підрахунку прочитаних книг загалом, за рік та за місяць/рік.
     *
     * @throws Exception якщо виникає помилка під час виконання тесту.
     */
    @Test
    void testGetBooksStatistics() throws Exception {
        // Given: Додаємо книги з різними датами прочитання та статусами
        Book bookReadJan2024 = new Book("Книга Січень 2024", "Автор 1", "Проза", ReadingStatus.READ, null);
        bookReadJan2024.setDateRead(LocalDate.of(2024, 1, 15));
        bookReadJan2024.setDateAdded(TODAY); // dateAdded є NOT NULL
        bookDao.addBook(bookReadJan2024);

        Book bookReadFeb2024 = new Book("Книга Лютий 2024", "Автор 2", "Проза", ReadingStatus.READ, null);
        bookReadFeb2024.setDateRead(LocalDate.of(2024, 2, 1));
        bookReadFeb2024.setDateAdded(TODAY);
        bookDao.addBook(bookReadFeb2024);

        Book bookWantToRead = new Book("Книга Хочу Прочитати", "Автор 3", "Проза", ReadingStatus.WANT_TO_READ, null);
        // dateRead залишається null для книг, які не прочитані
        bookWantToRead.setDateAdded(TODAY);
        bookDao.addBook(bookWantToRead);

        Book bookReadJan2023 = new Book("Книга Січень 2023", "Автор 4", "Поезія", ReadingStatus.READ, null);
        bookReadJan2023.setDateRead(LocalDate.of(2023, 1, 10));
        bookReadJan2023.setDateAdded(TODAY);
        bookDao.addBook(bookReadJan2023);

        // When & Then: Перевіряємо загальну кількість прочитаних книг
        int totalRead = bookDao.getTotalBooksReadCount();
        assertEquals(3, totalRead, "Неправильна загальна кількість прочитаних книг.");

        // When & Then: Перевіряємо кількість прочитаних книг за 2024 рік
        int readIn2024 = bookDao.getBooksReadCountByYear(2024);
        assertEquals(2, readIn2024, "Неправильна кількість книг, прочитаних у 2024 році.");

        // When & Then: Перевіряємо кількість прочитаних книг за 2023 рік
        int readIn2023 = bookDao.getBooksReadCountByYear(2023);
        assertEquals(1, readIn2023, "Неправильна кількість книг, прочитаних у 2023 році.");

        // When & Then: Перевіряємо кількість прочитаних книг за неіснуючий рік (наприклад, майбутній)
        int readIn2025 = bookDao.getBooksReadCountByYear(2025);
        assertEquals(0, readIn2025, "Кількість прочитаних книг у 2025 році повинна бути 0.");

        // When & Then: Перевіряємо кількість прочитаних книг за січень 2024 року
        int readInJan2024 = bookDao.getBooksReadCountByMonthAndYear(2024, 1);
        assertEquals(1, readInJan2024, "Неправильна кількість книг, прочитаних у січні 2024 року.");

        // When & Then: Перевіряємо кількість прочитаних книг за лютий 2024 року
        int readInFeb2024 = bookDao.getBooksReadCountByMonthAndYear(2024, 2);
        assertEquals(1, readInFeb2024, "Неправильна кількість книг, прочитаних у лютому 2024 року.");

        // When & Then: Перевіряємо кількість прочитаних книг за березень 2024 року (має бути 0)
        int readInMar2024 = bookDao.getBooksReadCountByMonthAndYear(2024, 3);
        assertEquals(0, readInMar2024, "Кількість книг, прочитаних у березні 2024 року, повинна бути 0.");

        // When & Then: Перевіряємо кількість прочитаних книг за січень 2023 року
        int readInJan2023 = bookDao.getBooksReadCountByMonthAndYear(2023, 1);
        assertEquals(1, readInJan2023, "Неправильна кількість книг, прочитаних у січні 2023 року.");
    }

    /**
     * Тестує поведінку методу {@link SqliteBookDao#addBook(Book)} при спробі додати книгу
     * з полем {@code title}, що дорівнює {@code null}.
     * Очікується, що буде згенеровано виняток {@link DataAccessException}, оскільки
     * поле {@code title} в базі даних має обмеження {@code NOT NULL}.
     * Перевіряється, що {@link DataAccessException} має причину {@link SQLException}.
     */
    @Test
    void testAddBook_withNullTitle_shouldThrowDataAccessExceptionWrappingSqlException() {
        // Given: Створюємо книгу з title = null
        // Важливо: інші NOT NULL поля (status, dateAdded) мають бути валідними,
        // щоб помилка виникла саме через title на рівні БД, а не через NPE в setBookParameters чи конструкторі.
        Book bookWithNullTitle = new Book(
                null, // Title є null
                "Тарас Шевченко",
                "Поезія",
                ReadingStatus.READ, // Status є NOT NULL
                "covers/kobzar.jpg"
        );
        // dateAdded встановлюється автоматично в конструкторі і є NOT NULL

        // When & Then: Очікуємо DataAccessException
        DataAccessException exception = assertThrows(DataAccessException.class, () -> {
            bookDao.addBook(bookWithNullTitle);
        }, "Додавання книги з null title повинно генерувати DataAccessException.");

        // Перевіряємо, що повідомлення винятку вказує на помилку бази даних
        assertTrue(exception.getMessage().toLowerCase().contains("помилка") &&
                        exception.getMessage().toLowerCase().contains("базою даних"),
                "Повідомлення винятку повинно вказувати на помилку бази даних.");

        // Перевіряємо, що причиною DataAccessException є SQLException
        assertNotNull(exception.getCause(), "DataAccessException повинен мати причину (cause).");
        assertTrue(exception.getCause() instanceof SQLException,
                "Причиною DataAccessException має бути SQLException.");
    }
}