package com.engineersbox.quanta.device.gpu.buffer;

import com.engineersbox.quanta.device.gpu.GPUResource;

import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;

public final class VBO extends DataBuffer {

    public VBO() {
        super(DataBufferType.VAB);
    }

    public void enableAttributeAndPointer(final int index,
                                          final int size,
                                          final int type,
                                          final boolean normalized,
                                          final int stride,
                                          final int pointer,
                                          final boolean autoBindAndUnbind) {
        if (!autoBindAndUnbind && !super.bound) {
            throw new IllegalStateException(String.format(
                    "Cannot enable vertex attribute on unbound resource %s",
                    getClass().getSimpleName()
            ));
        }
        if (autoBindAndUnbind) {
            super.bind();
        }
        glEnableVertexAttribArray(index);
        glVertexAttribPointer(
                index,
                size,
                type,
                normalized,
                stride,
                pointer
        );
        if (autoBindAndUnbind) {
            super.unbind();
        }
    }

    public void enableAttributeAndPointer(final int index,
                                          final int size,
                                          final int type,
                                          final boolean normalized,
                                          final int stride,
                                          final int pointer) {
        enableAttributeAndPointer(
                index,
                size,
                type,
                normalized,
                stride,
                pointer,
                false
        );
    }

}
