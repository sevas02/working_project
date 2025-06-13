package working_project.rendering;

import working_project.chunk.ChunkService.Chunk;
import working_project.geometry.Point3D;
import working_project.core.Camera;
import working_project.core.WindowManager;
import working_project.geometry.Triangle;

import java.util.List;


/**
 * Сервис визуализации сцены: управление данными (чанки, облако точек, ориентация)
 * и делегирование отрисовки в Renderer.
 */
public class RenderService {
    private final Renderer renderer;

    public RenderService(WindowManager window, Camera camera) {
        this.renderer = new Renderer(window, camera);
        this.renderer.init();
    }

    /**
     * Обновляет меши чанков для отрисовки.
     */
    public void updateChunks(List<Chunk> chunks) {
        renderer.clearMeshes();
        for (Chunk c : chunks) {
            // Преобразуем список треугольников в массивы вершин (позиции + нормали) и индексов
            List<Triangle> tris = c.getTriangles();
            int triCount = tris.size();
            float[] vertexData = new float[triCount * 18]; // 3 вершины × (3 координаты + 3 нормали)
            int[] indices = new int[triCount * 3];
            int vPos = 0, iPos = 0;

            for (int t = 0; t < triCount; t++) {
                Triangle tri = tris.get(t);
                Point3D[] verts = { tri.getV1(), tri.getV2(), tri.getV3() };
                Point3D norm = tri.getNormal();

                // Записываем в буфер: позиция и нормаль
                for (Point3D v : verts) {
                    vertexData[vPos++] = v.x;
                    vertexData[vPos++] = v.y;
                    vertexData[vPos++] = v.z;
                    vertexData[vPos++] = norm.x;
                    vertexData[vPos++] = norm.y;
                    vertexData[vPos++] = norm.z;
                }
                // Заполняем индексы треугольника
                indices[iPos++] = t * 3;
                indices[iPos++] = t * 3 + 1;
                indices[iPos++] = t * 3 + 2;
            }

            Mesh mesh = new Mesh(vertexData, indices);
            renderer.addMesh(mesh);
        }

    }

    /** Передать облако точек для отрисовки */
    public void updatePointCloud(List<Point3D> points) {
        renderer.clearMeshes();
        renderer.updatePointCloud(points);
    }

    /** Задать вращение модели */
    public void setModelOrientation(float yaw, float pitch) {
        renderer.setOrientation(yaw, pitch);
    }

    /** Задать цвет объекта */
    public void setObjectColor(float r, float g, float b) {
        renderer.setObjectColor(r, g, b);
    }

    /** Отрисовать один кадр */
    public void renderFrame() {
        renderer.renderScene();
    }

    /** Освободить все OpenGL-ресурсы */
    public void cleanup() {
        renderer.cleanup();
    }
}
