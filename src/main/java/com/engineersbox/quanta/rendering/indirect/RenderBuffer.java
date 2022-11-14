package com.engineersbox.quanta.rendering.indirect;

import com.engineersbox.quanta.resources.assets.object.MeshData;
import com.engineersbox.quanta.resources.assets.object.Model;
import com.engineersbox.quanta.scene.Scene;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.*;

public class RenderBuffer {

    private int staticVaoId;
    private final List<Integer> vboIdList;

    public RenderBuffer() {
        this.vboIdList = new ArrayList<>();
    }

    public void cleanup() {
        this.vboIdList.forEach(GL30::glDeleteBuffers);
        glDeleteVertexArrays(this.staticVaoId);
    }

    public final int getStaticVaoId() {
        return this.staticVaoId;
    }

    public void loadAnimatedModels(final Scene scene) {
        // To be completed
    }

    public void loadStaticModels(final Scene scene) {
        final List<Model> modelList = scene.getModels().values().stream().filter(m -> !m.isAnimated()).toList();
        this.staticVaoId = glGenVertexArrays();
        glBindVertexArray(this.staticVaoId);
        int positionsSize = 0;
        int normalsSize = 0;
        int textureCoordsSize = 0;
        int indicesSize = 0;
        int offset = 0;
        for (final Model model : modelList) {
            final List<MeshDrawData> meshDrawDataList = model.getMeshDrawDataList();
            for (final MeshData meshData : model.getMeshDataList()) {
                positionsSize += meshData.getPositions().length;
                normalsSize += meshData.getNormals().length;
                textureCoordsSize += meshData.getTextCoords().length;
                indicesSize += meshData.getIndices().length;

                final int meshSizeInBytes = meshData.getPositions().length * 14 * 4;
                meshDrawDataList.add(new MeshDrawData(meshSizeInBytes, meshData.getMaterialIdx(), offset,
                        meshData.getIndices().length));
                offset = positionsSize / 3;
            }
        }

        int vboId = glGenBuffers();
        this.vboIdList.add(vboId);
        final FloatBuffer meshesBuffer = MemoryUtil.memAllocFloat(positionsSize + normalsSize * 3 + textureCoordsSize);
        for (final Model model : modelList) {
            for (final MeshData meshData : model.getMeshDataList()) {
                populateMeshBuffer(meshesBuffer, meshData);
            }
        }
        meshesBuffer.flip();
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBufferData(GL_ARRAY_BUFFER, meshesBuffer, GL_STATIC_DRAW);
        MemoryUtil.memFree(meshesBuffer);

        defineVertexAttribs();

        // Index VBO
        vboId = glGenBuffers();
        this.vboIdList.add(vboId);
        final IntBuffer indicesBuffer = MemoryUtil.memAllocInt(indicesSize);
        for (final Model model : modelList) {
            for (final MeshData meshData : model.getMeshDataList()) {
                indicesBuffer.put(meshData.getIndices());
            }
        }
        indicesBuffer.flip();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vboId);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL_STATIC_DRAW);
        MemoryUtil.memFree(indicesBuffer);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    private void populateMeshBuffer(final FloatBuffer meshesBuffer,
                                    final MeshData meshData) {
        final float[] positions = meshData.getPositions();
        final float[] normals = meshData.getNormals();
        final float[] tangents = meshData.getTangents();
        final float[] biTangents = meshData.getBiTangents();
        final float[] textCoords = meshData.getTextCoords();
        final int rows = positions.length / 3;
        for (int row = 0; row < rows; row++) {
            final int startPos = row * 3;
            final int startTextCoord = row * 2;
            meshesBuffer.put(positions[startPos]);
            meshesBuffer.put(positions[startPos + 1]);
            meshesBuffer.put(positions[startPos + 2]);
            meshesBuffer.put(normals[startPos]);
            meshesBuffer.put(normals[startPos + 1]);
            meshesBuffer.put(normals[startPos + 2]);
            meshesBuffer.put(tangents[startPos]);
            meshesBuffer.put(tangents[startPos + 1]);
            meshesBuffer.put(tangents[startPos + 2]);
            meshesBuffer.put(biTangents[startPos]);
            meshesBuffer.put(biTangents[startPos + 1]);
            meshesBuffer.put(biTangents[startPos + 2]);
            meshesBuffer.put(textCoords[startTextCoord]);
            meshesBuffer.put(textCoords[startTextCoord + 1]);
        }
    }

    private void defineVertexAttribs() {
        final int stride = 3 * 4 * 4 + 2 * 4;
        int pointer = 0;
        // Positions
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, pointer);
        pointer += 3 * 4;
        // Normals
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, pointer);
        pointer += 3 * 4;
        // Tangents
        glEnableVertexAttribArray(2);
        glVertexAttribPointer(2, 3, GL_FLOAT, false, stride, pointer);
        pointer += 3 * 4;
        // Bitangents
        glEnableVertexAttribArray(3);
        glVertexAttribPointer(3, 3, GL_FLOAT, false, stride, pointer);
        pointer += 3 * 4;
        // Texture coordinates
        glEnableVertexAttribArray(4);
        glVertexAttribPointer(4, 2, GL_FLOAT, false, stride, pointer);
    }

}
