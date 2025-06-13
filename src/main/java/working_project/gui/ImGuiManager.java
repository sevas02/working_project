// working_project/gui/ImGuiManager.java
package working_project.gui;

import imgui.ImFontAtlas;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.ImGuiStyle;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiConfigFlags;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiDockNodeFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import imgui.type.ImFloat;
import imgui.type.ImBoolean;
import imgui.type.ImInt;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryUtil;
import working_project.core.WindowManager;
import working_project.input.FileDialogHandler;
import working_project.service.SceneService;

import java.io.IOException;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;

/**
 * Управление Dear ImGui интерфейсом: панель управления сценой.
 */
public class ImGuiManager {
    private final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();
    private final ImGuiImplGl3  imGuiGl3  = new ImGuiImplGl3();
    private int dockingNodeId;

    private final ImFloat voxelSize = new ImFloat(0.1f);
    private final ImFloat isoLevel  = new ImFloat(1.0f);

    public ImGuiManager(WindowManager window) {
        // Создаём контекст
        ImGui.createContext();
        ImGuiIO io = ImGui.getIO();
        io.addConfigFlags(ImGuiConfigFlags.NavEnableKeyboard);
        io.addConfigFlags(ImGuiConfigFlags.DockingEnable);
        io.addConfigFlags(ImGuiConfigFlags.ViewportsEnable);

        // Инициализация для GLFW и OpenGL3
        imGuiGlfw.init(window.getWindow(), true);
        imGuiGl3.init("#version 410 core");

        // Настройка шрифта
        ImFontAtlas atlas = io.getFonts();
        atlas.addFontDefault();
        // Генерация текстуры шрифта
        ImInt texW = new ImInt(), texH = new ImInt();
        java.nio.ByteBuffer pixels = atlas.getTexDataAsRGBA32(texW, texH);
        int fontTex = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, fontTex);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, texW.get(), texH.get(), 0,
                GL_RGBA, GL_UNSIGNED_BYTE, pixels);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        atlas.setTexID(fontTex);
        atlas.clearTexData();

        // Стиль
        ImGuiStyle style = ImGui.getStyle();
        style.setColor(ImGuiCol.WindowBg,      0.1f, 0.1f, 0.13f, 1.0f);
        style.setColor(ImGuiCol.TitleBg,       0.2f, 0.2f, 0.25f, 1.0f);
        style.setColor(ImGuiCol.TitleBgActive, 0.3f, 0.3f, 0.35f, 1.0f);
        style.setColor(ImGuiCol.Button,        0.0f, 0.6f, 1.0f, 1.0f);
        style.setColor(ImGuiCol.ButtonHovered, 0.0f, 0.8f, 1.0f, 1.0f);
        style.setColor(ImGuiCol.ButtonActive,  0.0f, 0.5f, 0.9f, 1.0f);
        style.setColor(ImGuiCol.Text,          1.0f, 1.0f, 1.0f, 1.0f);
        style.setWindowRounding(8.0f);
        style.setFrameRounding(4.0f);
    }

    public void newFrame() {
        imGuiGlfw.newFrame();
        ImGui.newFrame();
        dockingNodeId = ImGui.dockSpaceOverViewport(ImGui.getMainViewport(), ImGuiDockNodeFlags.PassthruCentralNode);
    }

    /**
     * Отрисовка панели управления. Вызывает методы SceneService.
     */
    public void renderUI(SceneService sceneService, FileDialogHandler fileDialog, boolean lassoMode) throws IOException {
        ImGui.setNextWindowPos(0, 0, ImGuiCond.Always);
        ImGui.setNextWindowSize(300, (int)ImGui.getIO().getDisplaySizeY(), ImGuiCond.Always);
        ImGui.setNextWindowDockID(dockingNodeId, ImGuiCond.Always);
        int flags = ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoMove;
        ImGui.begin("3D Editor Control", flags);

        // Files
        ImGui.text("Files");
        ImGui.pushStyleColor(ImGuiCol.Button, 0.0f, 0.5f, 1.0f, 1.0f);
        if (ImGui.button("Open Model", 280, 30)) {
            String path = fileDialog.openFileDialog();
            if (path != null) sceneService.loadModel(path);
        }
        if (ImGui.button("Save Model", 280, 30)) {
            String path = fileDialog.saveFileDialog();
            if (path != null) sceneService.saveModel(path);
        }
        ImGui.popStyleColor();
        ImGui.spacing();
        ImGui.separator();

        // Processing
        ImGui.text("Model Processing");
        ImGui.pushStyleColor(ImGuiCol.Button, 0.2f, 0.7f, 0.2f, 1.0f);
        ImGui.inputFloat("Voxel Size", voxelSize, 0.01f, 0.1f, "%.2f");
        ImGui.inputFloat("Iso Level",  isoLevel,  0.1f, 1.0f, "%.1f");
        if (ImGui.button("Marching Cubes", 280, 30)) sceneService.applyMarchingCubes(voxelSize.get(), isoLevel.get());
        if (ImGui.button("Remove Noise",    280, 30)) sceneService.removeNoise(100);
        if (ImGui.button("Smooth Model",    280, 30)) System.out.println("SMOOTH TODO");//sceneService.smoothModel();
        if (ImGui.button("Largest Component", 280, 30)) sceneService.findLargestComponent();
        ImGui.popStyleColor();
        ImGui.spacing();
        ImGui.separator();

        // Rendering toggle
        ImGui.text("Rendering");
        ImGui.pushStyleColor(ImGuiCol.Button, 1.0f, 0.5f, 0.0f, 1.0f);
        if (ImGui.button("Draw Model", 280, 30)) {
            sceneService.renderFrame();
        }
        ImGui.popStyleColor();
        ImGui.separator();

        // Color
        ImGui.text("Color");
        float[] col = sceneService.getObjectColor();
        if (ImGui.colorEdit3("Object Color", col)) sceneService.setObjectColor(col[0], col[1], col[2]);
        ImGui.spacing();
        ImGui.separator();

        // Controls info
        ImGui.text("Controls:");
        ImGui.bulletText("WASD - move");
        ImGui.bulletText("LMB - rotate model");
        ImGui.bulletText("RMB - rotate camera");
        ImGui.bulletText("Scroll - zoom");

        ImGui.end();
    }

    public void renderDrawData() {
        ImGui.render();
        imGuiGl3.renderDrawData(ImGui.getDrawData());
        if (ImGui.getIO().hasConfigFlags(ImGuiConfigFlags.ViewportsEnable)) {
            long backup = org.lwjgl.glfw.GLFW.glfwGetCurrentContext();
            ImGui.updatePlatformWindows();
            ImGui.renderPlatformWindowsDefault();
            org.lwjgl.glfw.GLFW.glfwMakeContextCurrent(backup);
        }
    }

    public void cleanup() {
        imGuiGl3.dispose();
        imGuiGlfw.dispose();
        ImGui.destroyContext();
    }
}
