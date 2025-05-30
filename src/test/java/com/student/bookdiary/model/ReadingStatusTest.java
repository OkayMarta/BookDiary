package com.student.bookdiary.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Тестовий клас для перелічення {@link ReadingStatus}.
 */
class ReadingStatusTest {

    /**
     * Тестує метод {@link ReadingStatus#getDisplayName()}.
     * Перевіряє, чи повертаються коректні відображувані назви для кожного статусу читання.
     */
    @Test
    void testGetDisplayName() {
        assertAll(
                () -> assertEquals("Прочитана", ReadingStatus.READ.getDisplayName()),
                () -> assertEquals("Хочу прочитати", ReadingStatus.WANT_TO_READ.getDisplayName())
        );
    }

    /**
     * Тестує метод {@link ReadingStatus#toString()}.
     * Перевіряє, чи метод toString() повертає відображувану назву,
     * що збігається з результатом getDisplayName().
     */
    @Test
    void testToString() {
        assertAll(
                () -> assertEquals("Прочитана", ReadingStatus.READ.toString()),
                () -> assertEquals("Хочу прочитати", ReadingStatus.WANT_TO_READ.toString())
        );
    }

    /**
     * Тестує метод {@link ReadingStatus#valueOf(String)}.
     * Перевіряє, чи метод valueOf() коректно перетворює рядкове представлення
     * константи перелічення на відповідний об'єкт ReadingStatus.
     * Також перевіряє, чи генерується виняток IllegalArgumentException для неіснуючого статусу.
     */
    @Test
    void testValueOf() {
        assertAll(
                () -> assertEquals(ReadingStatus.READ, ReadingStatus.valueOf("READ")),
                () -> assertEquals(ReadingStatus.WANT_TO_READ, ReadingStatus.valueOf("WANT_TO_READ")),
                // Перевірка, що для неіснуючого значення генерується виняток
                () -> assertThrows(IllegalArgumentException.class,
                        () -> ReadingStatus.valueOf("INVALID_STATUS"))
        );
    }
}