package com.student.bookdiary.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Клас, що представляє книгу в читацькому щоденнику.
 * Містить інформацію про назву, автора, жанр, статус читання,
 * дати, оцінку, коментар та обкладинку книги.
 */
public class Book {

    // --- Поля класу ---

    /**
     * Унікальний ідентифікатор книги з бази даних.
     * Значення 0 вказує, що книга ще не була збережена в БД.
     */
    private long id;

    /**
     * Назва книги.
     */
    private String title;

    /**
     * Автор книги.
     */
    private String author;

    /**
     * Жанр книги.
     */
    private String genre;

    /**
     * Статус читання книги (наприклад, ПРОЧИТАНА, ХОЧУ ПРОЧИТАТИ).
     * Використовує перелік {@link ReadingStatus}.
     */
    private ReadingStatus status;

    /**
     * Дата додавання книги до щоденника.
     */
    private LocalDate dateAdded;

    /**
     * Дата прочитання книги.
     * Має значення null, якщо книга ще не прочитана (статус WANT_TO_READ).
     */
    private LocalDate dateRead;

    /**
     * Оцінка книги за шкалою від 1 до 5.
     * Значення 0 вказує, що книга не оцінена або ще не прочитана.
     */
    private int rating;

    /**
     * Коментар користувача до книги.
     * Може бути null, якщо коментар відсутній.
     */
    private String comment;

    /**
     * Шлях до файлу зображення обкладинки книги.
     * Може бути null, якщо обкладинка відсутня.
     */
    private String coverImagePath;

    /**
     * Прапорець, що вказує, чи є книга улюбленою.
     * true - улюблена, false - ні.
     */
    private boolean favorite;

    // --- Конструктори ---

    /**
     * Порожній конструктор.
     * Дозволяє створювати об'єкт Book без початкової ініціалізації полів.
     * Може використовуватися бібліотеками для десеріалізації або для ручного заповнення полів.
     */
    public Book() {
    }

    /**
     * Конструктор для створення нового запису про книгу перед збереженням у базу даних.
     * Автоматично встановлює поточну дату додавання книги.
     * Інші поля, такі як дата прочитання, оцінка, коментар та статус "улюблене",
     * ініціалізуються значеннями за замовчуванням.
     * Поле `id` буде встановлено після збереження книги в базу даних.
     *
     * @param title Назва книги.
     * @param author Автор книги.
     * @param genre Жанр книги.
     * @param status Початковий статус читання книги.
     * @param coverImagePath Шлях до файлу обкладинки (може бути null).
     */
    public Book(String title, String author, String genre, ReadingStatus status, String coverImagePath) {
        this.title = title;
        this.author = author;
        this.genre = genre;
        this.status = status;
        this.coverImagePath = coverImagePath;
        this.dateAdded = LocalDate.now(); // Автоматичне встановлення поточної дати додавання
        this.dateRead = null;
        this.rating = 0;
        this.comment = null;
        this.favorite = false;
    }


    /**
     * Повний конструктор для створення об'єкта Book з усіма полями.
     * Зазвичай використовується для завантаження існуючого запису про книгу з бази даних.
     *
     * @param id Унікальний ідентифікатор книги з БД.
     * @param title Назва книги.
     * @param author Автор книги.
     * @param genre Жанр книги.
     * @param status Статус читання книги.
     * @param dateAdded Дата додавання книги до щоденника.
     * @param dateRead Дата прочитання книги (може бути null).
     * @param rating Оцінка книги (1-5, або 0).
     * @param comment Коментар до книги (може бути null).
     * @param coverImagePath Шлях до файлу обкладинки (може бути null).
     * @param favorite Статус "Улюблене" (true або false).
     */
    public Book(long id, String title, String author, String genre, ReadingStatus status,
                LocalDate dateAdded, LocalDate dateRead, int rating, String comment,
                String coverImagePath, boolean favorite) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.genre = genre;
        this.status = status;
        this.dateAdded = dateAdded;
        this.dateRead = dateRead;
        this.rating = rating;
        this.comment = comment;
        this.coverImagePath = coverImagePath;
        this.favorite = favorite;
    }

    // --- Гетери ---

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getGenre() {
        return genre;
    }

    public ReadingStatus getStatus() {
        return status;
    }

    public LocalDate getDateAdded() {
        return dateAdded;
    }

    public LocalDate getDateRead() {
        return dateRead;
    }

    public int getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }

    public String getCoverImagePath() {
        return coverImagePath;
    }

    /**
     * Повертає статус "Улюблене" для книги.
     * Для boolean полів прийнято використовувати префікс 'is' у назві гетера.
     *
     * @return true, якщо книга позначена як улюблена, false - в іншому випадку.
     */
    public boolean isFavorite() {
        return favorite;
    }

    // --- Сетери ---

    /**
     * Встановлює унікальний ідентифікатор книги.
     * Зазвичай використовується об'єктами доступу до даних (DAO)
     * після збереження книги в базу даних та отримання згенерованого ID.
     *
     * @param id Новий ідентифікатор книги.
     */
    public void setId(long id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public void setStatus(ReadingStatus status) {
        this.status = status;
    }

    public void setDateAdded(LocalDate dateAdded) {
        this.dateAdded = dateAdded;
    }

    public void setDateRead(LocalDate dateRead) {
        this.dateRead = dateRead;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setCoverImagePath(String coverImagePath) {
        this.coverImagePath = coverImagePath;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    // --- Методи equals, hashCode, toString ---

    /**
     * Порівнює цей об'єкт Book з іншим об'єктом на рівність.
     * Два об'єкти Book вважаються рівними, якщо їхні поля `id` співпадають
     * і не дорівнюють 0 (тобто обидві книги вже збережені в БД і мають унікальні ідентифікатори).
     * Якщо `id` однієї або обох книг дорівнює 0, порівняння відбувається за посиланням (ідентичність об'єктів).
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
        Book book = (Book) o;
        
        // Якщо обидва об'єкти мають id=0, порівнюємо всі поля
        if (id == 0 && book.id == 0) {
            return Objects.equals(title, book.title) &&
                   Objects.equals(author, book.author) &&
                   Objects.equals(genre, book.genre) &&
                   status == book.status &&
                   Objects.equals(dateAdded, book.dateAdded) &&
                   Objects.equals(dateRead, book.dateRead) &&
                   rating == book.rating &&
                   Objects.equals(comment, book.comment) &&
                   Objects.equals(coverImagePath, book.coverImagePath) &&
                   favorite == book.favorite;
        }
        // Інакше порівнюємо за id, якщо вони не нульові
        return id != 0 && id == book.id;
    }

    /**
     * Генерує хеш-код для об'єкта Book.
     * Хеш-код базується на полі `id`, щоб забезпечити узгодженість з методом `equals`.
     * Якщо `id` дорівнює 0, хеш-код буде розрахований стандартним чином на основі посилання на об'єкт.
     *
     * @return Хеш-код об'єкта.
     */
    @Override
    public int hashCode() {
        // Якщо id = 0, використовуємо всі поля для обчислення хеш-коду
        if (id == 0) {
            return Objects.hash(title, author, genre, status, dateAdded, 
                              dateRead, rating, comment, coverImagePath, favorite);
        }
        // Інакше використовуємо тільки id
        return Objects.hash(id);
    }

    /**
     * Повертає рядкове представлення об'єкта Book.
     * Використовується переважно для цілей відлагодження та логування.
     *
     * @return Рядкове представлення, що містить значення всіх полів об'єкта.
     */
    @Override
    public String toString() {
        return "Book{" +
                "id=" + id +
                ", title=" + (title != null ? "'" + title + "'" : "null") +
                ", author=" + (author != null ? "'" + author + "'" : "null") +
                ", genre=" + (genre != null ? "'" + genre + "'" : "null") +
                ", status=" + status +
                ", dateAdded=" + dateAdded +
                ", dateRead=" + dateRead +
                ", rating=" + rating +
                ", comment=" + (comment != null ? "'" + comment + "'" : "null") +
                ", coverImagePath=" + (coverImagePath != null ? "'" + coverImagePath + "'" : "null") +
                ", favorite=" + favorite +
                '}';
    }
}