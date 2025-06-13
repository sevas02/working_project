package working_project;

import working_project.model.Model;
import working_project.model.io.ModelIOService;

import java.io.File;

public class ModelIOTest {
    public static void main(String[] args) {
        ModelIOService ioService = new ModelIOService();
        try {
            // 1) Попробуем импортировать какую-нибудь модель
            String inputPath = "src/main/resources/models (with triangles)/axee.ply"; // путь к вашему obj/stl/ply
            Model model = ioService.importModel(inputPath);
            System.out.println("Импортирована модель с " +
                    model.getVertices().size() + " вершинами и " +
                    model.getTriangles().size() + " треугольниками.");

            // 2) Экспортируем её в другой формат
            File outFile = new File("teapot_copy.ply");
            ioService.exportModel(model, outFile);
            System.out.println("Модель сохранена в: " + outFile.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}