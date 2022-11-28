package com.engineersbox.quanta.device.gpu.texture;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.glTexImage1D;

public class MemoryTexture1D extends MemoryTexture {

    public MemoryTexture1D() {
        super(TextureType.T1D);
    }

    @Override
    public void createTexImage(final int level,
                               final int internalformat,
                               final int[] dimensions,
                               final int border,
                               final int format,
                               final int type,
                               @Nullable final ByteBuffer pixels) {
        if (dimensions.length != 1) {
            throw new IllegalArgumentException("Expected 1 dimension, got " + dimensions.length);
        }
        glTexImage1D(
                super.type.glType(),
                level,
                internalformat,
                dimensions[0],
                border,
                format,
                type,
                pixels
        );
    }
}
