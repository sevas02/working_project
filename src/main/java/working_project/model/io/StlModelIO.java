package working_project.model.io;

import working_project.geometry.Point3D;
import working_project.geometry.Triangle;
import working_project.model.Model;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Обработчик импорта и экспорта моделей в формате STL.
 */
public class StlModelIO implements ModelIO {
    private static final String EXTENSION = ".stl";

    @Override
    public boolean supports(String filePath) {
        return filePath.toLowerCase().endsWith(EXTENSION);
    }

    @Override
    public Model importModel(String filePath) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String first = br.readLine();
            if (first != null && first.trim().startsWith("solid")) {
                return readAscii(filePath);
            }
        }
        return readBinary(filePath);
    }

    private Model readAscii(String filePath) throws IOException {
        Model model = new Model();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            List<Point3D> facet = new ArrayList<>();
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("vertex")) {
                    String[] p = line.split("\\s+");
                    facet.add(new Point3D(
                            Float.parseFloat(p[1]),
                            Float.parseFloat(p[2]),
                            Float.parseFloat(p[3])
                    ));
                } else if (line.startsWith("endfacet")) {
                    if (facet.size() == 3) {
                        Point3D v1 = facet.get(0), v2 = facet.get(1), v3 = facet.get(2);
                        model.addTriangle(new Triangle(v1, v2, v3));
                        model.addVertex(v1); model.addVertex(v2); model.addVertex(v3);
                    }
                    facet.clear();
                }
            }
        }
        return model;
    }

    private Model readBinary(String filePath) throws IOException {
        Model model = new Model();
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
            raf.seek(80);
            int count = Integer.reverseBytes(raf.readInt());
            for (int i = 0; i < count; i++) {
                byte[] data = new byte[50];
                raf.readFully(data);
                ByteBuffer bb = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
                Point3D v1 = new Point3D(bb.getFloat(12), bb.getFloat(16), bb.getFloat(20));
                Point3D v2 = new Point3D(bb.getFloat(24), bb.getFloat(28), bb.getFloat(32));
                Point3D v3 = new Point3D(bb.getFloat(36), bb.getFloat(40), bb.getFloat(44));
                model.addTriangle(new Triangle(v1, v2, v3));
                model.addVertex(v1); model.addVertex(v2); model.addVertex(v3);
            }
        }
        return model;
    }

    @Override
    public void exportModel(Model model, File file) throws IOException {
        List<Triangle> triangles = model.getTriangles();
        try (PrintWriter out = new PrintWriter(new FileWriter(file))) {
            out.println("solid exported_model");
            for (Triangle t : triangles) {
                Point3D n = t.getNormal();
                out.printf(Locale.US, "  facet normal %.6f %.6f %.6f%n", n.x, n.y, n.z);
                out.println("    outer loop");
                Point3D v1 = t.getV1(), v2 = t.getV2(), v3 = t.getV3();
                out.printf(Locale.US, "      vertex %.6f %.6f %.6f%n", v1.x, v1.y, v1.z);
                out.printf(Locale.US, "      vertex %.6f %.6f %.6f%n", v2.x, v2.y, v2.z);
                out.printf(Locale.US, "      vertex %.6f %.6f %.6f%n", v3.x, v3.y, v3.z);
                out.println("    endloop");
                out.println("  endfacet");
            }
            out.println("endsolid exported_model");
        }
    }
}