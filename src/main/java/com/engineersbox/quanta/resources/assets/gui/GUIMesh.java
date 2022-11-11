package com.engineersbox.quanta.resources.assets.gui;


import imgui.ImDrawData;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.*;

public class GUIMesh {

    private final int indicesVBO;
    private final int vaoId;
    private final int verticesVBO;

    public GUIMesh() {
        this.vaoId = glGenVertexArrays();
        glBindVertexArray(this.vaoId);

        this.verticesVBO = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, this.verticesVBO);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(
                0,
                2,
                GL_FLOAT,
                false,
                ImDrawData.SIZEOF_IM_DRAW_VERT,
                0
        );
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(
                1,
                2,
                GL_FLOAT,
                false,
                ImDrawData.SIZEOF_IM_DRAW_VERT,
                8
        );
        glEnableVertexAttribArray(2);
        glVertexAttribPointer(
                2,
                4,
                GL_UNSIGNED_BYTE,
                true,
                ImDrawData.SIZEOF_IM_DRAW_VERT,
                16
        );

        this.indicesVBO = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    public void cleanup() {
        glDeleteBuffers(this.indicesVBO);
        glDeleteBuffers(this.verticesVBO);
        glDeleteVertexArrays(this.vaoId);
    }

    public int getIndicesVBO() {
        return this.indicesVBO;
    }

    public int getVaoId() {
        return this.vaoId;
    }

    public int getVerticesVBO() {
        return this.verticesVBO;
    }

}
