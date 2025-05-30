package com.student.bookdiary.persistence;

import com.student.bookdiary.model.Goal;
import com.student.bookdiary.model.GoalType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Клас {@code SqliteGoalDao} є реалізацією інтерфейсу {@link GoalDao}
 * для роботи з базою даних SQLite. Він відповідає за виконання операцій
 * CRUD (Create, Read, Update, Delete) та інших запитів до таблиці цілей.
 */
public class SqliteGoalDao implements GoalDao {

    private static final Logger log = LoggerFactory.getLogger(SqliteGoalDao.class);

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
     * Додає нову ціль до бази даних.
     * Встановлює згенерований ID для об'єкта цілі після успішного додавання.
     *
     * @param goal Об'єкт {@link Goal} для додавання.
     * @throws DataAccessException Якщо виникає помилка SQL під час виконання операції.
     */
    @Override
    public void addGoal(Goal goal) {
        log.debug("Спроба додати нову ціль. Опис: '{}', тип: {}, значення: {}, рік: {}, місяць: {}",
                goal.getDescription(), goal.getType(), goal.getTargetValue(), goal.getYear(), goal.getMonth());
        String sql = "INSERT INTO goals(description, type, targetValue, year, month, dateAdded) VALUES(?,?,?,?,?,?)";

        executeWithConnection(conn -> {
            try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                setGoalParameters(pstmt, goal); // Встановлення параметрів за допомогою допоміжного методу
                int affectedRows = pstmt.executeUpdate();

                if (affectedRows == 0) {
                    String errorMessage = String.format("Додавання цілі '%s' не змінило жодного рядка в БД.", goal.getDescription());
                    log.error(errorMessage);
                    throw new DataAccessException(errorMessage);
                }

                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        goal.setId(generatedKeys.getLong(1));
                        log.info("Ціль '{}' успішно додано до БД з ID={}", goal.getDescription(), goal.getId());
                    } else {
                        String errorMessage = String.format("Не вдалося отримати згенерований ID для цілі '%s' після додавання.", goal.getDescription());
                        log.error(errorMessage);
                        throw new DataAccessException(errorMessage);
                    }
                }
            }
            return null;
        });
    }

    /**
     * Оновлює дані існуючої цілі в базі даних.
     *
     * @param goal Об'єкт {@link Goal} з оновленими даними та існуючим ID.
     * @throws DataAccessException Якщо виникає помилка SQL під час виконання операції.
     */
    @Override
    public void updateGoal(Goal goal) {
        log.debug("Спроба оновити ціль з ID={}. Новий опис: '{}'", goal.getId(), goal.getDescription());
        String sql = "UPDATE goals SET description = ?, type = ?, targetValue = ?, year = ?, month = ?, dateAdded = ? WHERE id = ?";

        executeWithConnection(conn -> {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                setGoalParameters(pstmt, goal); // Встановлення основних параметрів цілі
                pstmt.setLong(7, goal.getId()); // Встановлення ID для умови WHERE
                int affectedRows = pstmt.executeUpdate();
                if (affectedRows > 0) {
                    log.info("Ціль з ID={} успішно оновлено в БД. Опис: '{}'", goal.getId(), goal.getDescription());
                } else {
                    String errorMessage = String.format("Ціль з ID=%d не знайдена в БД", goal.getId());
                    log.error(errorMessage);
                    throw new DataAccessException(errorMessage);
                }
            }
            return null;
        });
    }

    /**
     * Видаляє ціль з бази даних за її ID.
     *
     * @param goalId ID цілі для видалення.
     * @throws DataAccessException Якщо виникає помилка SQL під час виконання операції.
     */
    @Override
    public void deleteGoal(long goalId) {
        log.debug("Спроба видалити ціль з ID={}", goalId);
        String sql = "DELETE FROM goals WHERE id = ?";

        executeWithConnection(conn -> {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setLong(1, goalId);
                int affectedRows = pstmt.executeUpdate();

                if (affectedRows > 0) {
                    log.info("Ціль з ID={} успішно видалено з БД.", goalId);
                } else {
                    log.warn("Видалення цілі з ID={} не змінило жодного рядка. Можливо, ціль з таким ID не знайдена.", goalId);
                }
            }
            return null;
        });
    }

    /**
     * Знаходить ціль в базі даних за її ID.
     *
     * @param goalId ID цілі для пошуку.
     * @return {@link Optional} з об'єктом {@link Goal}, якщо знайдено, або порожній Optional.
     * @throws DataAccessException Якщо виникає помилка SQL під час виконання операції.
     */
    @Override
    public Optional<Goal> getGoalById(long goalId) {
        log.debug("Спроба знайти ціль за ID={}", goalId);
        String sql = "SELECT * FROM goals WHERE id = ?";
        final Holder<Optional<Goal>> goalHolder = new Holder<>(Optional.empty());

        executeWithConnection(conn -> {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setLong(1, goalId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        goalHolder.value = Optional.of(mapResultSetToGoal(rs));
                        log.debug("Ціль з ID={} знайдено: '{}'", goalId, goalHolder.value.get().getDescription());
                    } else {
                        log.debug("Ціль з ID={} не знайдено.", goalId);
                    }
                }
            }
            return null;
        });
        return goalHolder.value;
    }

    /**
     * Повертає список всіх цілей з бази даних, відсортованих за датою додавання у зворотному порядку.
     *
     * @return {@link List} об'єктів {@link Goal}.
     * @throws DataAccessException Якщо виникає помилка SQL під час виконання операції.
     */
    @Override
    public List<Goal> getAllGoals() {
        log.debug("Спроба отримати список всіх цілей.");
        String sql = "SELECT * FROM goals ORDER BY dateAdded DESC";
        final List<Goal> goals = new ArrayList<>();

        executeWithConnection(conn -> {
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    goals.add(mapResultSetToGoal(rs));
                }
                log.debug("Отримано {} цілей(і) з БД.", goals.size());
            }
            return null;
        });
        return goals;
    }

    /**
     * Допоміжний приватний метод для перетворення поточного рядка {@link ResultSet} на об'єкт {@link Goal}.
     * Інкапсулює логіку мапінгу даних з ResultSet на поля об'єкта Goal.
     *
     * @param rs {@link ResultSet}, курсор якого встановлено на рядок з даними цілі.
     * @return Створений об'єкт {@link Goal}.
     * @throws SQLException Якщо виникає помилка доступу до даних у {@link ResultSet}.
     */
    private Goal mapResultSetToGoal(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        String description = rs.getString("description");
        GoalType type = GoalType.valueOf(rs.getString("type"));
        int targetValue = rs.getInt("targetValue");

        // Отримання року; якщо значення в БД було NULL, getInt поверне 0,
        // а rs.wasNull() поверне true.
        Integer year = rs.getInt("year");
        if (rs.wasNull()) {
            year = null;
        }

        // Аналогічно для місяця.
        Integer month = rs.getInt("month");
        if (rs.wasNull()) {
            month = null;
        }

        LocalDate dateAdded = LocalDate.parse(rs.getString("dateAdded"));

        return new Goal(id, description, type, targetValue, year, month, dateAdded);
    }

    /**
     * Допоміжний приватний метод для встановлення параметрів {@link PreparedStatement} з полів об'єкта {@link Goal}.
     * Використовується для уникнення дублювання коду в методах {@code addGoal} та {@code updateGoal}.
     *
     * @param pstmt Об'єкт {@link PreparedStatement}, для якого встановлюються параметри.
     * @param goal Об'єкт {@link Goal}, з якого беруться значення.
     * @throws SQLException Якщо виникає помилка під час встановлення параметрів.
     */
    private void setGoalParameters(PreparedStatement pstmt, Goal goal) throws SQLException {
        // Валідація обов'язкових полів
        if (goal.getType() == null) {
            throw new DataAccessException("Тип цілі не може бути null");
        }
        if (goal.getDateAdded() == null) {
            throw new DataAccessException("Дата додавання не може бути null");
        }
        
        pstmt.setString(1, goal.getDescription());
        pstmt.setString(2, goal.getType().name());
        pstmt.setInt(3, goal.getTargetValue());

        if (goal.getYear() != null) {
            pstmt.setInt(4, goal.getYear());
        } else {
            pstmt.setNull(4, Types.INTEGER);
        }

        if (goal.getMonth() != null) {
            pstmt.setInt(5, goal.getMonth());
        } else {
            pstmt.setNull(5, Types.INTEGER);
        }
        pstmt.setString(6, goal.getDateAdded().toString());
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