package com.engineersbox.quanta.device.gpu.buffer;

import com.engineersbox.quanta.device.gpu.GPUResource;

import static org.lwjgl.opengl.GL15.glGenBuffers;

public final class UBO extends DataBuffer {

    public UBO() {
        super(DataBufferType.UNB);
    }

}
