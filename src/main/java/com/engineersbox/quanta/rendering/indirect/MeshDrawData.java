package com.engineersbox.quanta.rendering.indirect;

public record MeshDrawData(int sizeInBytes,
                           int materialIdx,
                           int offset,
                           int vertices,
                           AnimMeshDrawData animMeshDrawData) {

    public MeshDrawData(int sizeInBytes, int materialIdx, int offset, int vertices) {
        this(sizeInBytes, materialIdx, offset, vertices, null);
    }

}
