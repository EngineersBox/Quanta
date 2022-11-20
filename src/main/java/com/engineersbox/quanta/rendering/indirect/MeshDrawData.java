package com.engineersbox.quanta.rendering.indirect;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonSerialize
public record MeshDrawData(int sizeInBytes,
                           int materialIdx,
                           int offset,
                           int vertices,
                           AnimMeshDrawData animMeshDrawData) {

    public MeshDrawData(final int sizeInBytes,
                        final int materialIdx,
                        final int offset,
                        final int vertices) {
        this(sizeInBytes, materialIdx, offset, vertices, null);
    }

}
