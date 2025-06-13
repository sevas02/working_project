package working_project.processing;

import working_project.marching_cubes.MarchingCubes;
import working_project.model.Model;
import working_project.geometry.Point3D;
import working_project.geometry.Triangle;

import java.util.*;

/**
 * Сервис для различных преобразований модели: удаление шума, выделение компонентов, сглаживание и вычисление нормалей.
 */
public class ModelProcessingService {

    /**
     * Генерирует поверхность методом Marching Cubes из облака точек и заменяет содержимое модели.
     * @param model модель для заполнения поверхностью
     * @param voxelSize размер вокселя
     * @param isoLevel значение уровня изоповерхности
     */
    public void applyMarchingCubes(Model model, double voxelSize, double isoLevel) {
        // Генерируем меш
        MarchingCubes.Mesh mesh = MarchingCubes.processPointCloud(model.getVertices(), voxelSize, isoLevel);
        // Очищаем исходную модель
        model.clear();
        // Добавляем вершины
        for (double[] v : mesh.vertices) {
            model.addVertex(new Point3D((float) v[0], (float) v[1], (float) v[2]));
        }
        // Добавляем грани
        List<Point3D> verts = model.getVertices();
        for (int[] face : mesh.faces) {
            Point3D p1 = verts.get(face[0]);
            Point3D p2 = verts.get(face[1]);
            Point3D p3 = verts.get(face[2]);
            model.addTriangle(new Triangle(p1, p2, p3));
        }
        // Пересчитываем нормали
        computeNormals(model);
    }

    /**
     * Удаляет шум (мелкие несвязные компоненты) из модели.
     * @param model модель для обработки
     * @param minComponentSize минимальный размер компонента (в вершинах) для сохранения
     */
    public void removeNoise(Model model, int minComponentSize) {
        List<Triangle> triangles = new ArrayList<>(model.getTriangles());
        List<Point3D> vertices = new ArrayList<>(model.getVertices());

        // Построение смежности вершин через треугольники
        Map<Point3D, Set<Point3D>> adjacency = buildVertexAdjacency(triangles);

        // Найти все компоненты
        List<Set<Point3D>> components = findConnectedComponents(adjacency);

        // Определить, какие вершины сохранить
        Set<Point3D> toKeep = new HashSet<>();
        for (Set<Point3D> comp : components) {
            if (comp.size() >= minComponentSize) {
                toKeep.addAll(comp);
            }
        }

        // Оставить только треугольники, все вершины которых в toKeep
        List<Triangle> filtered = new ArrayList<>();
        for (Triangle t : triangles) {
            if (toKeep.contains(t.getV1()) && toKeep.contains(t.getV2()) && toKeep.contains(t.getV3())) {
                filtered.add(t);
            }
        }

        // Обновить модель
        model.clear();
        for (Point3D v : vertices) {
            if (toKeep.contains(v)) model.addVertex(v);
        }
        for (Triangle t : filtered) {
            model.addTriangle(t);
        }
        computeNormals(model);
    }

    /**
     * Вычисляет и устанавливает нормали для всех треугольников модели.
     */
    public void computeNormals(Model model) {
        for (Triangle t : model.getTriangles()) {
            t.getNormal();
        }
    }

    /**
     * Применяет лапласианово сглаживание для вершин модели.
     * @param model модель для сглаживания
     * @param lambda коэффициент сглаживания [0,1]
     * @param iterations число итераций
     */
    public void laplacianSmooth(Model model, float lambda, int iterations) {
        List<Point3D> vertices = new ArrayList<>(model.getVertices());
        Map<Point3D, Set<Point3D>> adjacency = buildVertexAdjacency(model.getTriangles());

        for (int iter = 0; iter < iterations; iter++) {
            Map<Point3D, Point3D> newPositions = new HashMap<>();
            for (Point3D v : vertices) {
                Set<Point3D> neigh = adjacency.getOrDefault(v, Collections.emptySet());
                if (neigh.isEmpty()) continue;
                float sumX=0, sumY=0, sumZ=0;
                for (Point3D n : neigh) {
                    sumX += n.x; sumY += n.y; sumZ += n.z;
                }
                int N = neigh.size();
                Point3D avg = new Point3D(sumX/N, sumY/N, sumZ/N);
                Point3D updated = new Point3D(
                        (1-lambda)*v.x + lambda*avg.x,
                        (1-lambda)*v.y + lambda*avg.y,
                        (1-lambda)*v.z + lambda*avg.z
                );
                newPositions.put(v, updated);
            }
            // Применить новые позиции
            for (Point3D v : vertices) {
                if (newPositions.containsKey(v)) {
                    Point3D np = newPositions.get(v);
                    v.x = np.x; v.y = np.y; v.z = np.z;
                }
            }
        }
        computeNormals(model);
    }

    /**
     * Строит список смежности вершин по треугольникам.
     */
    private Map<Point3D, Set<Point3D>> buildVertexAdjacency(List<Triangle> triangles) {
        Map<Point3D, Set<Point3D>> adj = new HashMap<>();
        for (Triangle t : triangles) {
            addEdge(adj, t.getV1(), t.getV2());
            addEdge(adj, t.getV2(), t.getV3());
            addEdge(adj, t.getV3(), t.getV1());
        }
        return adj;
    }

    private void addEdge(Map<Point3D, Set<Point3D>> adj, Point3D a, Point3D b) {
        adj.computeIfAbsent(a, k -> new HashSet<>()).add(b);
        adj.computeIfAbsent(b, k -> new HashSet<>()).add(a);
    }

    /**
     * Находит все связные компоненты в графе вершин.
     */
    private List<Set<Point3D>> findConnectedComponents(Map<Point3D, Set<Point3D>> adj) {
        List<Set<Point3D>> comps = new ArrayList<>();
        Set<Point3D> visited = new HashSet<>();
        for (Point3D v : adj.keySet()) {
            if (!visited.contains(v)) {
                Set<Point3D> comp = new HashSet<>();
                Deque<Point3D> stack = new ArrayDeque<>();
                stack.push(v);
                visited.add(v);
                while (!stack.isEmpty()) {
                    Point3D cur = stack.pop();
                    comp.add(cur);
                    for (Point3D nbr : adj.getOrDefault(cur, Collections.emptySet())) {
                        if (visited.add(nbr)) {
                            stack.push(nbr);
                        }
                    }
                }
                comps.add(comp);
            }
        }
        return comps;
    }
    /**
     * Оставляет в модели только треугольники из наибольшей связной компоненты.
     * @param model модель для обработки
     */
    public void findLargestConnectedComponent(Model model) {
        List<Triangle> triangles = new ArrayList<>(model.getTriangles());
        if (triangles.isEmpty()) return;

        // Построение отображения вершина -> индексы треугольников
        Map<Point3D, List<Integer>> vertexMap = new HashMap<>();
        for (int i = 0; i < triangles.size(); i++) {
            Triangle t = triangles.get(i);
            for (Point3D v : List.of(t.getV1(), t.getV2(), t.getV3())) {
                vertexMap.computeIfAbsent(v, k -> new ArrayList<>()).add(i);
            }
        }

        // Граф треугольников по общим ребрам
        Map<Integer, Set<Integer>> adj = new HashMap<>();
        for (int i = 0; i < triangles.size(); i++) adj.put(i, new HashSet<>());
        for (int i = 0; i < triangles.size(); i++) {
            Triangle t1 = triangles.get(i);
            Set<Integer> neighbors = new HashSet<>();
            for (Point3D v : List.of(t1.getV1(), t1.getV2(), t1.getV3())) {
                neighbors.addAll(vertexMap.getOrDefault(v, Collections.emptyList()));
            }
            for (Integer j : neighbors) {
                if (i < j) {
                    Triangle t2 = triangles.get(j);
                    int shared = 0;
                    for (Point3D a : List.of(t1.getV1(), t1.getV2(), t1.getV3())) {
                        for (Point3D b : List.of(t2.getV1(), t2.getV2(), t2.getV3())) {
                            if (a.equals(b)) shared++;
                        }
                    }
                    if (shared == 2) {
                        adj.get(i).add(j);
                        adj.get(j).add(i);
                    }
                }
            }
        }

        // Поиск компонент и выбор наибольшей
        Set<Integer> visited = new HashSet<>();
        List<Triangle> largest = new ArrayList<>();
        for (int i = 0; i < triangles.size(); i++) {
            if (!visited.contains(i)) {
                List<Triangle> comp = new ArrayList<>();
                Queue<Integer> queue = new LinkedList<>();
                queue.add(i);
                visited.add(i);
                while (!queue.isEmpty()) {
                    int u = queue.poll();
                    comp.add(triangles.get(u));
                    for (int v : adj.get(u)) {
                        if (visited.add(v)) queue.add(v);
                    }
                }
                if (comp.size() > largest.size()) largest = comp;
            }
        }

        // Обновление модели
        model.clear();
        Set<Point3D> verts = new HashSet<>();
        for (Triangle t : largest) {
            verts.add(t.getV1()); verts.add(t.getV2()); verts.add(t.getV3());
        }
        for (Point3D v : verts) model.addVertex(v);
        for (Triangle t : largest) model.addTriangle(t);
        computeNormals(model);
    }
}
