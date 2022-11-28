package com.engineersbox.quanta.device.gpu.buffer;

import com.engineersbox.quanta.device.gpu.GPUResource;

import static org.lwjgl.opengl.GL15.glGenBuffers;

public final class EBO extends DataBuffer {

    public EBO() {
        super(DataBufferType.EAB);
    }

}
