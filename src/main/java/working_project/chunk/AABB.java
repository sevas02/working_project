package working_project.chunk;

import org.joml.Vector3f;
import working_project.geometry.Point3D;
import working_project.geometry.Triangle;

import java.util.List;

/**
 * Ось-выравненный ограничивающий параллелепипед для набора треугольников.
 */
public class AABB {
    public Point3D min;
    public Point3D max;

    AABB(Point3D min, Point3D max) {
        this.min = min;
        this.max = max;
    }

    /**
     * Вычисляет AABB для списка треугольников.
     */
    public static AABB compute(List<Triangle> triangles) {
        if (triangles.isEmpty()) {
            throw new IllegalArgumentException("Нет треугольников для вычисления AABB");
        }
        Point3D min = new Point3D(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
        Point3D max = new Point3D(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE);
        for (Triangle tri : triangles) {
            for (Point3D v : List.of(tri.getV1(), tri.getV2(), tri.getV3())) {
                min.x = Math.min(min.x, v.x);
                min.y = Math.min(min.y, v.y);
                min.z = Math.min(min.z, v.z);
                max.x = Math.max(max.x, v.x);
                max.y = Math.max(max.y, v.y);
                max.z = Math.max(max.z, v.z);
            }
        }
        return new AABB(min, max);
    }

    public Point3D center() {
        return new Point3D(
                (min.x + max.x) * 0.5f,
                (min.y + max.y) * 0.5f,
                (min.z + max.z) * 0.5f
        );
    }
}
