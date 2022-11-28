package com.engineersbox.quanta.device.gpu.texture;

import com.engineersbox.quanta.device.gpu.GPUResource;

import javax.annotation.Nullable;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.glTexParameterf;

public abstract class MemoryTexture extends GPUResource {

    protected final TextureType type;

    protected MemoryTexture(final TextureType type) {
        super.id = glGenTextures();
        this.type = type;
    }

    public abstract void createTexImage(final int level,
                                        final int internalformat,
                                        final int[] dimensions,
                                        final int border,
                                        final int format,
                                        final int type,
                                        @Nullable final ByteBuffer pixels);

    public void setTexParameterf(final int paramName,
                                 final float value) {
        glTexParameterf(
                this.type.glType(),
                paramName,
                value
        );
    }

    public void setTexParameteri(final int paramName,
                                 final int value) {
        glTexParameteri(
                this.type.glType(),
                paramName,
                value
        );
    }

    public TextureType getType() {
        return this.type;
    }

    @Override
    public void bind() {
        super.bind();
        glBindTexture(this.type.glType(), super.id);
    }

    @Override
    public void unbind() {
        glBindTexture(this.type.glType(), 0);
    }

    @Override
    public void destroy() {
        super.destroy();
        glDeleteTextures(super.id);
    }

}
