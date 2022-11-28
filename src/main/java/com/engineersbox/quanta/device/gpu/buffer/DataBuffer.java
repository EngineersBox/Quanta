package com.engineersbox.quanta.device.gpu.buffer;

import com.engineersbox.quanta.device.gpu.GPUResource;
import org.apache.logging.log4j.util.TriConsumer;

import java.nio.*;

import static org.lwjgl.opengl.GL15.*;

public abstract sealed class DataBuffer extends GPUResource permits EBO, VBO, UBO {

    protected final DataBufferType type;

    protected DataBuffer(final DataBufferType type) {
        super.id = glGenBuffers();
        this.type = type;
    }

    public DataBufferType getType() {
        return this.type;
    }

    public void setData(final long data,
                        final int usage) {
        glBufferData(
                super.id,
                data,
                usage
        );
    }

    public void setData(final int[] data,
                        final int usage) {
        glBufferData(
                super.id,
                data,
                usage
        );
    }

    public void setData(final short[] data,
                        final int usage) {
        glBufferData(
                super.id,
                data,
                usage
        );
    }

    public void setData(final long[] data,
                        final int usage) {
        glBufferData(
                super.id,
                data,
                usage
        );
    }

    public void setData(final float[] data,
                        final int usage) {
        glBufferData(
                super.id,
                data,
                usage
        );
    }

    public void setData(final double[] data,
                        final int usage) {
        glBufferData(
                super.id,
                data,
                usage
        );
    }

    public void setData(final IntBuffer data,
                        final int usage) {
        glBufferData(
                super.id,
                data,
                usage
        );
    }

    public void setData(final ShortBuffer data,
                        final int usage) {
        glBufferData(
                super.id,
                data,
                usage
        );
    }

    public void setData(final LongBuffer data,
                        final int usage) {
        glBufferData(
                super.id,
                data,
                usage
        );
    }

    public void setData(final FloatBuffer data,
                        final int usage) {
        glBufferData(
                super.id,
                data,
                usage
        );
    }

    public void setData(final DoubleBuffer data,
                        final int usage) {
        glBufferData(
                super.id,
                data,
                usage
        );
    }

    public void setData(final ByteBuffer data,
                        final int usage) {
        glBufferData(
                super.id,
                data,
                usage
        );
    }

    @Override
    public void bind() {
        super.unbind();
        glBindBuffer(
                this.type.getGlType(),
                super.id
        );
    }

    @Override
    public void unbind() {
        super.unbind();
        glBindBuffer(
                this.type.getGlType(),
                0
        );
    }

    @Override
    public void destroy() {
        super.destroy();
        glDeleteBuffers(super.id);
    }

}
