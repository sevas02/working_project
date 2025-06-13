package working_project.model;

import working_project.geometry.Point3D;
import working_project.geometry.Triangle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Хранит данные модели: вершины и треугольники.
 * Логика обработки (шум, компоненты, сглаживание) вынесена в отдельные сервисы.
 */
public class Model {
    private final List<Point3D> vertices;
    private final List<Triangle> triangles;

    /**
     * Создает пустую модель.
     */
    public Model() {
        this.vertices = new ArrayList<>();
        this.triangles = new ArrayList<>();
    }

    /**
     * Создает модель с заданными вершинами и треугольниками.
     */
    public Model(List<Point3D> vertices, List<Triangle> triangles) {
        this.vertices = new ArrayList<>(vertices);
        this.triangles = new ArrayList<>(triangles);
    }

    /**
     * Возвращает неизменяемый список вершин.
     */
    public List<Point3D> getVertices() {
        return Collections.unmodifiableList(vertices);
    }

    /**
     * Возвращает неизменяемый список треугольников.
     */
    public List<Triangle> getTriangles() {
        return Collections.unmodifiableList(triangles);
    }

    /**
     * Добавляет вершину в конец списка.
     */
    public void addVertex(Point3D vertex) {
        vertices.add(vertex);
    }

    /**
     * Добавляет треугольник в конец списка.
     */
    public void addTriangle(Triangle triangle) {
        triangles.add(triangle);
    }

    /**
     * Очищает модель: удаляет все вершины и треугольники.
     */
    public void clear() {
        vertices.clear();
        triangles.clear();
    }
}