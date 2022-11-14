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
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public class Mesh {

    public static final int MAX_WEIGHTS = 4;

    private final int vertexCount;
    private final int vaoId;
    private final List<Integer> vboIds;

    public Mesh(final float[] positions,
                final float[] normals,
                final float[] tangents,
                final float[] biTangents,
                final float[] textCoords,
                final int[] indices,
                final int[] boneIndices,
                final float[] weights) {
        try (final MemoryStack stack = MemoryStack.stackPush()) {
            this.vertexCount = indices.length;
            this.vboIds = new ArrayList<>();
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

            // Normals
            vboId = glGenBuffers();
            this.vboIds.add(vboId);
            final FloatBuffer normalsBuffer = stack.callocFloat(normals.length);
            normalsBuffer.put(0, normals);
            glBindBuffer(GL_ARRAY_BUFFER, vboId);
            glBufferData(GL_ARRAY_BUFFER, normalsBuffer, GL_STATIC_DRAW);
            glEnableVertexAttribArray(1);
            glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0);

            // Tangents
            vboId = glGenBuffers();
            this.vboIds.add(vboId);
            FloatBuffer tangentsBuffer = stack.callocFloat(tangents.length);
            tangentsBuffer.put(0, tangents);
            glBindBuffer(GL_ARRAY_BUFFER, vboId);
            glBufferData(GL_ARRAY_BUFFER, tangentsBuffer, GL_STATIC_DRAW);
            glEnableVertexAttribArray(2);
            glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);

            // BiTangents
            vboId = glGenBuffers();
            this.vboIds.add(vboId);
            FloatBuffer bitangentsBuffer = stack.callocFloat(biTangents.length);
            bitangentsBuffer.put(0, biTangents);
            glBindBuffer(GL_ARRAY_BUFFER, vboId);
            glBufferData(GL_ARRAY_BUFFER, bitangentsBuffer, GL_STATIC_DRAW);
            glEnableVertexAttribArray(3);
            glVertexAttribPointer(3, 3, GL_FLOAT, false, 0, 0);

            // Texture coordinates
            vboId = glGenBuffers();
            this.vboIds.add(vboId);
            final FloatBuffer textCoordsBuffer = MemoryUtil.memAllocFloat(textCoords.length);
            textCoordsBuffer.put(0, textCoords);
            glBindBuffer(GL_ARRAY_BUFFER, vboId);
            glBufferData(GL_ARRAY_BUFFER, textCoordsBuffer, GL_STATIC_DRAW);
            glEnableVertexAttribArray(4);
            glVertexAttribPointer(4, 2, GL_FLOAT, false, 0, 0);

            // Bone weights
            vboId = glGenBuffers();
            this.vboIds.add(vboId);
            FloatBuffer weightsBuffer = MemoryUtil.memAllocFloat(weights.length);
            weightsBuffer.put(weights).flip();
            glBindBuffer(GL_ARRAY_BUFFER, vboId);
            glBufferData(GL_ARRAY_BUFFER, weightsBuffer, GL_STATIC_DRAW);
            glEnableVertexAttribArray(5);
            glVertexAttribPointer(5, 4, GL_FLOAT, false, 0, 0);

            // Bone indices
            vboId = glGenBuffers();
            this.vboIds.add(vboId);
            IntBuffer boneIndicesBuffer = MemoryUtil.memAllocInt(boneIndices.length);
            boneIndicesBuffer.put(boneIndices).flip();
            glBindBuffer(GL_ARRAY_BUFFER, vboId);
            glBufferData(GL_ARRAY_BUFFER, boneIndicesBuffer, GL_STATIC_DRAW);
            glEnableVertexAttribArray(6);
            glVertexAttribPointer(6, 4, GL_FLOAT, false, 0, 0);

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

    public int getVertexCount() {
        return this.vertexCount;
    }

    public int getVaoId() {
        return this.vaoId;
    }

    public void cleanup() {
        this.vboIds.forEach(GL30::glDeleteBuffers);
    }

}
