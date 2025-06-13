package working_project.geometry;

/**
 * Универсальное представление треугольника в 3D-пространстве.
 */
public class Triangle {
    private final Point3D v1;
    private final Point3D v2;
    private final Point3D v3;
    private Point3D normal; // вычисляется лениво

    /**
     * Создает треугольник с указанием вершин.
     * Нормаль будет вычислена при первом запросе.
     */
    public Triangle(Point3D v1, Point3D v2, Point3D v3) {
        this.v1 = v1;
        this.v2 = v2;
        this.v3 = v3;
    }

    /**
     * Создает треугольник с явно указанной нормалью.
     */
    public Triangle(Point3D v1, Point3D v2, Point3D v3, Point3D normal) {
        this(v1, v2, v3);
        this.normal = normal;
    }

    public Point3D getV1() { return v1; }
    public Point3D getV2() { return v2; }
    public Point3D getV3() { return v3; }

    /**
     * Возвращает нормаль. Если не задана явно, вычисляет ее.
     */
    public Point3D getNormal() {
        if (normal == null) {
            computeNormal();
        }
        return normal;
    }

    /**
     * Вычисляет нормаль к поверхности треугольника по правилу правого винта.
     */
    private void computeNormal() {
        float ux = v2.x - v1.x;
        float uy = v2.y - v1.y;
        float uz = v2.z - v1.z;
        float vx = v3.x - v1.x;
        float vy = v3.y - v1.y;
        float vz = v3.z - v1.z;

        float nx = uy * vz - uz * vy;
        float ny = uz * vx - ux * vz;
        float nz = ux * vy - uy * vx;
        float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (length == 0) length = 1;
        this.normal = new Point3D(nx / length, ny / length, nz / length);
    }

    /**
     * Проверяет, принадлежит ли указанная вершина этому треугольнику.
     */
    public boolean contains(Point3D p) {
        return v1.equals(p) || v2.equals(p) || v3.equals(p);
    }
}
