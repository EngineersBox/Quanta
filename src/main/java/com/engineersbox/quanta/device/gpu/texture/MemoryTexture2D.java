package com.engineersbox.quanta.device.gpu.texture;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.glTexImage2D;

public final class MemoryTexture2D extends MemoryTexture {

    public MemoryTexture2D() {
        super(TextureType.T2D);
    }

    @Override
    public void createTexImage(final int level,
                               final int internalformat,
                               final int[] dimensions,
                               final int border,
                               final int format,
                               final int type,
                               @Nullable final ByteBuffer pixels) {
        if (dimensions.length != 2) {
            throw new IllegalArgumentException("Expected 2 dimensions, got " + dimensions.length);
        }
        glTexImage2D(
                super.type.glType(),
                level,
                internalformat,
                dimensions[0],
                dimensions[1],
                border,
                format,
                type,
                pixels
        );
    }

}
