package working_project.input;

import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;

/**
 * Обёртка над нативным диалогом выбора файла.
 * Возвращает путь к файлу или null, если пользователь отменил.
 */
public class FileDialogHandler {

    /**
     * Открывает окно загрузки и возвращает полный путь выбранного файла.
     */
    public String openFileDialog() {
        FileDialog dlg = new FileDialog((Frame) null, "Select a model file", FileDialog.LOAD);
        dlg.setFilenameFilter((dir, name) -> {
            String lower = name.toLowerCase();
            return lower.endsWith(".stl") || lower.endsWith(".ply") || lower.endsWith(".obj");
        });
        dlg.setVisible(true);
        String file = dlg.getFile();
        String dir  = dlg.getDirectory();
        if (file != null && dir != null) {
            return dir + file;
        }
        return null;
    }

    /**
     * Открывает окно сохранения и возвращает полный путь, куда нужно сохранить.
     */
    public String saveFileDialog() {
        FileDialog dlg = new FileDialog((Frame) null, "Save model", FileDialog.SAVE);
        dlg.setFilenameFilter((dir, name) -> {
            String lower = name.toLowerCase();
            return lower.endsWith(".stl") || lower.endsWith(".ply") || lower.endsWith(".obj");
        });
        dlg.setVisible(true);
        String file = dlg.getFile();
        String dir  = dlg.getDirectory();
        if (file != null && dir != null) {
            // Гарантируем, что расширение есть
            if (!(file.toLowerCase().endsWith(".stl")
                    || file.toLowerCase().endsWith(".ply")
                    || file.toLowerCase().endsWith(".obj"))) {
                file += ".obj"; // дефолт
            }
            return dir + file;
        }
        return null;
    }
}