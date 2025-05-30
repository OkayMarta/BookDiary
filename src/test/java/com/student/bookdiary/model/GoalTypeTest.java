package com.student.bookdiary.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Тестовий клас для перелічення {@link GoalType}.
 */
class GoalTypeTest {

    /**
     * Тестує метод {@link GoalType#getDisplayName()}.
     * Перевіряє, чи повертаються коректні відображувані назви для кожного типу цілі.
     */
    @Test
    void testGetDisplayName() {
        assertAll(
                () -> assertEquals("За місяць", GoalType.MONTHLY.getDisplayName()),
                () -> assertEquals("За рік", GoalType.YEARLY.getDisplayName()),
                () -> assertEquals("Загалом", GoalType.TOTAL.getDisplayName())
        );
    }

    /**
     * Тестує метод {@link GoalType#toString()}.
     * Перевіряє, чи метод toString() повертає відображувану назву,
     * що збігається з результатом getDisplayName().
     */
    @Test
    void testToString() {
        assertAll(
                () -> assertEquals("За місяць", GoalType.MONTHLY.toString()),
                () -> assertEquals("За рік", GoalType.YEARLY.toString()),
                () -> assertEquals("Загалом", GoalType.TOTAL.toString())
        );
    }

    /**
     * Тестує метод {@link GoalType#valueOf(String)}.
     * Перевіряє, чи метод valueOf() коректно перетворює рядкове представлення
     * константи перелічення на відповідний об'єкт GoalType.
     * Також перевіряє, чи генерується виняток IllegalArgumentException для неіснуючого типу.
     */
    @Test
    void testValueOf() {
        assertAll(
                () -> assertEquals(GoalType.MONTHLY, GoalType.valueOf("MONTHLY")),
                () -> assertEquals(GoalType.YEARLY, GoalType.valueOf("YEARLY")),
                () -> assertEquals(GoalType.TOTAL, GoalType.valueOf("TOTAL")),
                // Перевірка, що для неіснуючого значення генерується виняток
                () -> assertThrows(IllegalArgumentException.class,
                        () -> GoalType.valueOf("INVALID_TYPE"))
        );
    }
}