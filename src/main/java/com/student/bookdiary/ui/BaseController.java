package com.student.bookdiary.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Абстрактний базовий клас для всіх контролерів подань (view) у додатку.
 * Надає загальну функціональність, таку як зберігання посилання на головний контролер
 * {@link PrimaryController} та визначення абстрактного методу {@link #refreshData()}
 * для оновлення даних у конкретному поданні.
 */
public abstract class BaseController {
    private static final Logger log = LoggerFactory.getLogger(BaseController.class);

    /**
     * Посилання на головний контролер програми ({@link PrimaryController}).
     * Це посилання дозволяє дочірнім контролерам взаємодіяти з головним вікном,
     * викликати діалоги, переходити між поданнями тощо.
     */
    protected PrimaryController primaryController;

    /**
     * Встановлює посилання на головний контролер програми {@link PrimaryController}.
     * Цей метод зазвичай викликається головним контролером під час завантаження
     * або перемикання на подання, кероване цим контролером.
     *
     * @param controller Головний контролер програми.
     */
    public void setPrimaryController(PrimaryController controller) {
        log.debug("Встановлення PrimaryController ({}) для контролера {}",
                (controller != null ? controller.getClass().getSimpleName() : "null"),
                this.getClass().getSimpleName());
        this.primaryController = controller;
    }

    /**
     * Абстрактний метод для оновлення даних у поданні, керованому цим контролером.
     * Кожен конкретний контролер (нащадок {@code BaseController}) повинен реалізувати
     * цей метод для завантаження або оновлення даних, що відображаються користувачеві.
     * Цей метод типово викликається при першому показі подання або коли дані
     * в ньому потребують оновлення (наприклад, після додавання, редагування або
     * видалення елементів).
     */
    public abstract void refreshData();
}