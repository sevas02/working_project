package working_project.model.io;

import working_project.geometry.Point3D;
import working_project.geometry.Triangle;
import working_project.model.Model;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

/**
 * Обработчик импорта и экспорта моделей в формате PLY.
 */
public class PlyModelIO implements ModelIO {
    private static final String EXTENSION = ".ply";

    @Override
    public boolean supports(String filePath) {
        return filePath.toLowerCase().endsWith(EXTENSION);
    }

    @Override
    public Model importModel(String filePath) throws IOException {
        // Определяем формат (ASCII или Binary) по заголовку
        boolean isBinary = false;
        long headerEnd = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                headerEnd += line.length() + 1; // + newline
                line = line.trim();
                if (line.startsWith("format binary_little_endian")) {
                    isBinary = true;
                }
                if (line.equals("end_header")) {
                    break;
                }
            }
        }
        return isBinary ? readBinary(filePath, headerEnd) : readAscii(filePath);
    }

    private Model readAscii(String filePath) throws IOException {
        Model model = new Model();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean header = true;
            int vertexCount = 0, faceCount = 0;
            List<String> body = new ArrayList<>();
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (header) {
                    if (line.startsWith("element vertex")) {
                        vertexCount = Integer.parseInt(line.split("\s+")[2]);
                    } else if (line.startsWith("element face")) {
                        faceCount = Integer.parseInt(line.split("\s+")[2]);
                    } else if (line.equals("end_header")) {
                        header = false;
                    }
                    continue;
                }
                body.add(line);
            }
            // Парсим вершины
            int idx = 0;
            for (; idx < vertexCount && idx < body.size(); idx++) {
                String[] parts = body.get(idx).split("\s+");
                model.addVertex(new Point3D(
                        Float.parseFloat(parts[0]),
                        Float.parseFloat(parts[1]),
                        Float.parseFloat(parts[2])
                ));
            }
            // Парсим грани
            for (int i = 0; i < faceCount && idx < body.size(); i++, idx++) {
                String[] parts = body.get(idx).split("\s+");
                if (parts.length >= 4 && Integer.parseInt(parts[0]) == 3) {
                    int v1 = Integer.parseInt(parts[1]);
                    int v2 = Integer.parseInt(parts[2]);
                    int v3 = Integer.parseInt(parts[3]);
                    Point3D p1 = model.getVertices().get(v1);
                    Point3D p2 = model.getVertices().get(v2);
                    Point3D p3 = model.getVertices().get(v3);
                    model.addTriangle(new Triangle(p1, p2, p3));
                }
            }
        }
        return model;
    }

    private Model readBinary(String filePath, long dataStart) throws IOException {
        Model model = new Model();
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
            raf.seek(dataStart);
            // Снова читаем header, чтобы получить counts
            raf.seek(0);
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            String line;
            int vertexCount = 0, faceCount = 0;
            long offset = 0;
            while ((line = reader.readLine()) != null) {
                offset += line.length() + 1;
                line = line.trim();
                if (line.startsWith("element vertex")) {
                    vertexCount = Integer.parseInt(line.split("\s+")[2]);
                } else if (line.startsWith("element face")) {
                    faceCount = Integer.parseInt(line.split("\s+")[2]);
                } else if (line.equals("end_header")) {
                    break;
                }
            }
            raf.seek(offset);
            // Читаем вершины
            for (int i = 0; i < vertexCount; i++) {
                byte[] buf = new byte[12];
                raf.readFully(buf);
                ByteBuffer bb = ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN);
                model.addVertex(new Point3D(bb.getFloat(), bb.getFloat(), bb.getFloat()));
            }
            // Читаем грани
            for (int i = 0; i < faceCount; i++) {
                int vertexPerFace = raf.readUnsignedByte();
                if (vertexPerFace == 3) {
                    int v1 = raf.readInt();
                    int v2 = raf.readInt();
                    int v3 = raf.readInt();
                    Point3D p1 = model.getVertices().get(v1);
                    Point3D p2 = model.getVertices().get(v2);
                    Point3D p3 = model.getVertices().get(v3);
                    model.addTriangle(new Triangle(p1, p2, p3));
                } else {
                    // Пропускаем лишние данные
                    raf.skipBytes(vertexPerFace * 4);
                }
                raf.skipBytes(1); // attribute byte count
            }
        }
        return model;
    }

    @Override
    public void exportModel(Model model, File file) throws IOException {
        List<Point3D> vertices = model.getVertices();
        List<Triangle> triangles = model.getTriangles();
        Map<Point3D, Integer> indexMap = new HashMap<>();
        int idx = 0;
        try (PrintWriter out = new PrintWriter(new FileWriter(file))) {
            out.println("ply");
            out.println("format ascii 1.0");
            out.println("element vertex " + vertices.size());
            out.println("property float x");
            out.println("property float y");
            out.println("property float z");
            out.println("element face " + triangles.size());
            out.println("property list uchar int vertex_indices");
            out.println("end_header");
            for (Point3D v : vertices) { indexMap.put(v, idx++); out.printf(Locale.US,"%.6f %.6f %.6f%n",v.x,v.y,v.z);}
            for (Triangle t : triangles) {
                int i1=indexMap.get(t.getV1()), i2=indexMap.get(t.getV2()), i3=indexMap.get(t.getV3());
                out.printf("3 %d %d %d%n",i1,i2,i3);
            }
        }
    }
}
