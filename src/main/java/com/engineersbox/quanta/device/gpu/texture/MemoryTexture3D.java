package com.engineersbox.quanta.device.gpu.texture;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL12.glTexImage3D;

public class MemoryTexture3D extends MemoryTexture {

    public MemoryTexture3D() {
        super(TextureType.T3D);
    }

    @Override
    public void createTexImage(final int level,
                               final int internalformat,
                               final int[] dimensions,
                               final int border,
                               final int format,
                               final int type,
                               @Nullable final ByteBuffer pixels) {
        if (dimensions.length != 3) {
            throw new IllegalArgumentException("Expected 3 dimensions, got " + dimensions.length);
        }
        glTexImage3D(
                super.type.glType(),
                level,
                internalformat,
                dimensions[0],
                dimensions[1],
                dimensions[2],
                border,
                format,
                type,
                pixels
        );
    }

}
