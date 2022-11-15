package com.engineersbox.quanta.resources.assets.object;

import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class MeshData {

    private final Vector3f aabbMax;
    private final Vector3f aabbMin;
    private final float[] biTangents;
    private final int[] boneIndices;
    private final int[] indices;
    private int materialIdx;
    private final float[] normals;
    private final float[] positions;
    private final float[] tangents;
    private final float[] textCoords;
    private final float[] weights;

    public MeshData(final float[] positions,
                    final float[] normals,
                    final float[] tangents,
                    final float[] biTangents,
                    final float[] textCoords,
                    final int[] indices,
                    final int[] boneIndices,
                    final float[] weights,
                    final Vector3f aabbMin,
                    final Vector3f aabbMax) {
        this.materialIdx = 0;
        this.positions = positions;
        this.normals = normals;
        this.tangents = tangents;
        this.biTangents = biTangents;
        this.textCoords = textCoords;
        this.indices = indices;
        this.boneIndices = boneIndices;
        this.weights = weights;
        this.aabbMin = aabbMin;
        this.aabbMax = aabbMax;
    }

    public Vector3f getAabbMax() {
        return this.aabbMax;
    }

    public Vector3f getAabbMin() {
        return this.aabbMin;
    }

    public float[] getBiTangents() {
        return this.biTangents;
    }

    public int[] getBoneIndices() {
        return this.boneIndices;
    }

    public int[] getIndices() {
        return this.indices;
    }

    public int getMaterialIdx() {
        return this.materialIdx;
    }

    public float[] getNormals() {
        return this.normals;
    }

    public float[] getPositions() {
        return this.positions;
    }

    public float[] getTangents() {
        return this.tangents;
    }

    public float[] getTextCoords() {
        return this.textCoords;
    }

    public float[] getWeights() {
        return this.weights;
    }

    public void setMaterialIdx(final int materialIdx) {
        this.materialIdx = materialIdx;
    }

    public List<Vector3f> getGroupedVertices() {
        final List<Vector3f> vertices = new ArrayList<>();
        for (int i = 0; i < this.positions.length; i += 3) {
            vertices.add(new Vector3f(
                    this.positions[i],
                    this.positions[i + 1],
                    this.positions[i + 2]
            ));
        }
        return vertices;
    }

    public List<Vector3f> getGroupedNormals() {
        final List<Vector3f> groupedNormals = new ArrayList<>();
        for (int i = 0; i < this.normals.length; i += 3) {
            groupedNormals.add(new Vector3f(
                    this.normals[i],
                    this.normals[i + 1],
                    this.normals[i + 2]
            ));
        }
        return groupedNormals;
    }

    public int triangleCount() {
        return this.indices.length / 3;
    }

    public int vertexCount() {
        return this.indices.length;
    }

}
