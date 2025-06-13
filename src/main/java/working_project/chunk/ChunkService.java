package working_project.chunk;

import working_project.geometry.Point3D;
import working_project.geometry.Triangle;
import working_project.model.Model;
import java.util.ArrayList;
import java.util.List;

/**
 * Сервис разбиения модели на чанки.
 */
public class ChunkService {
    /**
     * Разбивает модель на чанки заданного максимального количества треугольников.
     * @param model модель для чанкинга
     * @param maxTriangles максимальное число треугольников в чанке
     * @return список чанков, каждый представляет собой набор треугольников
     */
    public List<Chunk> createChunks(Model model, int maxTriangles) {
        List<Chunk> chunks = new ArrayList<>();
        Chunk current = new Chunk();
        int count = 0;
        for (Triangle t : model.getTriangles()) {
            if (count >= maxTriangles) {
                chunks.add(current);
                current = new Chunk();
                count = 0;
            }
            current.addTriangle(t);
            count++;
        }
        if (!current.getTriangles().isEmpty()) {
            chunks.add(current);
        }
        return chunks;
    }

    /**
     * Вычисляет общий AABB для списка чанков.
     */
    public AABB getGlobalAABB(List<Chunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            throw new IllegalArgumentException("Chunk list is empty");
        }
        // Инициализируем экстремумы
        Point3D min = new Point3D(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
        Point3D max = new Point3D(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE);

        for (Chunk c : chunks) {
            AABB box = c.getAABB();
            Point3D bmin = box.min;
            Point3D bmax = box.max;
            // Обновляем min
            min.x = Math.min(min.x, bmin.x);
            min.y = Math.min(min.y, bmin.y);
            min.z = Math.min(min.z, bmin.z);
            // Обновляем max
            max.x = Math.max(max.x, bmax.x);
            max.y = Math.max(max.y, bmax.y);
            max.z = Math.max(max.z, bmax.z);
        }
        return new AABB(min, max);
    }

    /**
     * Класс-чанк, содержащий список треугольников и вычисляемый AABB.
     */
    public static class Chunk {
        private final List<Triangle> triangles = new ArrayList<>();
        private AABB aabb;

        public void addTriangle(Triangle t) {
            triangles.add(t);
            // lazy-инвалидация AABB
            aabb = null;
        }

        public List<Triangle> getTriangles() {
            return triangles;
        }

        /**
         * Возвращает ось-выравненный ограничивающий параллелепипед.
         */
        public AABB getAABB() {
            if (aabb == null) {
                aabb = AABB.compute(triangles);
            }
            return aabb;
        }
    }
}