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
import java.util.stream.Collectors;

public abstract class ShaderRenderHandler {

    private static final Logger LOGGER = LogManager.getLogger(ShaderRenderHandler.class);

    protected final String handlerName;
    protected final Map<String, Pair<ShaderProgram, Uniforms>> shaderPrograms;

    protected ShaderRenderHandler(final ShaderProgram ...shaders) {
        this.shaderPrograms = Arrays.stream(shaders).collect(Collectors.toMap(
                ShaderProgram::getName,
                (final ShaderProgram shader) -> ImmutablePair.of(
                        shader,
                        new Uniforms(shader.getProgramId())
                )
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
                .map(Pair::getLeft)
                .toList();
    }

    private Pair<ShaderProgram, Uniforms> getPair(final String name) {
        final Pair<ShaderProgram, Uniforms> pair = this.shaderPrograms.get(name);
        if (pair == null) {
            throw new IllegalArgumentException(String.format(
                    "No shader present with name \"%s\"",
                    name
            ));
        }
        return pair;
    }

    public void bind(final String name) {
        final Pair<ShaderProgram, Uniforms> shader = getPair(name);
        shader.getLeft().bind();
    }

    public void unbind(final String name) {
        final Pair<ShaderProgram, Uniforms> shader = getPair(name);
        shader.getLeft().unbind();
    }

    public ShaderProgram getShader(final String name) {
        final Pair<ShaderProgram, Uniforms> shader = getPair(name);
        return shader.getLeft();
    }
    public Uniforms getUniforms(final String name) {
        final Pair<ShaderProgram, Uniforms> shader = getPair(name);
        return shader.getRight();
    }

    public void cleanup() {
        this.shaderPrograms.values()
                .stream()
                .map(Pair::getLeft)
                .forEach(ShaderProgram::cleanup);
    }
}
