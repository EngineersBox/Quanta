package com.engineersbox.quanta.resources.assets.shader;

import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL32.GL_GEOMETRY_SHADER;
import static org.lwjgl.opengl.GL40.GL_TESS_CONTROL_SHADER;
import static org.lwjgl.opengl.GL40.GL_TESS_EVALUATION_SHADER;
import static org.lwjgl.opengl.GL43.GL_COMPUTE_SHADER;

public enum ShaderType {
    VERTEX(GL_VERTEX_SHADER),
    FRAGMENT(GL_FRAGMENT_SHADER),
    GEOMETRY(GL_GEOMETRY_SHADER),
    TESSELLATION_EVALUATION(GL_TESS_EVALUATION_SHADER),
    TESSELLATION_CONTROL(GL_TESS_CONTROL_SHADER),
    COMPUTE(GL_COMPUTE_SHADER);

    private final int type;

    ShaderType(final int type) {
        this.type = type;
    }

    public int getType() {
        return this.type;
    }
}
