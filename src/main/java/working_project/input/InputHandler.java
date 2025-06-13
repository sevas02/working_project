// working_project/input/InputHandler.java
package working_project.input;

import imgui.ImGui;
import working_project.core.Camera;
import working_project.core.WindowManager;
import working_project.geometry.Point3D;
import working_project.service.SceneService;
import org.joml.Vector3f;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Обработка ввода мыши, клавиатуры и скролла.
 */
public class InputHandler {
    private final WindowManager window;
    private final Camera camera;
    private final SceneService sceneService;
    private final AtomicBoolean lassoMode;

    private boolean leftMousePressed = false;
    private boolean rightMousePressed = false;
    private double lastX;
    private double lastY;
    private boolean firstMouse = true;

    public InputHandler(WindowManager window, Camera camera, SceneService sceneService, AtomicBoolean lassoMode) {
        this.window = window;
        this.camera = camera;
        this.sceneService = sceneService;
        this.lassoMode = lassoMode;

        // Изначальная позиция курсора — центр окна
        lastX = window.getWidth() / 2.0;
        lastY = window.getHeight() / 2.0;

        initCallbacks();
    }

    private void initCallbacks() {
        // Кнопки WASD и стрелки
        window.setKeyCallback((win, key, scancode, action, mods) -> {
            if (action == org.lwjgl.glfw.GLFW.GLFW_PRESS || action == org.lwjgl.glfw.GLFW.GLFW_REPEAT) {
                float moveSpeed = 0.2f;
                float rotSpeed  = 2.0f;
                // Камера: WASD
                if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_W) camera.move(0,  moveSpeed, 0);
                if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_S) camera.move(0, -moveSpeed, 0);
                if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_A) camera.move(-moveSpeed, 0, 0);
                if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_D) camera.move( moveSpeed, 0, 0);
                // Модель: стрелки
                if (!lassoMode.get()) {
                    float yaw   = sceneService.getModelYaw();
                    float pitch = sceneService.getModelPitch();
                    if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT)  yaw   -= rotSpeed;
                    if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT) yaw   += rotSpeed;
                    if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_UP)    pitch += rotSpeed;
                    if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN)  pitch -= rotSpeed;
                    pitch = Math.max(-89.0f, Math.min(89.0f, pitch));
                    sceneService.setModelOrientation(yaw, pitch);
                }
            }
        });

        // Движение мышью
        window.setCursorPosCallback((win, xpos, ypos) -> {
            if (ImGui.getIO().getWantCaptureMouse()) return;
            // Модель: LMB
            if (!lassoMode.get() && leftMousePressed) {
                if (firstMouse) {
                    lastX = xpos; lastY = ypos; firstMouse = false;
                    return;
                }
                float sensitivity = 0.05f;
                float dx = (float)(xpos - lastX);
                float dy = (float)(lastY - ypos);
                float yaw   = sceneService.getModelYaw()   + dx * sensitivity;
                float pitch = sceneService.getModelPitch() + dy * sensitivity;
                pitch = Math.max(-89.0f, Math.min(89.0f, pitch));
                sceneService.setModelOrientation(yaw, pitch);
            }
            // Камера: RMB
            if (rightMousePressed) {
                if (firstMouse) {
                    lastX = xpos; lastY = ypos; firstMouse = false;
                    return;
                }
                float sens = 0.005f;
                float dx = (float)(xpos - lastX);
                float dy = (float)(lastY - ypos);
                camera.rotate(dx, dy, sens);
            }
            lastX = xpos;
            lastY = ypos;
        });

        // Кнопки мыши
        window.setMouseButtonCallback((win, button, action, mods) -> {
            if (button == org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                leftMousePressed = (action == org.lwjgl.glfw.GLFW.GLFW_PRESS);
                firstMouse = true;
            }
            if (button == org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                rightMousePressed = (action == org.lwjgl.glfw.GLFW.GLFW_PRESS);
                firstMouse = true;
            }
        });

        // Скролл: зум по направлению к центру модели
        window.setScrollCallback((win, xoffset, yoffset) -> {
            if (ImGui.getIO().getWantCaptureMouse()) return;
            Point3D objectCenter = sceneService.getObjectCenter();
            Vector3f center = new Vector3f(objectCenter.x, objectCenter.y, objectCenter.z);
            Vector3f pos    = camera.getPosition();
            Vector3f dir    = new Vector3f(center).sub(pos).normalize();
            float distance  = pos.distance(center);
            float zoomSpeed = 0.1f;
            float delta     = (float) yoffset * zoomSpeed * distance;
            float newDist   = Math.max(0.1f, Math.min(100.0f, distance - delta));
            Vector3f newPos = new Vector3f(center).sub(dir.mul(newDist));
            camera.setPosition(newPos);
        });
    }
}
