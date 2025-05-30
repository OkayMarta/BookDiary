package com.student.bookdiary.persistence;

import com.student.bookdiary.model.Book;
import com.student.bookdiary.model.ReadingStatus;

import java.util.List;
import java.util.Optional;

/**
 * Інтерфейс Data Access Object (DAO) для операцій з об'єктами {@link Book}.
 * Визначає контракт для взаємодії з даними книг у постійному сховищі (наприклад, базі даних).
 * Кожна реалізація цього інтерфейсу відповідатиме за конкретний механізм зберігання даних.
 */
public interface BookDao {

    /**
     * Додає нову книгу до сховища.
     * Після успішного додавання, реалізація цього методу повинна встановити
     * згенерований унікальний ідентифікатор (ID) для переданого об'єкта {@code book}.
     *
     * @param book Об'єкт книги, який потрібно додати. Поле ID в цьому об'єкті буде оновлено.
     * @throws DataAccessException Якщо виникає помилка під час доступу до сховища даних.
     */
    void addBook(Book book);

    /**
     * Оновлює існуючу книгу в сховищі.
     * Книга для оновлення ідентифікується за її ID.
     *
     * @param book Об'єкт книги з оновленими даними. Повинен мати коректний ID існуючої книги.
     * @throws DataAccessException Якщо виникає помилка під час доступу до сховища даних
     *                             або книга з вказаним ID не знайдена.
     */
    void updateBook(Book book);

    /**
     * Видаляє книгу зі сховища за її унікальним ідентифікатором (ID).
     *
     * @param bookId ID книги, яку потрібно видалити.
     * @throws DataAccessException Якщо виникає помилка під час доступу до сховища даних.
     */
    void deleteBook(long bookId);

    /**
     * Знаходить та повертає книгу зі сховища за її унікальним ідентифікатором (ID).
     *
     * @param bookId ID книги для пошуку.
     * @return Об'єкт {@link Optional}, що містить знайдену книгу,
     *         або порожній {@link Optional}, якщо книга з таким ID не знайдена.
     * @throws DataAccessException Якщо виникає помилка під час доступу до сховища даних.
     */
    Optional<Book> getBookById(long bookId);

    /**
     * Повертає список всіх книг, наявних у сховищі.
     * Список може бути порожнім, якщо у сховищі немає жодної книги.
     *
     * @return {@link List} об'єктів {@link Book}.
     * @throws DataAccessException Якщо виникає помилка під час доступу до сховища даних.
     */
    List<Book> getAllBooks();

    /**
     * Повертає список книг, що відповідають вказаному статусу читання.
     *
     * @param status Статус читання ({@link ReadingStatus#READ} або {@link ReadingStatus#WANT_TO_READ}).
     * @return {@link List} книг з відповідним статусом.
     * @throws DataAccessException Якщо виникає помилка під час доступу до сховища даних.
     */
    List<Book> getBooksByStatus(ReadingStatus status);

    /**
     * Повертає список книг, які позначені користувачем як улюблені.
     *
     * @return {@link List} улюблених книг.
     * @throws DataAccessException Якщо виникає помилка під час доступу до сховища даних.
     */
    List<Book> getFavoriteBooks();

    /**
     * Здійснює пошук книг у сховищі за частковим збігом у назві або імені автора.
     * Пошук зазвичай виконується без урахування регістру символів.
     *
     * @param searchTerm Рядок, що містить ключові слова для пошуку.
     * @return {@link List} знайдених книг, що відповідають критеріям пошуку.
     * @throws DataAccessException Якщо виникає помилка під час доступу до сховища даних.
     */
    List<Book> searchBooks(String searchTerm);

    /**
     * Повертає список всіх унікальних жанрів, які присутні серед книг у сховищі.
     * Цей метод корисний для заповнення фільтрів або випадаючих списків жанрів.
     *
     * @return {@link List} рядків, що представляють унікальні назви жанрів.
     * @throws DataAccessException Якщо виникає помилка під час доступу до сховища даних.
     */
    List<String> getDistinctGenres();

    // --- Методи для отримання статистичних даних ---

    /**
     * Повертає загальну кількість книг у сховищі, які мають статус {@link ReadingStatus#READ} (прочитані).
     *
     * @return Загальна кількість прочитаних книг.
     * @throws DataAccessException Якщо виникає помилка під час доступу до сховища даних.
     */
    int getTotalBooksReadCount();

    /**
     * Повертає кількість книг зі статусом {@link ReadingStatus#READ},
     * дата прочитання яких припадає на вказаний рік.
     *
     * @param year Рік, для якого потрібно підрахувати кількість прочитаних книг.
     * @return Кількість книг, прочитаних у вказаному році.
     * @throws DataAccessException Якщо виникає помилка під час доступу до сховища даних.
     */
    int getBooksReadCountByYear(int year);

    /**
     * Повертає кількість книг зі статусом {@link ReadingStatus#READ},
     * дата прочитання яких припадає на вказаний місяць та рік.
     *
     * @param year Рік.
     * @param month Номер місяця (від 1 до 12).
     * @return Кількість книг, прочитаних у вказаному місяці та році.
     * @throws DataAccessException Якщо виникає помилка під час доступу до сховища даних.
     */
    int getBooksReadCountByMonthAndYear(int year, int month);
}