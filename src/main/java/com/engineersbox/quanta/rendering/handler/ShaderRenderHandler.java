package com.engineersbox.quanta.rendering.handler;

import com.engineersbox.quanta.rendering.RenderContext;
import com.engineersbox.quanta.resources.assets.shader.ShaderProgram;
import com.engineersbox.quanta.resources.assets.shader.Uniforms;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class ShaderRenderHandler {

    private static final Logger LOGGER = LogManager.getLogger(ShaderRenderHandler.class);

    protected final String handlerName;
    protected final Map<String, ShaderProgram> shaderPrograms;

    protected ShaderRenderHandler(final ShaderProgram ...shaders) {
        this.shaderPrograms = Arrays.stream(shaders).collect(Collectors.toMap(
                ShaderProgram::getName,
                Function.identity()
        ));
        this.handlerName = resolveHandlerName();
    }

    private String resolveHandlerName() {
        final RenderHandler annotation = getClass().getAnnotation(RenderHandler.class);
        if (annotation == null) {
            ShaderRenderHandler.LOGGER.warn("No @RenderHandler annotation found, cannot resolve render handler name");
            return null;
        }
        return annotation.name();
    }

    public String getName() {
        return this.handlerName;
    }

    public abstract void render(final RenderContext context);

    public void setupData(final RenderContext context) {}

    public void resize(final int width,
                       final int height) {}

    public List<ShaderProgram> provideShaders() {
        return this.shaderPrograms.values()
                .stream()
                .toList();
    }

    private ShaderProgram getShaderInternal(final String name) {
        final ShaderProgram shader = this.shaderPrograms.get(name);
        if (shader == null) {
            throw new IllegalArgumentException(String.format(
                    "No shader present with name \"%s\"",
                    name
            ));
        }
        return shader;
    }

    public void bind(final String name) {
        final ShaderProgram shader = getShaderInternal(name);
        shader.bind();
    }

    public void unbind(final String name) {
        final ShaderProgram shader = getShaderInternal(name);
        shader.unbind();
    }

    public ShaderProgram getShader(final String name) {
        return getShaderInternal(name);
    }

    public Uniforms getUniforms(final String name) {
        final ShaderProgram shader = getShaderInternal(name);
        return shader.getUniforms();
    }

    public void cleanup() {
        this.shaderPrograms.values().forEach(ShaderProgram::cleanup);
    }
}
