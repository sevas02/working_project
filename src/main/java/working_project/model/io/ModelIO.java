package working_project.model.io;

import working_project.model.Model;
import java.io.File;
import java.io.IOException;

/**
 * Интерфейс для обработчиков импорта и экспорта моделей.
 */
public interface ModelIO {
    /**
     * Определяет, поддерживает ли обработчик указанный файл по его пути (расширению).
     * @param filePath путь к файлу
     * @return true, если формат поддерживается
     */
    boolean supports(String filePath);

    /**
     * Импортирует модель из указанного файла.
     * @param filePath путь к файлу модели
     * @return загруженная модель
     * @throws IOException при ошибках чтения файла
     */
    Model importModel(String filePath) throws IOException;

    /**
     * Экспортирует модель в указанный файл.
     * @param model модель для сохранения
     * @param file файл назначения
     * @throws IOException при ошибках записи
     */
    void exportModel(Model model, File file) throws IOException;
}
