package working_project.model.io;

import working_project.model.Model;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Сервис для управления импортом и экспортом моделей через зарегистрированные обработчики ModelIO.
 */
public class ModelIOService {
    private final List<ModelIO> handlers = new ArrayList<>();

    public ModelIOService() {
        ServiceLoader.load(ModelIO.class).forEach(handlers::add);
    }

    /**
     * Импортирует модель из файла, автоматически выбирая подходящий обработчик.
     * @param filePath путь к файлу модели
     * @return загруженная модель
     * @throws IOException при ошибках чтения
     */
    public Model importModel(String filePath) throws IOException {
        for (ModelIO handler : handlers) {
            if (handler.supports(filePath)) {
                return handler.importModel(filePath);
            }
        }
        throw new IOException("Нет обработчика ModelIO для файла: " + filePath);
    }

    /**
     * Экспортирует модель в указанный файл, автоматически выбирая подходящий обработчик.
     * @param model модель для экспорта
     * @param file файл назначения
     * @throws IOException при ошибках записи
     */
    public void exportModel(Model model, File file) throws IOException {
        String path = file.getAbsolutePath();
        for (ModelIO handler : handlers) {
            if (handler.supports(path)) {
                handler.exportModel(model, file);
                return;
            }
        }
        throw new IOException("Нет обработчика ModelIO для файла: " + path);
    }
}

