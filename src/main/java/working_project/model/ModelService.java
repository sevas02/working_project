package working_project.model;

import working_project.model.io.ModelIOService;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Сервис управления моделями: загрузка, кеширование, экспорт.
 */
public class ModelService {
    private final ModelIOService ioService;
    private final Map<String, Model> cache = new HashMap<>();

    public ModelService(ModelIOService ioService) {
        this.ioService = ioService;
    }

    /**
     * Загружает модель из файла или возвращает из кеша.
     * @param filePath путь к файлу модели
     * @return загруженная или закешированная модель
     * @throws IOException при ошибках I/O
     */
    public Model loadModel(String filePath) throws IOException {
        return cache.computeIfAbsent(filePath, path -> {
            try {
                return ioService.importModel(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Экспортирует модель в файл.
     * @param filePath путь к файлу для экспорта
     * @throws IOException при ошибках I/O
     */
    public void saveModel(String filePath) throws IOException {
        Model model = cache.get(filePath);
        if (model == null) {
            throw new IllegalArgumentException("Модель не загружена: " + filePath);
        }
        ioService.exportModel(model, new File(filePath));
    }

    /**
     * Удаляет модель из кеша.
     * @param filePath путь к модели
     */
    public void removeModel(String filePath) {
        cache.remove(filePath);
    }

    /**
     * Очищает кеш всех моделей.
     */
    public void clearCache() {
        cache.clear();
    }
}