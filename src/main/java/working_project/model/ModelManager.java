package working_project.model;

import working_project.core.Camera;
import working_project.io.FileDialogHandler;
import working_project.marching_cubes.MarchingCubes;
import org.joml.Vector3f;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import working_project.rendering.Point3D;
import working_project.rendering.Renderer;
import working_project.rendering.Triangle;

public class ModelManager {
    private final ModelLoader loader;
    private final Camera camera;
    private final Renderer renderer;
    private Model model;
    private final float[] modelYaw = {0.0f};
    private final float[] modelPitch = {0.0f};
    private final Vector3f objectCenter = new Vector3f(0, 0, 0);
    private volatile boolean isExporting = false;
    private volatile String exportStatus = "";
    private final float[] objectColor = {0.0f, 0.5f, 1.0f};

    private final ExecutorService executorService = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors() - 1)
    );

    public ModelManager(ModelLoader loader, Camera camera, Renderer renderer) {
        this.loader = loader;
        this.camera = camera;
        this.renderer = renderer;
    }

    public void loadModel(
            File file,
            List<ModelLoader.Chunk> chunks,
            List<Point3D> points,
            boolean[] isModelLoaded,
            boolean[] isPointCloud,
            boolean[] onlyPointMode) {
        if (file == null) return;

        String filePath = file.getAbsolutePath();
        String fileExtension = filePath.substring(filePath.lastIndexOf(".") + 1).toLowerCase();
        if (!"stl".equals(fileExtension) && !"ply".equals(fileExtension) && !"obj".equals(fileExtension)) {
            System.out.println("Error: unsupported file format. Select .stl, .ply or .obj.");
            return;
        }

        System.out.println("Downloading model from: " + filePath);
        try {
            model = ModelImporter.loadModel(filePath);
            for (ModelLoader.Chunk chunk : chunks) {
                chunk.cleanup();
            }
            chunks.clear();
            points.clear();
            isPointCloud[0] = false;

            if (!model.triangles.isEmpty() && !onlyPointMode[0]) {
                model.computeNormals();
                chunks.addAll(loader.createChunksFromData(model.toChunkData()));
                System.out.println("Downloaded " + chunks.size() + " chunks.");
                renderer.cleanupPointsBuffers();
            } else {
                points.addAll(model.vertices);
                isPointCloud[0] = true;
                renderer.initOrUpdatePointsBuffers(points);
                System.out.println("Downloaded point cloud with " + points.size() + " points.");
            }

            isModelLoaded[0] = !chunks.isEmpty() || !points.isEmpty();
            if (isModelLoaded[0]) {
                updateCameraAndCenter(chunks, points);
                renderer.setObjectColor(objectColor[0], objectColor[1], objectColor[2]); // Применяем начальный цвет
            } else {
                System.out.println("Couldn't load model: no chunks or points.");
                renderer.cleanupPointsBuffers();
            }
        } catch (IOException e) {
            isModelLoaded[0] = false;
            System.err.println("Model loading error: " + e.getMessage());
            e.printStackTrace();
            renderer.cleanupPointsBuffers();
        }
    }

    public void exportModel(FileDialogHandler fileDialogHandler) {
        if (!isModelLoaded() || model == null || model.vertices.isEmpty()) {
            exportStatus = "Unable to export: the model is not loaded.";
            System.out.println("Unable to export: the model is not loaded or empty.");
            return;
        }

        isExporting = true;
        exportStatus = "Starting export...";
        File file = fileDialogHandler.saveFileDialog();
        if (file != null) {
            String filePath = file.getAbsolutePath();
            String extension = filePath.contains(".") ? filePath.substring(filePath.lastIndexOf(".") + 1).toLowerCase() : "obj";
            if (!extension.equals("obj") && !extension.equals("stl") && !extension.equals("ply")) {
                filePath += ".obj";
                extension = "obj";
            }
            final String finalFilePath = filePath;
            final String finalExtension = extension;
            executorService.submit(() -> {
                try {
                    long startTime = System.nanoTime();
                    ModelExporter.exportModel(model, new File(finalFilePath), finalExtension);
                    long duration = (System.nanoTime() - startTime) / 1_000_000;
                    exportStatus = "Export finished in " + duration + " ms: " + finalFilePath;
                    System.out.println("Model exported to: " + finalFilePath);
                } catch (Exception e) {
                    exportStatus = "Export error: " + e.getMessage();
                    System.err.println("Error exporting model: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    isExporting = false;
                }
            });
        } else {
            isExporting = false;
            exportStatus = "Export canceled.";
            System.out.println("Export cancelled by user.");
        }
    }

    public void applyMarchingCubes(
            List<ModelLoader.Chunk> chunks,
            List<Point3D> points,
            boolean[] isPointCloud,
            boolean[] isRendering,
            float voxelSize,
            float isoLevel) {
        if (!isModelLoaded() || !isPointCloud[0] || points.isEmpty()) {
            System.out.println("Marching cubes cannot be applied: the point cloud is not loaded.");
            return;
        }

        try {
            MarchingCubes.Mesh marchingMesh = MarchingCubes.processPointCloud(points, voxelSize, isoLevel);
            System.out.println("Marching cubes generated a mesh with " + marchingMesh.vertices.size() +
                    " vertices and " + marchingMesh.faces.size() + " faces.");

            Model newModel = new Model();
            for (double[] vertex : marchingMesh.vertices) {
                newModel.vertices.add(new Point3D((float) vertex[0], (float) vertex[1], (float) vertex[2]));
            }
            for (int[] face : marchingMesh.faces) {
                Point3D p1 = newModel.vertices.get(face[0]);
                Point3D p2 = newModel.vertices.get(face[1]);
                Point3D p3 = newModel.vertices.get(face[2]);
                newModel.triangles.add(new Triangle(p1, p2, p3));
            }
            newModel.computeNormals();

            for (ModelLoader.Chunk chunk : chunks) {
                chunk.cleanup();
            }
            chunks.clear();
            points.clear();
            model = newModel;

            chunks.addAll(loader.createChunksFromData(model.toChunkData()));
            isPointCloud[0] = false;
            isRendering[0] = true;
            System.out.println("Created " + chunks.size() + " chunks for rendering.");
            renderer.cleanupPointsBuffers();

            updateCameraAndCenter(chunks, points);
            renderer.setObjectColor(objectColor[0], objectColor[1], objectColor[2]); // Применяем текущий цвет
        } catch (Exception e) {
            System.err.println("Marching cubes processing error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void removeNoise(
            List<ModelLoader.Chunk> chunks,
            boolean[] isPointCloud,
            boolean[] isModelLoaded) {

        if (!isModelLoaded() || model == null) return;
        if (isPointCloud[0]){
            System.out.println("The function is not applicable to the point cloud.");
        }
        else {
            System.out.println("Before removing the noise: vertices=" + model.vertices.size() + ", triangles=" + model.triangles.size());
            model.removeNoise(100);
            System.out.println("After removing the noise: vertices=" + model.vertices.size() + ", triangles=" + model.triangles.size());
            if (model.triangles.isEmpty()) {
                System.err.println("Error: the model is empty after removing the noise.");
                isModelLoaded[0] = false;
                chunks.clear();
            } else {
                for (ModelLoader.Chunk chunk : chunks) chunk.cleanup();
                chunks.clear();
                chunks.addAll(loader.createChunksFromData(model.toChunkData()));
                System.out.println("Noise removed. Chunks: " + chunks.size());
                renderer.cleanupPointsBuffers();
            }
        }
        renderer.setObjectColor(objectColor[0], objectColor[1], objectColor[2]); // Применяем
    }

    public void findLargestComponent(
            List<ModelLoader.Chunk> chunks,
            boolean[] isPointCloud) {
        if (!isModelLoaded() || model == null) return;

        if (isPointCloud[0]) {
            System.out.println("The function is not applicable to the point cloud.");
        } else {
            System.out.println("Before finding the largest object: vertices=" + model.vertices.size() + ", triangles=" + model.triangles.size());
            model.findLargestConnectedComponent();
            System.out.println("After finding the largest object: vertices=" + model.vertices.size() + ", triangles=" + model.triangles.size());
            for (ModelLoader.Chunk chunk : chunks) chunk.cleanup();
            chunks.clear();
            chunks.addAll(loader.createChunksFromData(model.toChunkData()));
            System.out.println("The main object is left on the stage. Chunks: " + chunks.size());
        }
        renderer.setObjectColor(objectColor[0], objectColor[1], objectColor[2]); // Применяем текущий цвет
    }

    public void smoothModel(List<ModelLoader.Chunk> chunks) {
        if (!isModelLoaded() || model == null) return;

        System.out.println("Before smoothing: vertices=" + model.vertices.size() + ", triangles=" + model.triangles.size());
        //model.gaussianSmooth(0.5f, 5);
        model.laplacianSmooth(0.05f, 5);
        System.out.println("After smoothing: vertices=" + model.vertices.size() + ", triangles=" + model.triangles.size());
        for (ModelLoader.Chunk chunk : chunks) chunk.cleanup();
        chunks.clear();
        chunks.addAll(loader.createChunksFromData(model.toChunkData()));
        System.out.println("Moded smoothed. Чанков: " + chunks.size());
        renderer.cleanupPointsBuffers();

        renderer.setObjectColor(objectColor[0], objectColor[1], objectColor[2]); // Применяем текущий цвет
    }

    public void smoothBoundaries(
            List<ModelLoader.Chunk> chunks,
            List<Point3D> points,
            boolean[] isPointCloud,
            boolean[] isModelLoaded) {
        if (!isModelLoaded() || model == null) {
            System.out.println("Cannot smooth boundaries: model not loaded.");
            return;
        }

        if (isPointCloud[0]) {
            System.out.println("Boundary smoothing not applicable to point clouds.");
            return;
        }

        System.out.println("Before boundary smoothing: vertices=" + model.vertices.size() + ", triangles=" + model.triangles.size());
        model.smoothBoundaries(0.6f, 5); // Умеренные параметры: фактор 0.5, 3 итерации
        System.out.println("After boundary smoothing: vertices=" + model.vertices.size() + ", triangles=" + model.triangles.size());

        if (model.triangles.isEmpty()) {
            System.err.println("Error: Model empty after boundary smoothing.");
            isModelLoaded[0] = false;
            chunks.clear();
        } else {
            for (ModelLoader.Chunk chunk : chunks) chunk.cleanup();
            chunks.clear();
            chunks.addAll(loader.createChunksFromData(model.toChunkData()));
            System.out.println("Boundary smoothing applied. Chunks: " + chunks.size());
        }

        renderer.setObjectColor(objectColor[0], objectColor[1], objectColor[2]); // Сохраняем цвет
        updateCameraAndCenter(chunks, points); // Обновляем камеру
    }


    private void updateCameraAndCenter(List<ModelLoader.Chunk> chunks, List<Point3D> points) {
        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
        float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        float minZ = Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;

        if (!points.isEmpty()) {
            for (Point3D point : points) {
                minX = Math.min(minX, point.x);
                maxX = Math.max(maxX, point.x);
                minY = Math.min(minY, point.y);
                maxY = Math.max(maxY, point.y);
                minZ = Math.min(minZ, point.z);
                maxZ = Math.max(maxZ, point.z);
            }
        } else {
            ModelLoader.AABB globalAABB = loader.getGlobalAABB(chunks);
            Vector3f center = globalAABB.center();
            minX = center.x - globalAABB.extent();
            maxX = center.x + globalAABB.extent();
            minY = center.y;
            maxY = center.y + globalAABB.extent();
            minZ = center.z - globalAABB.extent();
            maxZ = center.z + globalAABB.extent();
        }

        objectCenter.set((minX + maxX) / 2.0f, (minY + maxY) / 2.0f, (minZ + maxZ) / 2.0f);
        float extent = Math.max(maxX - minX, Math.max(maxY - minY, maxZ - minZ)) / 2.0f;
        camera.setPosition(new Vector3f(objectCenter.x, objectCenter.y, objectCenter.z + extent * 1.5f));
        renderer.updateGridSize(extent);
        System.out.println("Model loaded: camera centered at: " + camera.getPosition() + ", size: " + extent);
    }

    public void setObjectColor(float r, float g, float b) {
        objectColor[0] = r;
        objectColor[1] = g;
        objectColor[2] = b;
        renderer.setObjectColor(r, g, b);
        System.out.println("Color changed to: R=" + r + ", G=" + g + ", B=" + b);
    }

    public void clampModelPitch() {
        modelPitch[0] = Math.max(-89.0f, Math.min(89.0f, modelPitch[0]));
    }

    public void toggleCameraProjection() {
        camera.toggleProjection();
    }

    public float getModelYaw() {
        return modelYaw[0];
    }

    public float getModelPitch() {
        return modelPitch[0];
    }

    public Vector3f getObjectCenter() {
        return objectCenter;
    }

    public void updateModelYaw(float delta) {
        modelYaw[0] += delta;
    }

    public void updateModelPitch(float delta) {
        modelPitch[0] += delta;
    }
    public boolean isExporting() {
        return isExporting;
    }

    public String getExportStatus() {
        return exportStatus;
    }

    private boolean isModelLoaded() {
        return model != null && (!model.vertices.isEmpty() || !model.triangles.isEmpty());
    }

    public float[] getObjectColor() {
        return objectColor;
    }

    public void cleanup() {
        renderer.cleanup();
    }

}