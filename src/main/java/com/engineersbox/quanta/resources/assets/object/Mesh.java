package com.engineersbox.quanta.resources.assets.object;

import org.joml.Vector3f;
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

public class Mesh {

    public static final int MAX_WEIGHTS = 4;
    private final Vector3f aabbMax;
    private final Vector3f aabbMin;
    private final int numVertices;
    private final int vaoId;
    private final List<Integer> vboIds;

    public Mesh(final MeshData meshData) {
        try (final MemoryStack stack = MemoryStack.stackPush()) {
            this.aabbMin = meshData.getAabbMin();
            this.aabbMax = meshData.getAabbMax();
            this.numVertices = meshData.getIndices().length;
            this.vboIds = new ArrayList<>();

            this.vaoId = glGenVertexArrays();
            glBindVertexArray(this.vaoId);

            // Positions
            int vboId = glGenBuffers();
            this.vboIds.add(vboId);
            final FloatBuffer positionsBuffer = stack.callocFloat(meshData.getPositions().length);
            positionsBuffer.put(0, meshData.getPositions());
            glBindBuffer(GL_ARRAY_BUFFER, vboId);
            glBufferData(GL_ARRAY_BUFFER, positionsBuffer, GL_STATIC_DRAW);
            glEnableVertexAttribArray(0);
            glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);

            // Normals
            vboId = glGenBuffers();
            this.vboIds.add(vboId);
            final FloatBuffer normalsBuffer = stack.callocFloat(meshData.getNormals().length);
            normalsBuffer.put(0, meshData.getNormals());
            glBindBuffer(GL_ARRAY_BUFFER, vboId);
            glBufferData(GL_ARRAY_BUFFER, normalsBuffer, GL_STATIC_DRAW);
            glEnableVertexAttribArray(1);
            glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0);

            // Tangents
            vboId = glGenBuffers();
            this.vboIds.add(vboId);
            final FloatBuffer tangentsBuffer = stack.callocFloat(meshData.getTangents().length);
            tangentsBuffer.put(0, meshData.getTangents());
            glBindBuffer(GL_ARRAY_BUFFER, vboId);
            glBufferData(GL_ARRAY_BUFFER, tangentsBuffer, GL_STATIC_DRAW);
            glEnableVertexAttribArray(2);
            glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);

            // BiTangents
            vboId = glGenBuffers();
            this.vboIds.add(vboId);
            final FloatBuffer biTangentsBuffer = stack.callocFloat(meshData.getBiTangents().length);
            biTangentsBuffer.put(0, meshData.getBiTangents());
            glBindBuffer(GL_ARRAY_BUFFER, vboId);
            glBufferData(GL_ARRAY_BUFFER, biTangentsBuffer, GL_STATIC_DRAW);
            glEnableVertexAttribArray(3);
            glVertexAttribPointer(3, 3, GL_FLOAT, false, 0, 0);

            // Texture coordinates
            vboId = glGenBuffers();
            this.vboIds.add(vboId);
            final FloatBuffer textCoordsBuffer = MemoryUtil.memAllocFloat(meshData.getTextCoords().length);
            textCoordsBuffer.put(0, meshData.getTextCoords());
            glBindBuffer(GL_ARRAY_BUFFER, vboId);
            glBufferData(GL_ARRAY_BUFFER, textCoordsBuffer, GL_STATIC_DRAW);
            glEnableVertexAttribArray(4);
            glVertexAttribPointer(4, 2, GL_FLOAT, false, 0, 0);

            // Bone weights
            vboId = glGenBuffers();
            this.vboIds.add(vboId);
            final FloatBuffer weightsBuffer = MemoryUtil.memAllocFloat(meshData.getWeights().length);
            weightsBuffer.put(meshData.getWeights()).flip();
            glBindBuffer(GL_ARRAY_BUFFER, vboId);
            glBufferData(GL_ARRAY_BUFFER, weightsBuffer, GL_STATIC_DRAW);
            glEnableVertexAttribArray(5);
            glVertexAttribPointer(5, 4, GL_FLOAT, false, 0, 0);

            // Bone indices
            vboId = glGenBuffers();
            this.vboIds.add(vboId);
            final IntBuffer boneIndicesBuffer = MemoryUtil.memAllocInt(meshData.getBoneIndices().length);
            boneIndicesBuffer.put(meshData.getBoneIndices()).flip();
            glBindBuffer(GL_ARRAY_BUFFER, vboId);
            glBufferData(GL_ARRAY_BUFFER, boneIndicesBuffer, GL_STATIC_DRAW);
            glEnableVertexAttribArray(6);
            glVertexAttribPointer(6, 4, GL_FLOAT, false, 0, 0);

            // Index
            vboId = glGenBuffers();
            this.vboIds.add(vboId);
            final IntBuffer indicesBuffer = stack.callocInt(meshData.getIndices().length);
            indicesBuffer.put(0, meshData.getIndices());
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

    public Vector3f getAabbMax() {
        return this.aabbMax;
    }

    public Vector3f getAabbMin() {
        return this.aabbMin;
    }

    public int getNumVertices() {
        return this.numVertices;
    }

    public final int getVaoId() {
        return this.vaoId;
    }

}
