package com.engineersbox.quanta.device.gpu.buffer;

import com.engineersbox.quanta.device.gpu.GPUResource;

import static org.lwjgl.opengl.GL15.glGenBuffers;

public class VBO extends GPUResource {

    public VBO() {
        super.id = glGenBuffers();
        // TODO: Finish this
    }

    @Override
    public void unbind() {

    }
}
