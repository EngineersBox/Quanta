package com.engineersbox.quanta.resources.object;

import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public class Mesh {

    private final int vertexCount;
    private final int vaoId;
    private final List<Integer> vboIds;

    public Mesh(final float[] positions,
                final int vertexCount) {
        try (final MemoryStack stack = MemoryStack.stackPush()) {
            this.vertexCount = vertexCount;
            this.vboIds = new ArrayList<>();
            this.vaoId = glGenVertexArrays();
            glBindVertexArray(this.vaoId);

            final int vboId = glGenBuffers();
            this.vboIds.add(vboId);
            final FloatBuffer positionsBuffer = stack.callocFloat(positions.length);
            positionsBuffer.put(0, positions);
            glBindBuffer(GL_ARRAY_BUFFER, vboId);
            glBufferData(GL_ARRAY_BUFFER, positionsBuffer, GL_STATIC_DRAW);
            glEnableVertexAttribArray(0);
            glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);

            glBindBuffer(GL_ARRAY_BUFFER, 0);
            glBindVertexArray(0);
        }
    }

    public int getVetexCount() {
        return this.vertexCount;
    }

    public int getVaoId() {
        return this.vaoId;
    }

    public void cleanup() {
        this.vboIds.forEach(GL30::glDeleteBuffers);
    }

}
