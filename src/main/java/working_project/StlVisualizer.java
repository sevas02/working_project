package working_project;

import working_project.core.Camera;
import working_project.core.WindowManager;
import working_project.gui.ImGuiManager;
import working_project.input.FileDialogHandler;
import working_project.input.InputHandler;
import working_project.model.io.ModelIOService;
import working_project.rendering.RenderService;
import working_project.service.SceneService;
import org.joml.Vector3f;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Главный класс приложения. Инициализирует все сервисы и запускает основной цикл.
 */
public class StlVisualizer {
    public static void main(String[] args) throws IOException {
        // 1) Инициализация окна
        WindowManager window = new WindowManager(1920, 1080, "3D Editor");

        // 2) Инициализация GUI (ImGui)
        ImGuiManager imgui = new ImGuiManager(window);

        // 3) Инициализация камеры
        Camera camera = new Camera(new Vector3f(0, 0, 5));

        // 4) Сервис рендеринга
        RenderService renderService = new RenderService(window, camera);

        // 5) Сервис ввода файлов и сцена
        FileDialogHandler fileDialog = new FileDialogHandler();
        ModelIOService ioService = new ModelIOService();
        SceneService sceneService = new SceneService(ioService, renderService);

        // 6) Обработчик ввода (камера, модель, ласссо)
        AtomicBoolean lassoMode = new AtomicBoolean(false);
        InputHandler inputHandler = new InputHandler(window, camera, sceneService, lassoMode);

        // 7) Основной цикл приложения
        while (!window.shouldClose()) {
            // Обработка событий
            window.pollEvents();

            // Начало кадра ImGui
            imgui.newFrame();

            // Отрисовка UI панели
            imgui.renderUI(sceneService, fileDialog, lassoMode.get());

            // Отрисовка сцены (модель или облако точек)
            sceneService.renderFrame();

            // Отрисовка UI и swapBuffers
            imgui.renderDrawData();
            window.swapBuffers();
        }

        // 8) Очистка ресурсов
        sceneService.cleanup();    // включает очистку рендера и чанков
        imgui.cleanup();
        window.cleanup();
    }
}
