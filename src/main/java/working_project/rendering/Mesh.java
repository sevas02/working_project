package working_project.rendering;

import org.lwjgl.BufferUtils;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class Mesh {
    private int vao, vbo, ebo, indexCount;

    public Mesh(float[] vertices, int[] indices) {
        vao = glGenVertexArrays(); glBindVertexArray(vao);
        vbo = glGenBuffers(); glBindBuffer(GL_ARRAY_BUFFER, vbo);
        FloatBuffer vb = BufferUtils.createFloatBuffer(vertices.length); vb.put(vertices).flip();
        glBufferData(GL_ARRAY_BUFFER, vb, GL_STATIC_DRAW);
        glVertexAttribPointer(0,3,GL_FLOAT,false,6*4,0); glEnableVertexAttribArray(0);
        glVertexAttribPointer(1,3,GL_FLOAT,false,6*4,3*4); glEnableVertexAttribArray(1);
        ebo = glGenBuffers(); glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        IntBuffer ib = BufferUtils.createIntBuffer(indices.length); ib.put(indices).flip();
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, ib, GL_STATIC_DRAW);
        indexCount = indices.length;
        glBindVertexArray(0);
    }

    public void render(int mode) {
        glBindVertexArray(vao);
        glDrawElements(mode, indexCount, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
    }

    public void render() {
        render(GL_TRIANGLES);
    }

    public void cleanup() {
        glDeleteVertexArrays(vao);
        glDeleteBuffers(vbo);
        glDeleteBuffers(ebo);
    }
}
