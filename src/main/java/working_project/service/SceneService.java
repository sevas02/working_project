package working_project.service;

import working_project.model.Model;
import working_project.model.io.ModelIOService;
import working_project.processing.ModelProcessingService;
import working_project.model.ModelService;
import working_project.chunk.ChunkService;
import working_project.chunk.ChunkService.Chunk;
import working_project.rendering.RenderService;
import working_project.geometry.Point3D;

import java.io.IOException;
import java.util.List;

/**
 * Сервис управления сценой: загрузка/сохранение модели, её обработка, чанкинг и рендер.
 */
public class SceneService {
    private final ModelService modelService;
    private final ModelProcessingService processingService;
    private final ChunkService chunkService;
    private final RenderService renderService;
    private float modelYaw = 0f;
    private float modelPitch = 0f;
    private float[] objectColor = {0.0f, 0.5f, 1.0f};

    private Model currentModel;
    private List<Chunk> currentChunks;
    private String currentModelPath;

    public SceneService(ModelIOService ioService, RenderService renderService) {
        this.modelService = new ModelService(ioService);
        this.processingService = new ModelProcessingService();
        this.chunkService = new ChunkService();
        this.renderService = renderService;
    }

    /**
     * Возвращает текущий цвет объекта.
     */
    public float[] getObjectColor() {
        // возвращаем копию, чтобы никто не сломал инвариант
        return new float[] { objectColor[0], objectColor[1], objectColor[2] };
    }

    /**
     * Устанавливает цвет объекта и сообщает об этом RenderService.
     */
    public void setObjectColor(float r, float g, float b) {
        objectColor[0] = r;
        objectColor[1] = g;
        objectColor[2] = b;
        // Прокидываем в рендерер
        renderService.setObjectColor(r, g, b);
    }

    public float getModelYaw() {
        return modelYaw;
    }

    public float getModelPitch() {
        return modelPitch;
    }

    public void setModelOrientation(float yaw, float pitch) {
        this.modelYaw   = yaw;
        this.modelPitch = pitch;
    }

    public Point3D getObjectCenter() {
        if (currentChunks != null && !currentChunks.isEmpty()) {
            return chunkService.getGlobalAABB(currentChunks).center();
        } else if (currentModel != null) {
            // вычисляем центр по вершинам currentModel
            float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
            float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
            for (Point3D v : currentModel.getVertices()) {
                minX = Math.min(minX, v.x);
                minY = Math.min(minY, v.y);
                minZ = Math.min(minZ, v.z);
                maxX = Math.max(maxX, v.x);
                maxY = Math.max(maxY, v.y);
                maxZ = Math.max(maxZ, v.z);
            }
            return new Point3D((minX + maxX) * 0.5f,
                    (minY + maxY) * 0.5f,
                    (minZ + maxZ) * 0.5f);
        }
        return new Point3D(0.0f, 0.0f, 0.0f);
    }

    /**
     * Загружает модель из файла и подготавливает данные для рендеринга.
     */
    public void loadModel(String path) throws IOException {
        this.currentModelPath = path;
        currentModel = modelService.loadModel(path);
        processingService.computeNormals(currentModel);
        currentChunks = chunkService.createChunks(currentModel, 500);
        renderService.updateChunks(currentChunks);
    }

    /**
     * Сохраняет текущую модель в файл.
     */
    public void saveModel(String destinationPath) throws IOException {
        if (currentModel == null) throw new IllegalStateException("Model not loaded");
        modelService.saveModel(destinationPath);
    }

    /**
     * Удаляет шум из модели и обновляет рендеринг.
     */
    public void removeNoise(int minComponentSize) {
        processingService.removeNoise(currentModel, minComponentSize);
        refreshChunks();
    }

    /**
     * Оставляет только наибольшую компоненту и обновляет рендеринг.
     */
    public void findLargestComponent() {
        processingService.findLargestConnectedComponent(currentModel);
        refreshChunks();
    }

    /**
     * Применяет лапласианово сглаживание и обновляет рендеринг.
     */
    public void smoothModel(float lambda, int iterations) {
        processingService.laplacianSmooth(currentModel, lambda, iterations);
        refreshChunks();
    }

    /**
     * Применяет метод марширующих кубов.
     */
    public void applyMarchingCubes(double voxelSize, double isolevel) {
        processingService.applyMarchingCubes(currentModel, voxelSize, isolevel);
        refreshChunks();
    }

    /**
     * Сглаживает границы модели и обновляет рендеринг.
     */
//    public void smoothBoundaries(float factor, int iterations) {
//        processingService.smoothBoundaries(currentModel, factor, iterations);
//        refreshChunks();
//    }

    /**
     * Генерирует поверхностную модель методом Marching Cubes и обновляет рендеринг.
     */
    public void generateIsosurface(List<Point3D> pointCloud, float voxelSize, float isoLevel) {
        processingService.applyMarchingCubes(currentModel, voxelSize, isoLevel);
        refreshChunks();
    }

    /**
     * Перерисовывает чанки в рендерере.
     */
    private void refreshChunks() {
        currentChunks = chunkService.createChunks(currentModel, 500);
        renderService.updateChunks(currentChunks);
    }

    /**
     * Рендерит текущий кадр (вызывается в главном цикле).
     */
    public void renderFrame() {
        renderService.renderFrame();
    }

    public boolean isModelLoaded() {
        return currentModel != null;
    }

    public String getCurrentModelPath() {
        return currentModelPath;
    }

    public void cleanup() {
        renderService.cleanup();
    }
}
