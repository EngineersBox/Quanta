package com.engineersbox.quanta.rendering.indirect;

import com.engineersbox.quanta.resources.assets.object.MeshData;
import com.engineersbox.quanta.resources.assets.object.Model;
import com.engineersbox.quanta.resources.assets.object.animation.AnimatedFrame;
import com.engineersbox.quanta.resources.assets.object.animation.Animation;
import com.engineersbox.quanta.scene.Entity;
import com.engineersbox.quanta.scene.Scene;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BUFFER;

public class RenderBuffers {

    private int staticVaoId;
    private int animVaoId;
    private int bindingPosesBuffer;
    private int bonesIndicesWeightsBuffer;
    private int bonesMatricesBuffer;
    private int destAnimationBuffer;
    private final List<Integer> vboIdList;

    public RenderBuffers() {
        this.vboIdList = new ArrayList<>();
    }

    public int getAnimVaoId() {
        return this.animVaoId;
    }

    public int getBindingPosesBuffer() {
        return this.bindingPosesBuffer;
    }

    public int getBonesIndicesWeightsBuffer() {
        return this.bonesIndicesWeightsBuffer;
    }

    public int getBonesMatricesBuffer() {
        return this.bonesMatricesBuffer;
    }

    public int getDestAnimationBuffer() {
        return this.destAnimationBuffer;
    }

    public void cleanup() {
        this.vboIdList.forEach(GL30::glDeleteBuffers);
        glDeleteVertexArrays(this.staticVaoId);
        glDeleteVertexArrays(this.animVaoId);
    }

    public final int getStaticVaoId() {
        return this.staticVaoId;
    }

    public void loadAnimatedModels(final Scene scene) {
        final List<Model> modelList = scene.getModels()
                .values()
                .stream()
                .filter(Model::isAnimated)
                .toList();
        loadBindingPoses(modelList);
        loadBonesMatricesBuffer(modelList);
        loadBonesIndicesWeights(modelList);

        this.animVaoId = glGenVertexArrays();
        glBindVertexArray(this.animVaoId);
        int positionsSize = 0;
        int normalsSize = 0;
        int textureCoordsSize = 0;
        int indicesSize = 0;
        int offset = 0;
        int chunkBindingPoseOffset = 0;
        int bindingPoseOffset = 0;
        int chunkWeightsOffset = 0;
        int weightsOffset = 0;
        for (final Model model : modelList) {
            final List<Entity> entities = model.getEntities();
            for (final Entity entity : entities) {
                final List<MeshDrawData> meshDrawDataList = model.getMeshDrawDataList();
                bindingPoseOffset = chunkBindingPoseOffset;
                weightsOffset = chunkWeightsOffset;
                for (final MeshData meshData : model.getMeshDataList()) {
                    positionsSize += meshData.getPositions().length;
                    normalsSize += meshData.getNormals().length;
                    textureCoordsSize += meshData.getTextCoords().length;
                    indicesSize += meshData.getIndices().length;

                    final int meshSizeInBytes = (meshData.getPositions().length + meshData.getNormals().length * 3 + meshData.getTextCoords().length) * 4;
                    meshDrawDataList.add(new MeshDrawData(
                            meshSizeInBytes,
                            meshData.getMaterialIdx(),
                            offset,
                            meshData.getIndices().length,
                            new AnimMeshDrawData(
                                    entity,
                                    bindingPoseOffset,
                                    weightsOffset
                            )
                    ));
                    bindingPoseOffset += meshSizeInBytes / 4;
                    final int groupSize = (int) Math.ceil((float) meshSizeInBytes / (14 * 4));
                    weightsOffset += groupSize * 2 * 4;
                    offset = positionsSize / 3;
                }
            }
            chunkBindingPoseOffset += bindingPoseOffset;
            chunkWeightsOffset += weightsOffset;
        }

        this.destAnimationBuffer = glGenBuffers();
        this.vboIdList.add(this.destAnimationBuffer);
        final FloatBuffer meshesBuffer = MemoryUtil.memAllocFloat(positionsSize + normalsSize * 3 + textureCoordsSize);
        for (final Model model : modelList) {
            model.getEntities().forEach(e -> {
                for (final MeshData meshData : model.getMeshDataList()) {
                    populateMeshBuffer(meshesBuffer, meshData);
                }
            });
        }
        meshesBuffer.flip();
        glBindBuffer(GL_ARRAY_BUFFER, this.destAnimationBuffer);
        glBufferData(GL_ARRAY_BUFFER, meshesBuffer, GL_STATIC_DRAW);
        MemoryUtil.memFree(meshesBuffer);
        defineVertexAttribs();

        // Index VBO
        final int vboId = glGenBuffers();
        this.vboIdList.add(vboId);
        final IntBuffer indicesBuffer = MemoryUtil.memAllocInt(indicesSize);
        for (final Model model : modelList) {
            model.getEntities().forEach(e -> {
                for (final MeshData meshData : model.getMeshDataList()) {
                    indicesBuffer.put(meshData.getIndices());
                }
            });
        }
        indicesBuffer.flip();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vboId);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL_STATIC_DRAW);
        MemoryUtil.memFree(indicesBuffer);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    private void loadBindingPoses(final List<Model> modelList) {
        int meshSize = 0;
        for (final Model model : modelList) {
            for (final MeshData meshData : model.getMeshDataList()) {
                meshSize += meshData.getPositions().length + meshData.getNormals().length * 3 +
                        meshData.getTextCoords().length + meshData.getIndices().length;
            }
        }

        this.bindingPosesBuffer = glGenBuffers();
        this.vboIdList.add(this.bindingPosesBuffer);
        final FloatBuffer meshesBuffer = MemoryUtil.memAllocFloat(meshSize);
        for (final Model model : modelList) {
            for (final MeshData meshData : model.getMeshDataList()) {
                populateMeshBuffer(meshesBuffer, meshData);
            }
        }
        meshesBuffer.flip();
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, this.bindingPosesBuffer);
        glBufferData(GL_SHADER_STORAGE_BUFFER, meshesBuffer, GL_STATIC_DRAW);
        MemoryUtil.memFree(meshesBuffer);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    private void loadBonesMatricesBuffer(final List<Model> modelList) {
        int bufferSize = 0;
        for (final Model model : modelList) {
            final List<Animation> animationsList = model.getAnimationList();
            for (final Animation animation : animationsList) {
                final List<AnimatedFrame> frameList = animation.frames();
                for (final AnimatedFrame frame : frameList) {
                    final Matrix4f[] matrices = frame.getBoneMatrices();
                    bufferSize += matrices.length * 64;
                }
            }
        }
        this.bonesMatricesBuffer = glGenBuffers();
        this.vboIdList.add(this.bonesMatricesBuffer);
        final ByteBuffer dataBuffer = MemoryUtil.memAlloc(bufferSize);
        final int matrixSize = 4 * 4 * 4;
        for (final Model model : modelList) {
            final List<Animation> animationsList = model.getAnimationList();
            for (final Animation animation : animationsList) {
                final List<AnimatedFrame> frameList = animation.frames();
                for (final AnimatedFrame frame : frameList) {
                    frame.setOffset(dataBuffer.position() / matrixSize);
                    final Matrix4f[] matrices = frame.getBoneMatrices();
                    for (final Matrix4f matrix : matrices) {
                        matrix.get(dataBuffer);
                        dataBuffer.position(dataBuffer.position() + matrixSize);
                    }
                    frame.clearData();
                }
            }
        }
        dataBuffer.flip();
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, this.bonesMatricesBuffer);
        glBufferData(GL_SHADER_STORAGE_BUFFER, dataBuffer, GL_STATIC_DRAW);
        MemoryUtil.memFree(dataBuffer);
    }

    private void loadBonesIndicesWeights(final List<Model> modelList) {
        int bufferSize = 0;
        for (final Model model : modelList) {
            for (final MeshData meshData : model.getMeshDataList()) {
                bufferSize += meshData.getBoneIndices().length * 4 + meshData.getWeights().length * 4;
            }
        }
        final ByteBuffer dataBuffer = MemoryUtil.memAlloc(bufferSize);
        for (final Model model : modelList) {
            for (final MeshData meshData : model.getMeshDataList()) {
                final int[] bonesIndices = meshData.getBoneIndices();
                final float[] weights = meshData.getWeights();
                final int rows = bonesIndices.length / 4;
                for (int row = 0; row < rows; row++) {
                    final int startPos = row * 4;
                    dataBuffer.putFloat(weights[startPos]);
                    dataBuffer.putFloat(weights[startPos + 1]);
                    dataBuffer.putFloat(weights[startPos + 2]);
                    dataBuffer.putFloat(weights[startPos + 3]);
                    dataBuffer.putFloat(bonesIndices[startPos]);
                    dataBuffer.putFloat(bonesIndices[startPos + 1]);
                    dataBuffer.putFloat(bonesIndices[startPos + 2]);
                    dataBuffer.putFloat(bonesIndices[startPos + 3]);
                }
            }
        }
        dataBuffer.flip();
        this.bonesIndicesWeightsBuffer = glGenBuffers();
        this.vboIdList.add(this.bonesIndicesWeightsBuffer);
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, this.bonesIndicesWeightsBuffer);
        glBufferData(GL_SHADER_STORAGE_BUFFER, dataBuffer, GL_STATIC_DRAW);
        MemoryUtil.memFree(dataBuffer);
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
    }

    public void loadStaticModels(final Scene scene) {
        final List<Model> modelList = scene.getModels()
                .values()
                .stream()
                .filter(Predicate.not(Model::isAnimated))
                .toList();
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
