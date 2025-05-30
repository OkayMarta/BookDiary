package com.student.bookdiary.persistence;

/**
 * Загальний виняток для шару доступу до даних (DAO).
 * Вказує на помилку, що сталася під час операцій із постійним сховищем даних
 * (наприклад, базою даних).
 * Цей клас є неперевірюваним винятком (успадковується від RuntimeException).
 */
public class DataAccessException extends RuntimeException {

    /**
     * Конструктор, що створює новий {@code DataAccessException} з вказаним повідомленням про помилку.
     *
     * @param message Детальне повідомлення про помилку.
     */
    public DataAccessException(String message) {
        super(message);
    }

    /**
     * Конструктор, що створює новий {@code DataAccessException} з вказаним повідомленням про помилку
     * та оригінальною причиною (іншим винятком, що спричинив цю помилку).
     * Це корисно для збереження стеку викликів оригінальної помилки.
     *
     * @param message Детальне повідомлення про помилку.
     * @param cause Оригінальний виняток (причина помилки).
     */
    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}