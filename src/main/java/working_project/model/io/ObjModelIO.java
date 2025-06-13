package working_project.model.io;

import working_project.geometry.Point3D;
import working_project.geometry.Triangle;
import working_project.model.Model;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Обработчик импорта и экспорта моделей в формате OBJ.
 */
public class ObjModelIO implements ModelIO {
    private static final String EXTENSION = ".obj";

    @Override
    public boolean supports(String filePath) {
        return filePath.toLowerCase().endsWith(EXTENSION);
    }

    @Override
    public Model importModel(String filePath) throws IOException {
        Model model = new Model();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("v ")) {
                    String[] parts = line.split("\\s+");
                    model.addVertex(new Point3D(
                            Float.parseFloat(parts[1]),
                            Float.parseFloat(parts[2]),
                            Float.parseFloat(parts[3])
                    ));
                } else if (line.startsWith("f ")) {
                    String[] parts = line.split("\\s+");
                    int i1 = Integer.parseInt(parts[1].split("/")[0]) - 1;
                    int i2 = Integer.parseInt(parts[2].split("/")[0]) - 1;
                    int i3 = Integer.parseInt(parts[3].split("/")[0]) - 1;
                    Point3D v1 = model.getVertices().get(i1);
                    Point3D v2 = model.getVertices().get(i2);
                    Point3D v3 = model.getVertices().get(i3);
                    model.addTriangle(new Triangle(v1, v2, v3));
                }
            }
        }
        return model;
    }

    @Override
    public void exportModel(Model model, File file) throws IOException {
        List<Point3D> vertices = model.getVertices();
        List<Triangle> triangles = model.getTriangles();
        Map<Point3D, Integer> indexMap = new HashMap<>();
        int idx = 1;
        try (PrintWriter out = new PrintWriter(new FileWriter(file))) {
            // Вершины
            for (Point3D v : vertices) {
                indexMap.put(v, idx++);
                out.printf(Locale.US, "v %.6f %.6f %.6f%n", v.x, v.y, v.z);
            }
            // Грани
            for (Triangle t : triangles) {
                int i1 = indexMap.get(t.getV1());
                int i2 = indexMap.get(t.getV2());
                int i3 = indexMap.get(t.getV3());
                out.printf("f %d %d %d%n", i1, i2, i3);
            }
        }
    }
}
