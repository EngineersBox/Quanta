package com.engineersbox.quanta.device.gpu.buffer;

import com.engineersbox.quanta.device.gpu.GPUResource;

import static org.lwjgl.opengl.GL30.*;

public class VAO extends GPUResource {

    public VAO() {
        super.id = glGenVertexArrays();
    }

    @Override
    public void bind() {
        super.bind();
        glBindVertexArray(super.id);
    }

    @Override
    public void unbind() {
        super.unbind();
        glBindVertexArray(0);
    }

    @Override
    public void destroy() {
        super.destroy();
        glDeleteVertexArrays(super.id);
    }

}
