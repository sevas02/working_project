package working_project.rendering;

import org.lwjgl.opengl.GL32C;
import working_project.geometry.Point3D;
import working_project.core.Camera;
import working_project.core.ShaderProgram;
import working_project.core.WindowManager;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.*;


/**
 * Низкоуровневый рендерер: шейдеры, VAO/VBO/EBO, смена режимов и пр.
 * Рисует сетку, оси и либо список Mesh, либо облако точек.
 */
public class Renderer {
    private final WindowManager window;
    private final Camera camera;

    private ShaderProgram modelShader, triangleShader, gridShader;
    private Mesh gridMesh, axisMesh;

    private final List<Mesh> meshes = new ArrayList<>();
    private List<Point3D> pointCloud;
    private int pointsVao = 0, pointsVbo = 0, pointCount = 0;

    private float yaw = 0, pitch = 0;
    private float objectColorR = 0.3f, objectColorG = 0.3f, objectColorB = 0.3f;

    // Центр модели и коэффициент нормализации
    private Vector3f modelCenter = new Vector3f(0, 0, 0);
    private float modelScale = 1.0f;

    public Renderer(WindowManager window, Camera camera) {
        this.window = window;
        this.camera = camera;
    }

    /** Инициализация шейдеров, сетки и осей */
    public void init() {
        initShaders();
        buildGridAndAxes();
        glDisable(GL_CULL_FACE);
    }

    private void initShaders() {
        // Шейдер для точек
        String pV = "#version 410 core\n" +
                "layout(location=0) in vec3 aPos;\n" +
                "uniform mat4 model,view,projection; uniform float pointSizeScale;\n" +
                "void main(){ vec4 pos=projection*view*model*vec4(aPos,1); gl_Position=pos; float d=length(view*model*vec4(aPos,1)); gl_PointSize=clamp(pointSizeScale/d,5.0,20.0);} ";
        String pF = "#version 410 core\n" +
                "out vec4 FragColor; uniform vec3 objectColor;\n" +
                "void main(){ vec2 c=gl_PointCoord-vec2(0.5); if(length(c)>0.5) discard; FragColor=vec4(objectColor,1);} ";
        modelShader = new ShaderProgram(pV, pF);

        // Шейдер для треугольников
        String tV = "#version 410 core\n" +
                "layout(location=0) in vec3 aPos; layout(location=1) in vec3 aNormal;\n" +
                "out vec3 FragPos, Normal; uniform mat4 model,view,projection;\n" +
                "void main(){ FragPos=vec3(model*vec4(aPos,1)); Normal=mat3(transpose(inverse(model)))*aNormal; gl_Position=projection*view*model*vec4(aPos,1);} ";
        String tF = "#version 410 core\n" +
                "in vec3 FragPos, Normal; out vec4 FragColor;\n" +
                "uniform vec3 lightPos,viewPos,lightColor,objectColor,selectedColor;\n" +
                "void main(){ if(selectedColor!=vec3(0)){FragColor=vec4(selectedColor,1);return;} vec3 norm=normalize(Normal); vec3 viewDir=normalize(viewPos-FragPos); vec3 base=dot(norm,viewDir)>0?objectColor:vec3(0.5); vec3 ln=dot(norm,viewDir)>0?norm:-norm; vec3 ambient=0.1*lightColor; vec3 diff= max(dot(ln,normalize(lightPos-FragPos)),0.0)*lightColor; vec3 spec=pow(max(dot(viewDir,reflect(-normalize(lightPos-FragPos),ln)),0.0),32)*0.5*lightColor; FragColor=vec4((ambient+diff+spec)*base,1);} ";
        triangleShader = new ShaderProgram(tV, tF);

        // Шейдер для сетки и осей
        String gV = "#version 410 core\n" +
                "layout(location=0) in vec3 aPos; layout(location=1) in vec3 aColor; out vec3 Color; uniform mat4 model,view,projection; uniform float scale;\n" +
                "void main(){ gl_Position=projection*view*model*vec4(aPos*scale,1); Color=aColor;} ";
        String gF = "#version 410 core\n" +
                "in vec3 Color; out vec4 FragColor; void main(){ FragColor=vec4(Color,1);} ";
        gridShader = new ShaderProgram(gV, gF);
    }

    private void buildGridAndAxes() {
        // сетка
        List<Float> gv = new ArrayList<>();
        List<Integer> gi = new ArrayList<>();
        int idx = 0;
        for (int x = -50; x <= 50; x++) {
            gv.add((float)x); gv.add(0f); gv.add(-50f); gv.add(0.3f); gv.add(0.3f); gv.add(0.3f);
            gv.add((float)x); gv.add(0f); gv.add(50f);  gv.add(0.3f); gv.add(0.3f); gv.add(0.3f);
            gv.add(-50f);      gv.add(0f); gv.add((float)x); gv.add(0.3f); gv.add(0.3f); gv.add(0.3f);
            gv.add(50f);       gv.add(0f); gv.add((float)x); gv.add(0.3f); gv.add(0.3f); gv.add(0.3f);
            gi.add(idx++); gi.add(idx++); gi.add(idx++); gi.add(idx++);
        }
        float[] gva = new float[gv.size()]; for(int i=0;i<gv.size();i++) gva[i]=gv.get(i);
        int[] gia = new int[gi.size()]; for(int i=0;i<gi.size();i++) gia[i]=gi.get(i);
        gridMesh = new Mesh(gva, gia);
        // оси
        float[] ava = {
                -50f,0f,0f, 1f,0f,0f, 50f,0f,0f, 1f,0f,0f,
                0f,-50f,0f, 0f,1f,0f, 0f,50f,0f, 0f,1f,0f,
                0f,0f,-50f, 0f,0f,1f, 0f,0f,50f, 0f,0f,1f
        };
        int[] aia = {0,1,2,3,4,5};
        axisMesh = new Mesh(ava, aia);
    }

    public void clearMeshes() {
        for (Mesh m : meshes) m.cleanup();
        meshes.clear();
    }

    public void addMesh(Mesh mesh) {
        meshes.add(mesh);
    }

    public void clearPointCloud() {
        if (pointsVao != 0) { glDeleteVertexArrays(pointsVao); pointsVao = 0; }
        if (pointsVbo != 0) { glDeleteBuffers(pointsVbo); pointsVbo = 0; }
        pointCloud = null;
    }

    public void updatePointCloud(List<Point3D> points) {
        this.pointCloud = points;
        this.pointsVao = 0;
    }

    public void setOrientation(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public void setObjectColor(float r, float g, float b) {
        this.objectColorR = r;
        this.objectColorG = g;
        this.objectColorB = b;
        modelShader.use(); modelShader.setUniform3f("objectColor", r, g, b);
        triangleShader.use(); triangleShader.setUniform3f("objectColor", r, g, b);
    }

    /** Устанавливает центр и масштаб модели перед рендерингом */
    public void setModelTransform(Vector3f center, float scale) {
        this.modelCenter.set(center);
        this.modelScale = scale;
    }

    /** Основной метод рисования: сетка + оси + меши / точки */
    public void renderScene() {
        glClearColor(0.8f, 0.8f, 0.8f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glEnable(GL_DEPTH_TEST);

        // Модельная матрица: центрирование, масштаб, затем вращение
        Matrix4f modelMatrix = new Matrix4f()
                .translate(-modelCenter.x, -modelCenter.y, -modelCenter.z)
                .scale(modelScale)
                .rotateY((float)Math.toRadians(yaw))
                .rotateX((float)Math.toRadians(pitch));
        Matrix4f view = camera.getViewMatrix();
        Matrix4f proj = camera.getProjectionMatrix(
                (float)window.getWidth()/window.getHeight(), 0.1f, 1000f);

        FloatBuffer mb = BufferUtils.createFloatBuffer(16);
        FloatBuffer vb = BufferUtils.createFloatBuffer(16);
        FloatBuffer pb = BufferUtils.createFloatBuffer(16);
        modelMatrix.get(mb);
        view.get(vb);
        proj.get(pb);

        // Рисуем сетку и оси в мировых координатах
        gridShader.use();
        gridShader.setUniformMatrix4fv("model", mb);
        gridShader.setUniformMatrix4fv("view", vb);
        gridShader.setUniformMatrix4fv("projection", pb);
        gridShader.setUniform1f("scale", 1.0f);
        glLineWidth(1f);
        glDepthFunc(GL_LEQUAL);
        gridMesh.render(GL_LINES);
        axisMesh.render(GL_LINES);
        glDepthFunc(GL_LESS);

        if (!meshes.isEmpty()) {
            triangleShader.use();
            triangleShader.setUniformMatrix4fv("model", mb);
            triangleShader.setUniformMatrix4fv("view", vb);
            triangleShader.setUniformMatrix4fv("projection", pb);
            triangleShader.setUniform3f("lightPos", 10, 10, 10);
            triangleShader.setUniform3f("lightColor", 1, 1, 1);
            Vector3f camPos = camera.getPosition();
            triangleShader.setUniform3f("viewPos", camPos.x, camPos.y, camPos.z);
            triangleShader.setUniform3f("selectedColor", 0, 0, 0);
            for (Mesh m : meshes) m.render();
        } else if (pointCloud != null) {
            modelShader.use();
            modelShader.setUniformMatrix4fv("model", mb);
            modelShader.setUniformMatrix4fv("view", vb);
            modelShader.setUniformMatrix4fv("projection", pb);
            modelShader.setUniform1f("pointSizeScale", 10.0f);
            if (pointsVao == 0) initPointBuffers();
            glEnable(GL32C.GL_PROGRAM_POINT_SIZE);
            glBindVertexArray(pointsVao);
            glDrawArrays(GL_POINTS, 0, pointCloud.size());
            glBindVertexArray(0);
            glDepthFunc(GL_LESS);
        }
    }

    private void initPointBuffers() {
        pointsVao = glGenVertexArrays();
        pointsVbo = glGenBuffers();
        glBindVertexArray(pointsVao);
        glBindBuffer(GL_ARRAY_BUFFER, pointsVbo);
        int count = pointCloud.size();
        float[] data = new float[count*3];
        for (int i = 0; i < count; i++) {
            Point3D p = pointCloud.get(i);
            data[i*3]   = (p.x - modelCenter.x) * modelScale;
            data[i*3+1] = (p.y - modelCenter.y) * modelScale;
            data[i*3+2] = (p.z - modelCenter.z) * modelScale;
        }
        glBufferData(GL_ARRAY_BUFFER, data, GL_STATIC_DRAW);
        glVertexAttribPointer(0,3,GL_FLOAT,false,3*4,0);
        glEnableVertexAttribArray(0);
        glBindVertexArray(0);
    }

    public void cleanup() {
        modelShader.cleanup();
        triangleShader.cleanup();
        gridShader.cleanup();
        gridMesh.cleanup();
        axisMesh.cleanup();
        clearMeshes();
        clearPointCloud();
    }
}

