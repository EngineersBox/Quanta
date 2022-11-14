package com.engineersbox.quanta.resources.assets.object;

import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.*;

public class QuadMesh {

    private final int vertexCount;
    private final int vaoId;
    private final List<Integer> vboIds;

    public QuadMesh() {
        try (final MemoryStack stack = MemoryStack.stackPush()) {
            this.vboIds = new ArrayList<>();
            final float[] positions = new float[]{
                    -1.0f, 1.0f, 0.0f,
                    1.0f, 1.0f, 0.0f,
                    -1.0f, -1.0f, 0.0f,
                    1.0f, -1.0f, 0.0f,
            };
            final float[] textCoords = new float[]{
                    0.0f, 1.0f,
                    1.0f, 1.0f,
                    0.0f, 0.0f,
                    1.0f, 0.0f,
            };
            final int[] indices = new int[]{0, 2, 1, 1, 2, 3};
            this.vertexCount = indices.length;

            this.vaoId = glGenVertexArrays();
            glBindVertexArray(this.vaoId);

            // Positions
            int vboId = glGenBuffers();
            this.vboIds.add(vboId);
            final FloatBuffer positionsBuffer = stack.callocFloat(positions.length);
            positionsBuffer.put(0, positions);
            glBindBuffer(GL_ARRAY_BUFFER, vboId);
            glBufferData(GL_ARRAY_BUFFER, positionsBuffer, GL_STATIC_DRAW);
            glEnableVertexAttribArray(0);
            glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);

            // Texture coordinates
            vboId = glGenBuffers();
            this.vboIds.add(vboId);
            final FloatBuffer textCoordsBuffer = MemoryUtil.memAllocFloat(textCoords.length);
            textCoordsBuffer.put(0, textCoords);
            glBindBuffer(GL_ARRAY_BUFFER, vboId);
            glBufferData(GL_ARRAY_BUFFER, textCoordsBuffer, GL_STATIC_DRAW);
            glEnableVertexAttribArray(1);
            glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);

            // Index
            vboId = glGenBuffers();
            this.vboIds.add(vboId);
            final IntBuffer indicesBuffer = stack.callocInt(indices.length);
            indicesBuffer.put(0, indices);
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vboId);
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL_STATIC_DRAW);

            glBindBuffer(GL_ARRAY_BUFFER, 0);
            glBindVertexArray(0);
        }
    }

    public void cleanup() {
        this.vboIds.forEach(GL30::glDeleteBuffers);
        glDeleteVertexArrays(this.vaoId);
    }

    public int getVertexCount() {
        return this.vertexCount;
    }

    public int getVaoId() {
        return this.vaoId;
    }

}
