package com.engineersbox.quanta.resources.assets.object;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
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
        this(
                positions,
                normals,
                tangents,
                biTangents,
                textCoords,
                indices,
                boneIndices,
                weights,
                aabbMin,
                aabbMax,
                0
        );
    }

    @JsonCreator
    public MeshData(@JsonProperty("positions") final float[] positions,
                    @JsonProperty("normals") final float[] normals,
                    @JsonProperty("tangents") final float[] tangents,
                    @JsonProperty("bi_tangents") final float[] biTangents,
                    @JsonProperty("texture_coordinates") final float[] textCoords,
                    @JsonProperty("indices") final int[] indices,
                    @JsonProperty("bone_indices") final int[] boneIndices,
                    @JsonProperty("weights") final float[] weights,
                    @JsonProperty("aabb_min") final Vector3f aabbMin,
                    @JsonProperty("aabb_max") final Vector3f aabbMax,
                    @JsonProperty("material_index") final int materialIdx) {
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
        this.materialIdx = materialIdx;
    }

    @JsonProperty("aabb_max")
    public Vector3f getAabbMax() {
        return this.aabbMax;
    }

    @JsonProperty("aabb_min")
    public Vector3f getAabbMin() {
        return this.aabbMin;
    }

    @JsonProperty("bi_tangents")
    public float[] getBiTangents() {
        return this.biTangents;
    }

    @JsonProperty("bone_indices")
    public int[] getBoneIndices() {
        return this.boneIndices;
    }

    @JsonProperty("indices")
    public int[] getIndices() {
        return this.indices;
    }

    @JsonProperty("material_index")
    public int getMaterialIdx() {
        return this.materialIdx;
    }

    @JsonProperty("normals")
    public float[] getNormals() {
        return this.normals;
    }

    @JsonProperty("positions")
    public float[] getPositions() {
        return this.positions;
    }

    @JsonProperty("tangents")
    public float[] getTangents() {
        return this.tangents;
    }

    @JsonProperty("texture_coordinates")
    public float[] getTextCoords() {
        return this.textCoords;
    }

    @JsonProperty("weights")
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
