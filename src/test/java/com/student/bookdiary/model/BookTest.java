package com.student.bookdiary.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Тестовий клас для {@link Book}.
 * Цей клас містить набір тестів для перевірки коректності роботи
 * конструкторів, гетерів, сетерів та методів equals, hashCode, toString класу Book.
 */
class BookTest {
    private Book book1; // Книга, створена для тестування, ініціалізується в setUp()
    private Book book2; // Інша книга, створена для тестування, ініціалізується в setUp()
    private final LocalDate TODAY = LocalDate.now(); // Поточна дата для використання в тестах
    private final LocalDate YESTERDAY = LocalDate.now().minusDays(1); // Вчорашня дата для використання в тестах

    /**
     * Налаштовує тестове середовище перед кожним тестом.
     * Ініціалізує об'єкти book1 та book2 з різними параметрами.
     */
    @BeforeEach
    void setUp() {
        // book1 - створюється через конструктор для нових записів (id = 0)
        book1 = new Book("Кобзар", "Тарас Шевченко", "Поезія",
                ReadingStatus.READ, "covers/kobzar.jpg");
        // Заповнюємо решту полів для book1, які ініціалізуються за замовчуванням або сеттерами
        book1.setDateRead(TODAY);
        book1.setRating(5);
        book1.setComment("Чудовий коментар");
        book1.setFavorite(true);


        // book2 - створюється через повний конструктор, з явно вказаним id = 1L
        book2 = new Book(1L, "Кобзар", "Тарас Шевченко", "Поезія",
                ReadingStatus.READ, TODAY, TODAY, 5, "Чудова книга",
                "covers/kobzar.jpg", true);
    }

    /**
     * Тестує конструктор за замовчуванням класу {@link Book}.
     * Перевіряє, що новостворений об'єкт не є null та має id, встановлений в 0.
     */
    @Test
    void testDefaultConstructor() {
        Book book = new Book();
        assertNotNull(book, "Об'єкт книги не повинен бути null після створення конструктором за замовчуванням.");
        assertEquals(0, book.getId(), "ID книги за замовчуванням має бути 0."); // Перевірка, що id за замовчуванням 0
    }

    /**
     * Тестує "простий" конструктор класу {@link Book}, який приймає основні параметри.
     * Перевіряє коректність ініціалізації всіх полів книги.
     */
    @Test
    void testSimpleConstructor() {
        Book newBook = new Book("Назва", "Автор", "Жанр", ReadingStatus.WANT_TO_READ, "path/to/cover.jpg");
        assertAll("Перевірка значень полів після простого конструктора",
                () -> assertEquals(0, newBook.getId(), "ID має бути 0 для нового запису."), // ID має бути 0 для нового запису
                () -> assertEquals("Назва", newBook.getTitle(), "Назва книги має бути коректно встановлена."),
                () -> assertEquals("Автор", newBook.getAuthor(), "Автор книги має бути коректно встановлений."),
                () -> assertEquals("Жанр", newBook.getGenre(), "Жанр книги має бути коректно встановлений."),
                () -> assertEquals(ReadingStatus.WANT_TO_READ, newBook.getStatus(), "Статус читання має бути коректно встановлений."),
                () -> assertEquals("path/to/cover.jpg", newBook.getCoverImagePath(), "Шлях до обкладинки має бути коректно встановлений."),
                () -> assertEquals(TODAY, newBook.getDateAdded(), "Дата додавання має автоматично встановлюватися на поточну дату."), // Автоматично встановлюється поточна дата
                () -> assertNull(newBook.getDateRead(), "Дата прочитання має бути null за замовчуванням."), // Дата прочитання не встановлена
                () -> assertEquals(0, newBook.getRating(), "Рейтинг має бути 0 за замовчуванням."), // Рейтинг за замовчуванням
                () -> assertNull(newBook.getComment(), "Коментар має бути null за замовчуванням."), // Коментар не встановлений
                () -> assertFalse(newBook.isFavorite(), "Книга не має бути в улюблених за замовчуванням.") // Не в улюблених за замовчуванням
        );
    }

    /**
     * Тестує "повний" конструктор класу {@link Book}, який ініціалізує всі поля.
     * Використовує об'єкт book2, створений у методі setUp().
     */
    @Test
    void testFullConstructor() {
        assertAll("Перевірка значень полів після повного конструктора для book2",
                () -> assertEquals(1L, book2.getId()),
                () -> assertEquals("Кобзар", book2.getTitle()),
                () -> assertEquals("Тарас Шевченко", book2.getAuthor()),
                () -> assertEquals("Поезія", book2.getGenre()),
                () -> assertEquals(ReadingStatus.READ, book2.getStatus()),
                () -> assertEquals(TODAY, book2.getDateAdded()),
                () -> assertEquals(TODAY, book2.getDateRead()),
                () -> assertEquals(5, book2.getRating()),
                () -> assertEquals("Чудова книга", book2.getComment()),
                () -> assertEquals("covers/kobzar.jpg", book2.getCoverImagePath()),
                () -> assertTrue(book2.isFavorite())
        );
    }

    /**
     * Тестує роботу гетерів та сетерів класу {@link Book}.
     * Створює новий об'єкт, встановлює значення за допомогою сетерів та перевіряє їх за допомогою гетерів.
     */
    @Test
    void testSettersAndGetters() {
        Book book = new Book();
        LocalDate dateRead = LocalDate.now();

        book.setId(1L);
        book.setTitle("Нова книга");
        book.setAuthor("Новий автор");
        book.setGenre("Новий жанр");
        book.setStatus(ReadingStatus.WANT_TO_READ);
        book.setDateAdded(TODAY);
        book.setDateRead(dateRead);
        book.setRating(4);
        book.setComment("Тестовий коментар");
        book.setCoverImagePath("covers/new.jpg");
        book.setFavorite(true);

        assertAll("Перевірка гетерів після використання сетерів",
                () -> assertEquals(1L, book.getId()),
                () -> assertEquals("Нова книга", book.getTitle()),
                () -> assertEquals("Новий автор", book.getAuthor()),
                () -> assertEquals("Новий жанр", book.getGenre()),
                () -> assertEquals(ReadingStatus.WANT_TO_READ, book.getStatus()),
                () -> assertEquals(TODAY, book.getDateAdded()),
                () -> assertEquals(dateRead, book.getDateRead()),
                () -> assertEquals(4, book.getRating()),
                () -> assertEquals("Тестовий коментар", book.getComment()),
                () -> assertEquals("covers/new.jpg", book.getCoverImagePath()),
                () -> assertTrue(book.isFavorite())
        );
    }

    /**
     * Тестує метод {@link Book#equals(Object)}, коли об'єкти мають однаковий ненульовий ID.
     * В цьому випадку об'єкти повинні вважатися рівними.
     */
    @Test
    void testEquals_sameId() {
        Book book3 = new Book(1L, "Інша книга", "Інший автор", "Інший жанр",
                ReadingStatus.WANT_TO_READ, TODAY, null, 0, null, null, false);
        // book2 має id = 1L, book3 має id = 1L, тому вони мають бути рівними
        assertTrue(book2.equals(book3), "Книги з однаковим ненульовим ID мають бути рівними.");
    }

    /**
     * Тестує метод {@link Book#equals(Object)}, коли об'єкти мають різні ненульові ID.
     * В цьому випадку об'єкти не повинні вважатися рівними.
     */
    @Test
    void testEquals_differentId() {
        Book bookWithDifferentId = new Book(2L, "Кобзар", "Тарас Шевченко", "Поезія",
                ReadingStatus.READ, TODAY, TODAY, 5, "Чудова книга",
                "covers/kobzar.jpg", true);
        // book2 має id = 1L, bookWithDifferentId має id = 2L
        assertFalse(book2.equals(bookWithDifferentId), "Книги з різними ненульовими ID не мають бути рівними.");
    }

    /**
     * Тестує метод {@link Book#equals(Object)}, коли ID одного об'єкта дорівнює 0, а іншого - ні.
     * В цьому випадку порівняння повинно відбуватися за всіма полями об'єкта.
     */
    @Test
    void testEquals_oneIdZero() {
        Book newBookWithZeroId = new Book("Кобзар", "Тарас Шевченко", "Поезія",
                ReadingStatus.READ, "covers/kobzar.jpg");
        // Встановлюємо ті ж поля, що і в book1, але id = 0
        newBookWithZeroId.setDateAdded(TODAY);
        newBookWithZeroId.setDateRead(TODAY);
        newBookWithZeroId.setRating(5);
        // Змінюємо коментар, щоб відрізнити від book1, хоча порівняння буде з book2
        newBookWithZeroId.setComment("Чудовий коментар для книги з id=0");
        newBookWithZeroId.setFavorite(true);

        // book2 має id = 1L, newBookWithZeroId має id = 0L
        // Згідно з логікою equals:
        // Якщо this.id != 0 && other.id != 0, то порівнюються лише id.
        // В іншому випадку (тобто, якщо хоча б один id = 0), порівнюються всі поля.
        // Тут book2.id=1 (не 0), newBookWithZeroId.id=0. Отже, порівнюються всі поля.
        // Оскільки поля book2 та newBookWithZeroId відрізняються (наприклад, коментар),
        // equals має повернути false.
        assertFalse(book2.equals(newBookWithZeroId), "Книги мають бути нерівними, якщо одна має id=0, інша id!=0, і поля відрізняються.");
        assertFalse(newBookWithZeroId.equals(book2), "Метод equals має бути симетричним.");
    }


    /**
     * Тестує метод {@link Book#equals(Object)} для нових книг (з id = 0)
     * при зміні різних полів.
     * Коли id обох об'єктів дорівнює 0, порівняння має відбуватися за всіма полями.
     */
    @Test
    void testEquals_newBooks_differentFields() {
        // Створюємо дві книги з id = 0 та однаковими початковими даними
        Book b1 = new Book("Title", "Author", "Genre", ReadingStatus.READ, "path/to/cover1.jpg");
        b1.setDateAdded(TODAY);
        b1.setDateRead(TODAY);
        b1.setRating(5);
        b1.setComment("Original Comment");
        b1.setFavorite(true);

        Book b2 = new Book("Title", "Author", "Genre", ReadingStatus.READ, "path/to/cover1.jpg");
        b2.setDateAdded(TODAY);
        b2.setDateRead(TODAY);
        b2.setRating(5);
        b2.setComment("Original Comment");
        b2.setFavorite(true);

        // Переконуємося, що вони рівні (id=0 в обох, всі поля однакові)
        assertTrue(b1.equals(b2), "Книги з id=0 та однаковими полями мають бути рівними.");
        assertTrue(b2.equals(b1), "Метод equals має бути симетричним.");


        // 1. Перевірка поля title
        b2.setTitle("Different Title");
        assertFalse(b1.equals(b2), "Має бути false, якщо назви відрізняються.");
        b2.setTitle("Title"); // Скидання значення

        b1.setTitle(null); // b1.title = null, b2.title = "Title"
        assertFalse(b1.equals(b2), "Має бути false, якщо одна назва null, а інша ні.");
        b2.setTitle(null); // b1.title = null, b2.title = null
        assertTrue(b1.equals(b2), "Має бути true, якщо обидві назви null.");
        b1.setTitle("Title"); // Скидання значення
        b2.setTitle("Title"); // Скидання значення

        // 2. Перевірка поля author
        b2.setAuthor("Different Author");
        assertFalse(b1.equals(b2), "Має бути false, якщо автори відрізняються.");
        b2.setAuthor("Author"); // Скидання значення

        b1.setAuthor(null); // b1.author = null, b2.author = "Author"
        assertFalse(b1.equals(b2), "Має бути false, якщо один автор null, а інший ні.");
        b2.setAuthor(null); // b1.author = null, b2.author = null
        assertTrue(b1.equals(b2), "Має бути true, якщо обидва автори null.");
        b1.setAuthor("Author"); // Скидання значення
        b2.setAuthor("Author"); // Скидання значення

        // 3. Перевірка поля genre
        b2.setGenre("Different Genre");
        assertFalse(b1.equals(b2), "Має бути false, якщо жанри відрізняються.");
        b2.setGenre("Genre"); // Скидання значення

        b1.setGenre(null);
        assertFalse(b1.equals(b2), "Має бути false, якщо один жанр null.");
        b2.setGenre(null);
        assertTrue(b1.equals(b2), "Має бути true, якщо обидва жанри null.");
        b1.setGenre("Genre"); // Скидання значення
        b2.setGenre("Genre"); // Скидання значення

        // 4. Перевірка поля status
        b2.setStatus(ReadingStatus.WANT_TO_READ);
        assertFalse(b1.equals(b2), "Має бути false, якщо статуси відрізняються.");
        b2.setStatus(ReadingStatus.READ); // Скидання значення
        // status не може бути null згідно з логікою класу (ініціалізується в конструкторі)

        // 5. Перевірка поля dateAdded
        b2.setDateAdded(YESTERDAY);
        assertFalse(b1.equals(b2), "Має бути false, якщо dateAdded відрізняються.");
        b2.setDateAdded(TODAY); // Скидання значення
        // dateAdded не може бути null згідно з логікою класу (ініціалізується в конструкторі)

        // 6. Перевірка поля dateRead (може бути null)
        b2.setDateRead(YESTERDAY);
        assertFalse(b1.equals(b2), "Має бути false, якщо dateRead відрізняються.");
        b2.setDateRead(TODAY); // Скидання значення

        b1.setDateRead(null); // b1.dateRead = null, b2.dateRead = TODAY
        assertFalse(b1.equals(b2), "Має бути false, якщо одна dateRead null, а інша ні.");
        b2.setDateRead(null); // b1.dateRead = null, b2.dateRead = null
        assertTrue(b1.equals(b2), "Має бути true, якщо обидві dateRead null.");
        b1.setDateRead(TODAY); // Скидання значення
        b2.setDateRead(TODAY); // Скидання значення

        // 7. Перевірка поля rating
        b2.setRating(3);
        assertFalse(b1.equals(b2), "Має бути false, якщо рейтинги відрізняються.");
        b2.setRating(5); // Скидання значення

        // 8. Перевірка поля comment (може бути null)
        b2.setComment("Different Comment");
        assertFalse(b1.equals(b2), "Має бути false, якщо коментарі відрізняються.");
        b2.setComment("Original Comment"); // Скидання значення

        b1.setComment(null); // b1.comment = null, b2.comment = "Original Comment"
        assertFalse(b1.equals(b2), "Має бути false, якщо один коментар null, а інший ні.");
        b2.setComment(null); // b1.comment = null, b2.comment = null
        assertTrue(b1.equals(b2), "Має бути true, якщо обидва коментарі null.");
        b1.setComment("Original Comment"); // Скидання значення
        b2.setComment("Original Comment"); // Скидання значення

        // 9. Перевірка поля coverImagePath (може бути null)
        b2.setCoverImagePath("path/to/cover2.jpg");
        assertFalse(b1.equals(b2), "Має бути false, якщо coverImagePaths відрізняються.");
        b2.setCoverImagePath("path/to/cover1.jpg"); // Скидання значення

        b1.setCoverImagePath(null); // b1.coverImagePath = null, b2.coverImagePath = "path/to/cover1.jpg"
        assertFalse(b1.equals(b2), "Має бути false, якщо один coverImagePath null, а інший ні.");
        b2.setCoverImagePath(null); // b1.coverImagePath = null, b2.coverImagePath = null
        assertTrue(b1.equals(b2), "Має бути true, якщо обидва coverImagePaths null.");
        b1.setCoverImagePath("path/to/cover1.jpg"); // Скидання значення
        b2.setCoverImagePath("path/to/cover1.jpg"); // Скидання значення

        // 10. Перевірка поля favorite
        b2.setFavorite(false);
        assertFalse(b1.equals(b2), "Має бути false, якщо прапорці favorite відрізняються.");
        b2.setFavorite(true); // Скидання значення
    }

    /**
     * Тестує рефлексивність методу {@link Book#equals(Object)}.
     * Об'єкт має бути рівним самому собі.
     */
    @Test
    void testEquals_reflexive() {
        assertTrue(book2.equals(book2), "Книга з id=1 має бути рівна сама собі."); // Книга з id=1
        Book newBook = new Book();
        assertTrue(newBook.equals(newBook), "Книга з id=0 має бути рівна сама собі."); // Книга з id=0
    }

    /**
     * Тестує метод {@link Book#equals(Object)} при порівнянні з null.
     * Результат повинен бути false.
     */
    @Test
    void testEquals_null() {
        assertFalse(book2.equals(null), "Порівняння з null має повертати false.");
    }

    /**
     * Тестує метод {@link Book#equals(Object)} при порівнянні з об'єктом іншого класу.
     * Результат повинен бути false.
     */
    @Test
    void testEquals_differentClass() {
        assertFalse(book2.equals("Not a book"), "Порівняння з об'єктом іншого класу має повертати false.");
    }

    /**
     * Тестує консистентність методу {@link Book#hashCode()}.
     * Хеш-код має бути однаковим при повторних викликах для того самого незміненого об'єкта.
     */
    @Test
    void testHashCode_consistency() {
        // Хеш-код має бути однаковим при повторних викликах для того самого об'єкта
        assertEquals(book2.hashCode(), book2.hashCode(), "Хеш-код має бути консистентним.");
    }

    /**
     * Тестує метод {@link Book#hashCode()} для об'єктів, які є рівними згідно з equals().
     * Якщо два об'єкти рівні, їх хеш-коди також повинні бути рівними.
     */
    @Test
    void testHashCode_equalsObjects() {
        Book book3 = new Book(1L, "Інша книга", "Інший автор", "Інший жанр",
                ReadingStatus.WANT_TO_READ, TODAY, null, 0, null, null, false);
        // book2 та book3 мають однаковий id=1L, тому їх equals() повертає true,
        // отже, їх хеш-коди мають бути однаковими.
        assertEquals(book2.hashCode(), book3.hashCode(), "Рівні об'єкти (за ненульовим ID) повинні мати однаковий хеш-код.");
    }

    /**
     * Тестує метод {@link Book#hashCode()} для об'єктів з різними ненульовими ID.
     * Хеш-коди, як правило, повинні відрізнятися (хоча колізії можливі, але малоймовірні для простих ID).
     */
    @Test
    void testHashCode_differentIds() {
        Book bookWithDifferentId = new Book(2L, "Кобзар", "Тарас Шевченко", "Поезія",
                ReadingStatus.READ, TODAY, TODAY, 5, "Чудова книга",
                "covers/kobzar.jpg", true);
        // book2 має id=1, bookWithDifferentId має id=2.
        // Якщо id не нульові та різні, хеш-коди мають відрізнятися.
        assertNotEquals(book2.hashCode(), bookWithDifferentId.hashCode(), "Об'єкти з різними ненульовими ID, як правило, повинні мати різні хеш-коди.");
    }

    /**
     * Тестує метод {@link Book#hashCode()} для нових об'єктів (id=0) з однаковими полями.
     * Якщо id=0, хеш-код обчислюється на основі полів. Рівні об'єкти повинні мати однаковий хеш-код.
     */
    @Test
    void testHashCode_newObjects_sameFields() {
        Book b1 = new Book("Title", "Author", "Genre", ReadingStatus.READ, "path");
        b1.setDateAdded(TODAY);
        b1.setDateRead(TODAY); // Встановлюємо всі поля, що впливають на хеш-код, якщо id=0
        b1.setRating(5);
        b1.setComment("Comment");
        b1.setFavorite(true);


        Book b2 = new Book("Title", "Author", "Genre", ReadingStatus.READ, "path");
        b2.setDateAdded(TODAY);
        b2.setDateRead(TODAY);
        b2.setRating(5);
        b2.setComment("Comment");
        b2.setFavorite(true);

        // Якщо id=0, хеш-код залежить від усіх полів.
        // Оскільки b1.equals(b2) є true, їх хеш-коди мають бути однаковими.
        assertEquals(b1.hashCode(), b2.hashCode(), "Нові об'єкти (id=0) з однаковими полями повинні мати однаковий хеш-код.");
    }

    /**
     * Тестує метод {@link Book#hashCode()} для нових об'єктів (id=0) з різними полями.
     * Якщо id=0, хеш-код обчислюється на основі полів. Нерівні об'єкти, як правило, повинні мати різні хеш-коди.
     */
    @Test
    void testHashCode_newObjects_differentFields() {
        Book b1 = new Book("Title", "Author", "Genre", ReadingStatus.READ, "path");
        b1.setDateAdded(TODAY);
        b1.setDateRead(TODAY);
        b1.setRating(5); // Рейтинг 5

        Book b2 = new Book("Title", "Author", "Genre", ReadingStatus.READ, "path");
        b2.setDateAdded(TODAY);
        b2.setDateRead(TODAY);
        b2.setRating(4); // Інший рейтинг (4)

        // Якщо id=0, хеш-код залежить від усіх полів.
        // Оскільки b1.equals(b2) є false (через різний рейтинг), їх хеш-коди, як правило, відрізняються.
        assertNotEquals(b1.hashCode(), b2.hashCode(), "Нові об'єкти (id=0) з різними полями, як правило, повинні мати різні хеш-коди.");
    }


    /**
     * Тестує метод {@link Book#toString()} для різних станів об'єкта.
     * Перевіряє, що рядкове представлення містить очікувані значення полів.
     */
    @Test
    void testToString() {
        // book2 - це об'єкт, створений у setUp() з усіма заповненими полями
        // Формуємо очікувані рядки для полів, які можуть бути null або потребують лапок
        String book2Title = (book2.getTitle() != null ? "'" + book2.getTitle() + "'" : "null");
        String book2Author = (book2.getAuthor() != null ? "'" + book2.getAuthor() + "'" : "null");
        String book2Genre = (book2.getGenre() != null ? "'" + book2.getGenre() + "'" : "null");
        String book2Comment = (book2.getComment() != null ? "'" + book2.getComment() + "'" : "null");
        String book2Cover = (book2.getCoverImagePath() != null ? "'" + book2.getCoverImagePath() + "'" : "null");


        String bookString = book2.toString();

        // Для книги з переважно null полями (окрім тих, що ініціалізуються за замовчуванням)
        Book bookWithNulls = new Book(); // Створюємо книгу конструктором за замовчуванням
        bookWithNulls.setTitle("Test Title"); // Встановлюємо назву
        // Інші поля залишаються null або зі значеннями за замовчуванням

        // Важливо: конструктор Book() може не встановлювати dateAdded та status.
        // Для тесту toString, якщо ці поля null, вони повинні відображатися як "null".
        // Якщо конструктор за замовчуванням їх ініціалізує (наприклад, dateAdded = LocalDate.now()),
        // то в асертах потрібно це врахувати.
        // Поточна реалізація Book (простий конструктор) встановлює dateAdded і status.
        // Припускаємо, що конструктор за замовчуванням залишає їх null або встановлює значення за замовчуванням.
        // Якщо dateAdded ініціалізується за замовчуванням, то це потрібно врахувати:
        // LocalDate expectedDateAddedForNulls = (bookWithNulls.getDateAdded() == null) ? null : bookWithNulls.getDateAdded();
        // ReadingStatus expectedStatusForNulls = (bookWithNulls.getStatus() == null) ? null : bookWithNulls.getStatus();

        String nullString = bookWithNulls.toString();


        assertAll("Тест toString для book2 (поля не null)",
                () -> assertNotNull(bookString, "toString() не повинен повертати null."),
                () -> assertTrue(bookString.contains("id=" + book2.getId()), "Рядок повинен містити ID."),
                () -> assertTrue(bookString.contains("title=" + book2Title), "Рядок повинен містити назву."),
                () -> assertTrue(bookString.contains("author=" + book2Author), "Рядок повинен містити автора."),
                () -> assertTrue(bookString.contains("genre=" + book2Genre), "Рядок повинен містити жанр."),
                () -> assertTrue(bookString.contains("status=" + book2.getStatus()), "Рядок повинен містити статус."),
                () -> assertTrue(bookString.contains("dateAdded=" + book2.getDateAdded()), "Рядок повинен містити дату додавання."),
                () -> assertTrue(bookString.contains("dateRead=" + book2.getDateRead()), "Рядок повинен містити дату прочитання."),
                () -> assertTrue(bookString.contains("rating=" + book2.getRating()), "Рядок повинен містити рейтинг."),
                () -> assertTrue(bookString.contains("comment=" + book2Comment), "Рядок повинен містити коментар."),
                () -> assertTrue(bookString.contains("coverImagePath=" + book2Cover), "Рядок повинен містити шлях до обкладинки."),
                () -> assertTrue(bookString.contains("favorite=" + book2.isFavorite()), "Рядок повинен містити статус 'улюблене'.")
        );

        // Очікуваний вивід для bookWithNulls залежить від реалізації конструктора за замовчуванням
        // та методу toString для null значень.
        assertAll("Тест toString для bookWithNulls (переважно null поля)",
                () -> assertTrue(nullString.contains("id=0"), "Рядок повинен містити id=0."),
                () -> assertTrue(nullString.contains("title='Test Title'"), "Рядок повинен містити встановлену назву."),
                () -> assertTrue(nullString.contains("author=null"), "Рядок повинен містити 'author=null'."),
                () -> assertTrue(nullString.contains("genre=null"), "Рядок повинен містити 'genre=null'."),
                // Залежить від того, чи ініціалізує конструктор за замовчуванням status
                () -> assertTrue(nullString.contains("status=" + bookWithNulls.getStatus()), "Рядок повинен відображати поточний статус (можливо, null)."),
                // Залежить від того, чи ініціалізує конструктор за замовчуванням dateAdded
                () -> assertTrue(nullString.contains("dateAdded=" + bookWithNulls.getDateAdded()), "Рядок повинен відображати поточну дату додавання (можливо, null)."),
                () -> assertTrue(nullString.contains("dateRead=null"), "Рядок повинен містити 'dateRead=null'."),
                () -> assertTrue(nullString.contains("rating=0"), "Рядок повинен містити 'rating=0'."),
                () -> assertTrue(nullString.contains("comment=null"), "Рядок повинен містити 'comment=null'."),
                () -> assertTrue(nullString.contains("coverImagePath=" + (bookWithNulls.getCoverImagePath() == null ? "null" : "'" + bookWithNulls.getCoverImagePath() + "'")), "Рядок повинен коректно відображати шлях до обкладинки (можливо, null)."),
                () -> assertTrue(nullString.contains("favorite=false"), "Рядок повинен містити 'favorite=false'.")
        );
    }
}