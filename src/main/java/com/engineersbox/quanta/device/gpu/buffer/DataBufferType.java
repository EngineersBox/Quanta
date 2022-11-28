package com.engineersbox.quanta.device.gpu.buffer;

import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL21.GL_PIXEL_PACK_BUFFER;
import static org.lwjgl.opengl.GL21.GL_PIXEL_UNPACK_BUFFER;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL40.GL_DRAW_INDIRECT_BUFFER;
import static org.lwjgl.opengl.GL42.GL_ATOMIC_COUNTER_BUFFER;
import static org.lwjgl.opengl.GL43.GL_DISPATCH_INDIRECT_BUFFER;
import static org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BUFFER;
import static org.lwjgl.opengl.GL44.GL_QUERY_BUFFER;

public enum DataBufferType {
    VAB(GL_ARRAY_BUFFER),
    ACB(GL_ATOMIC_COUNTER_BUFFER),
    CRB(GL_COPY_READ_BUFFER),
    CWB(GL_COPY_WRITE_BUFFER),
    DIIB(GL_DISPATCH_INDIRECT_BUFFER),
    DRIB(GL_DRAW_INDIRECT_BUFFER),
    EAB(GL_ELEMENT_ARRAY_BUFFER),
    PPB(GL_PIXEL_PACK_BUFFER),
    PUB(GL_PIXEL_UNPACK_BUFFER),
    QUB(GL_QUERY_BUFFER),
    SSB(GL_SHADER_STORAGE_BUFFER),
    TEB(GL_TEXTURE_BUFFER),
    TFB(GL_TRANSFORM_FEEDBACK_BUFFER),
    UNB(GL_UNIFORM_BUFFER);

    private final int glType;

    DataBufferType(final int glType) {
        this.glType = glType;
    }

    public int getGlType() {
        return this.glType;
    }
}
