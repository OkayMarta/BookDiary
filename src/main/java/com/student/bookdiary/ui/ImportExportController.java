package com.student.bookdiary.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ImportExportController {

    private static final Logger log = LoggerFactory.getLogger(ImportExportController.class);

    private static final String DB_FILENAME = "book_diary.db"; // Ім'я файлу бази даних
    private static final String COVERS_DIR_NAME = "covers"; // Ім'я директорії з обкладинками

    @FXML
    private Button exportDataButton; // Кнопка для експорту даних

    @FXML
    private Button importDataButton; // Кнопка для імпорту даних

    @FXML
    private Label statusLabel; // Мітка для відображення статусу операцій

    /**
     * Метод ініціалізації контролера.
     * Встановлює обробники подій для кнопок та початковий текст статусу.
     */
    @FXML
    private void initialize() {
        log.info("Ініціалізація ImportExportController...");
        exportDataButton.setOnAction(event -> handleExportData());
        importDataButton.setOnAction(event -> handleImportData());
        statusLabel.setText("Оберіть дію для імпорту або експорту даних.");
        log.debug("Обробники кнопок імпорту/експорту встановлено.");
    }

    /**
     * Обробляє подію експорту даних.
     * Збирає файл бази даних та папку з обкладинками в ZIP-архів.
     */
    private void handleExportData() {
        log.info("Розпочато процес експорту даних.");
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Зберегти резервну копію");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Резервна копія BookDiary (*.zip)", "*.zip")
        );
        // Формування імені файлу за замовчуванням
        String defaultFileName = "book_diary_backup_" +
                LocalDate.now().format(DateTimeFormatter.ISO_DATE) + ".zip";
        fileChooser.setInitialFileName(defaultFileName);

        Stage stage = getStage(); // Отримання поточного вікна
        if (stage == null) {
            log.error("Не вдалося отримати поточне вікно для FileChooser (експорт).");
            statusLabel.setText("Помилка: Не вдалося відкрити діалог збереження.");
            return;
        }
        File selectedZipFile = fileChooser.showSaveDialog(stage);

        if (selectedZipFile == null) {
            log.info("Експорт скасовано користувачем.");
            statusLabel.setText("Експорт скасовано.");
            return;
        }

        File dbFile = new File(DB_FILENAME);
        File coversDir = new File(COVERS_DIR_NAME);

        if (!dbFile.exists()) {
            log.error("Файл бази даних {} не знайдено! Експорт неможливий.", DB_FILENAME);
            showErrorAlert("Помилка Експорту", "Файл бази даних не знайдено.",
                    "Неможливо створити резервну копію, оскільки файл " + DB_FILENAME + " відсутній.");
            statusLabel.setText("Помилка: Файл бази даних не знайдено.");
            return;
        }

        try (FileOutputStream fos = new FileOutputStream(selectedZipFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            log.info("Додавання файлу {} до архіву...", dbFile.getName());
            addFileToZip(dbFile, dbFile.getName(), zos);

            if (coversDir.exists() && coversDir.isDirectory()) {
                log.info("Додавання вмісту папки {} до архіву...", coversDir.getName());
                addDirectoryToZip(coversDir, coversDir.getName(), zos);
            } else {
                log.warn("Папка {} не знайдена, обкладинки не будуть експортовані.", COVERS_DIR_NAME);
            }

            log.info("Експорт даних успішно завершено: {}", selectedZipFile.getAbsolutePath());
            statusLabel.setText("Експорт успішно завершено: " + selectedZipFile.getName());
            showInfoAlert("Експорт Завершено", "Резервну копію успішно створено!",
                    "Файл збережено як: " + selectedZipFile.getAbsolutePath());

        } catch (IOException e) {
            log.error("Помилка під час експорту даних у файл {}", selectedZipFile.getAbsolutePath(), e);
            statusLabel.setText("Помилка експорту: " + e.getMessage());
            showErrorAlert("Помилка Експорту", "Не вдалося створити резервну копію.",
                    "Деталі помилки: " + e.getMessage());
        }
    }

    /**
     * Обробляє подію імпорту даних.
     * Показує попередження, розпаковує обраний ZIP-архів та замінює поточні дані.
     */
    private void handleImportData() {
        log.info("Розпочато процес імпорту даних.");
        Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationAlert.setTitle("Підтвердження імпорту");
        confirmationAlert.setHeaderText("УВАГА: Заміна даних!");
        confirmationAlert.setContentText("Поточні дані (база даних та обкладинки) будуть повністю замінені даними з обраної резервної копії. " +
                "Цю дію неможливо буде скасувати.\n\nВи впевнені, що хочете продовжити?");
        confirmationAlert.initOwner(getStage()); // Встановлення батьківського вікна для діалогу

        Optional<ButtonType> result = confirmationAlert.showAndWait();

        if (result.isEmpty() || result.get() != ButtonType.OK) {
            log.info("Імпорт скасовано користувачем на етапі підтвердження.");
            statusLabel.setText("Імпорт скасовано.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Обрати файл резервної копії для імпорту");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Резервна копія BookDiary (*.zip)", "*.zip")
        );

        Stage stage = getStage(); // Отримання поточного вікна
        if (stage == null) {
            log.error("Не вдалося отримати поточне вікно для FileChooser (імпорт).");
            statusLabel.setText("Помилка: Не вдалося відкрити діалог вибору файлу.");
            return;
        }
        File selectedZipFile = fileChooser.showOpenDialog(stage);

        if (selectedZipFile == null) {
            log.info("Вибір файлу для імпорту скасовано користувачем.");
            statusLabel.setText("Імпорт скасовано.");
            return;
        }

        File projectDir = new File("."); // Поточна директорія програми
        File tempUnpackDir = new File(projectDir, "temp_import_unpack_" + System.currentTimeMillis());

        try {
            if (!tempUnpackDir.mkdirs()) {
                throw new IOException("Не вдалося створити тимчасову директорію для розпакування: " + tempUnpackDir.getAbsolutePath());
            }
            log.info("Розпочато розпакування архіву {} у тимчасову директорію {}", selectedZipFile.getName(), tempUnpackDir.getAbsolutePath());

            // Розпакування архіву
            unpackZipArchive(selectedZipFile, tempUnpackDir);
            log.info("Архів успішно розпаковано в тимчасову папку.");

            // Перевірка наявності файлу БД в розпакованому архіві
            File importedDbFile = new File(tempUnpackDir, DB_FILENAME);
            if (!importedDbFile.exists()) {
                throw new IOException("Файл бази даних " + DB_FILENAME + " не знайдено в розпакованому архіві.");
            }

            // Заміна файлу бази даних
            Path targetDbPath = Paths.get(DB_FILENAME);
            Path sourceDbPath = importedDbFile.toPath();
            log.warn("Спроба замінити файл БД. Програма може потребувати перезапуску для коректної роботи з новими даними.");
            Files.copy(sourceDbPath, targetDbPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Файл бази даних {} успішно замінено.", DB_FILENAME);

            // Обробка папки з обкладинками
            File importedCoversDir = new File(tempUnpackDir, COVERS_DIR_NAME);
            File targetCoversDir = new File(COVERS_DIR_NAME);
            if (importedCoversDir.exists() && importedCoversDir.isDirectory()) {
                // Видалення старого вмісту папки обкладинок, якщо вона існує
                if (targetCoversDir.exists() && targetCoversDir.isDirectory()) {
                    log.info("Очищення існуючої папки обкладинок: {}", targetCoversDir.getAbsolutePath());
                    deleteDirectoryContents(targetCoversDir);
                } else if (!targetCoversDir.mkdirs()) { // Створення папки, якщо її немає
                    log.warn("Не вдалося створити цільову папку для обкладинок: {}", targetCoversDir.getAbsolutePath());
                }
                // Копіювання нового вмісту папки обкладинок
                log.info("Копіювання обкладинок з {} до {}", importedCoversDir.getAbsolutePath(), targetCoversDir.getAbsolutePath());
                copyDirectoryContents(importedCoversDir, targetCoversDir);
                log.info("Вміст папки {} успішно оновлено.", COVERS_DIR_NAME);
            } else {
                log.info("Папка {} не знайдена в архіві або порожня. Якщо папка {} існувала, її вміст буде очищено.", COVERS_DIR_NAME, targetCoversDir.getName());
                // Якщо папка обкладинок існувала, але в архіві її немає, очистити існуючу папку
                if (targetCoversDir.exists() && targetCoversDir.isDirectory()) {
                    deleteDirectoryContents(targetCoversDir);
                    log.info("Існуюча папка {} була очищена, оскільки в архіві вона відсутня.", targetCoversDir.getName());
                }
            }

            statusLabel.setText("Імпорт успішно завершено. Перезапустіть програму для застосування змін.");
            showInfoAlert("Імпорт Завершено", "Дані успішно імпортовано!",
                    "Будь ласка, перезапустіть програму, щоб зміни вступили в силу.");
            log.info("Імпорт даних успішно завершено. Користувачу рекомендовано перезапустити програму.");

        } catch (IOException e) {
            log.error("Помилка під час імпорту даних з файлу {}", selectedZipFile.getName(), e);
            statusLabel.setText("Помилка імпорту: " + e.getMessage());
            showErrorAlert("Помилка Імпорту", "Не вдалося імпортувати дані.",
                    "Деталі помилки: " + e.getMessage());
        } finally {
            // Видалення тимчасової папки розпакування
            if (tempUnpackDir.exists()) {
                if (deleteDirectory(tempUnpackDir)) {
                    log.info("Тимчасову папку розпакування {} успішно видалено.", tempUnpackDir.getAbsolutePath());
                } else {
                    log.error("Не вдалося повністю видалити тимчасову папку розпакування: {}. Можливо, деякі файли заблоковані.", tempUnpackDir.getAbsolutePath());
                }
            }
        }
    }

    /**
     * Розпаковує ZIP-архів у вказану директорію.
     * @param zipFile файл архіву для розпакування.
     * @param destDir директорія, куди буде розпаковано вміст архіву.
     * @throws IOException якщо виникає помилка в процесі розпакування.
     */
    private void unpackZipArchive(File zipFile, File destDir) throws IOException {
        byte[] buffer = new byte[4096]; // Буфер для читання даних
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                File newFile = new File(destDir, zipEntry.getName());
                // Захист від вразливості Zip Slip
                if (!newFile.getCanonicalPath().startsWith(destDir.getCanonicalPath() + File.separator)) {
                    throw new IOException("Спроба розпакувати файл за межами цільової директорії: " + zipEntry.getName());
                }

                log.debug("Розпакування запису: {}", zipEntry.getName());
                if (zipEntry.isDirectory()) {
                    if (!newFile.isDirectory() && !newFile.mkdirs()) {
                        throw new IOException("Не вдалося створити директорію " + newFile.getAbsolutePath());
                    }
                } else {
                    // Переконатися, що батьківська директорія існує
                    File parent = newFile.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        throw new IOException("Не вдалося створити батьківську директорію " + parent.getAbsolutePath());
                    }
                    // Запис файлу
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
                zipEntry = zis.getNextEntry();
            }
        }
    }


    // --- Допоміжні методи для роботи з ZIP та файлами ---

    /**
     * Додає файл до ZIP-архіву.
     * @param file файл для додавання.
     * @param entryName ім'я запису в архіві.
     * @param zos потік виводу ZIP-архіву.
     * @throws IOException якщо виникає помилка вводу-виводу.
     */
    private void addFileToZip(File file, String entryName, ZipOutputStream zos) throws IOException {
        log.debug("Додавання файлу '{}' як '{}' до ZIP-архіву.", file.getPath(), entryName);
        zos.putNextEntry(new ZipEntry(entryName));
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[4096]; // Буфер для читання
            int length;
            while ((length = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, length);
            }
        }
        zos.closeEntry(); // Закриття поточного запису в архіві
    }

    /**
     * Рекурсивно додає вміст директорії до ZIP-архіву.
     * @param folder директорія для додавання.
     * @param parentEntryName шлях батьківського запису в архіві.
     * @param zos потік виводу ZIP-архіву.
     * @throws IOException якщо виникає помилка вводу-виводу.
     */
    private void addDirectoryToZip(File folder, String parentEntryName, ZipOutputStream zos) throws IOException {
        File[] files = folder.listFiles();
        if (files == null) {
            log.warn("Неможливо прочитати вміст папки: {} (можливо, немає прав доступу або папка порожня).", folder.getAbsolutePath());
            return;
        }
        for (File file : files) {
            String entryName = parentEntryName + "/" + file.getName(); // Формування повного шляху запису в архіві
            log.debug("Обробка {} для додавання в архів як {}", file.getPath(), entryName);
            if (file.isDirectory()) {
                addDirectoryToZip(file, entryName, zos); // Рекурсивний виклик для піддиректорій
            } else {
                addFileToZip(file, entryName, zos);
            }
        }
    }

    /**
     * Рекурсивно видаляє директорію та весь її вміст.
     * @param directoryToBeDeleted директорія для видалення.
     * @return true, якщо директорію успішно видалено, інакше false.
     */
    private boolean deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file); // Рекурсивне видалення вмісту
            }
        }
        return directoryToBeDeleted.delete(); // Видалення самої директорії
    }

    /**
     * Видаляє вміст директорії (файли та піддиректорії), але не саму директорію.
     * @param directory директорія, вміст якої потрібно видалити.
     */
    private void deleteDirectoryContents(File directory) {
        File[] allContents = directory.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                if (file.isDirectory()) {
                    deleteDirectory(file); // Рекурсивне видалення піддиректорій
                } else {
                    if (!file.delete()) { // Видалення файлів
                        log.warn("Не вдалося видалити файл: {}", file.getAbsolutePath());
                    }
                }
            }
        }
    }

    /**
     * Копіює вміст однієї директорії в іншу.
     * @param source директорія-джерело.
     * @param target директорія-призначення.
     * @throws IOException якщо виникає помилка копіювання.
     */
    private void copyDirectoryContents(File source, File target) throws IOException {
        if (!target.exists()) {
            if (!target.mkdirs()) {
                throw new IOException("Не вдалося створити цільову директорію: " + target.getAbsolutePath());
            }
        }
        File[] files = source.listFiles();
        if (files == null) {
            log.warn("Неможливо прочитати вміст вихідної папки: {} (можливо, немає прав або папка порожня).", source.getAbsolutePath());
            return;
        }

        for (File file : files) {
            Path sourcePath = file.toPath();
            Path targetPath = new File(target, file.getName()).toPath();
            if (file.isDirectory()) {
                copyDirectoryContents(file, targetPath.toFile()); // Рекурсивне копіювання піддиректорій
            } else {
                Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                log.debug("Скопійовано файл: {} -> {}", sourcePath, targetPath);
            }
        }
    }

    // --- Допоміжні методи для показу Alert ---

    /**
     * Показує інформаційне діалогове вікно.
     * @param title заголовок вікна.
     * @param header заголовок повідомлення.
     * @param content текст повідомлення.
     */
    private void showInfoAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.initOwner(getStage()); // Встановлення батьківського вікна
        alert.showAndWait();
    }

    /**
     * Показує діалогове вікно з повідомленням про помилку.
     * @param title заголовок вікна.
     * @param header заголовок повідомлення.
     * @param content текст повідомлення про помилку.
     */
    private void showErrorAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.initOwner(getStage()); // Встановлення батьківського вікна
        alert.showAndWait();
    }

    /**
     * Отримує поточне вікно (Stage) програми.
     * @return об'єкт Stage або null, якщо вікно не вдалося визначити.
     */
    private Stage getStage() {
        // Спроба отримати вікно через один з доступних FXML елементів
        if (statusLabel != null && statusLabel.getScene() != null) {
            return (Stage) statusLabel.getScene().getWindow();
        }
        if (exportDataButton != null && exportDataButton.getScene() != null) {
            return (Stage) exportDataButton.getScene().getWindow();
        }
        if (importDataButton != null && importDataButton.getScene() != null) {
            return (Stage) importDataButton.getScene().getWindow();
        }
        log.warn("Не вдалося визначити поточне вікно (Stage) для ImportExportController.");
        return null; // Повернення null, якщо вікно не знайдено
    }
}