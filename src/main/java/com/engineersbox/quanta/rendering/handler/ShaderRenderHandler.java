package com.engineersbox.quanta.rendering.handler;

import com.engineersbox.quanta.rendering.RenderContext;
import com.engineersbox.quanta.resources.assets.shader.ShaderProgram;
import com.engineersbox.quanta.resources.assets.shader.Uniforms;

public abstract class ShaderRenderHandler {

    protected final ShaderProgram shader;
    protected final Uniforms uniforms;

    protected ShaderRenderHandler(final ShaderProgram shader) {
        this.shader = shader;
        this.uniforms = new Uniforms(shader.getProgramId());
    }

    public abstract void render(final RenderContext context);

    public ShaderProgram provideShader() {
        return this.shader;
    }

    public void bind() {
        this.shader.bind();
    }

    public void unbind() {
        this.shader.unbind();
    }

    public void cleanup() {
        this.shader.cleanup();
    }
}
