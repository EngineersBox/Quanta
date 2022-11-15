package com.engineersbox.quanta.rendering;

import com.engineersbox.quanta.core.Window;
import com.engineersbox.quanta.rendering.deferred.GBuffer;
import com.engineersbox.quanta.rendering.indirect.RenderBuffers;
import com.engineersbox.quanta.resources.assets.object.Model;
import com.engineersbox.quanta.scene.Scene;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL14.GL_FUNC_ADD;
import static org.lwjgl.opengl.GL14.glBlendEquation;
import static org.lwjgl.opengl.GL30.*;

public class Renderer {

    private final SceneRenderer sceneRenderer;
    private final GUIRenderer guiRenderer;
    private final SkyBoxRenderer skyBoxRenderer;
    private final ShadowRenderer shadowRenderer;
    private final GBuffer gBuffer;
    private final LightingRenderer lightingRenderer;
    private final RenderBuffers renderBuffers;
    private final AnimationRenderer animationRenderer;

    public Renderer(final Window window) {
        this.sceneRenderer = new SceneRenderer();
        this.guiRenderer = new GUIRenderer(window);
        this.skyBoxRenderer = new SkyBoxRenderer();
        this.shadowRenderer = new ShadowRenderer();
        this.gBuffer = new GBuffer(window);
        this.lightingRenderer = new LightingRenderer();
        this.renderBuffers = new RenderBuffers();
        this.animationRenderer = new AnimationRenderer();
    }

    public void cleanup() {
        this.sceneRenderer.cleanup();
        this.guiRenderer.cleanup();
        this.skyBoxRenderer.cleanup();
        this.shadowRenderer.cleanup();
        this.lightingRenderer.cleanup();
        this.gBuffer.cleanup();
        this.renderBuffers.cleanup();
        this.animationRenderer.cleanup();
    }

    private void lightingRenderFinish() {
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    }

    private void lightingRenderStart(final Window window) {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glViewport(0, 0, window.getWidth(), window.getHeight());
        glEnable(GL_BLEND);
        glBlendEquation(GL_FUNC_ADD);
        glBlendFunc(GL_ONE, GL_ONE);
        glBindFramebuffer(GL_READ_FRAMEBUFFER, this.gBuffer.getGBufferId());
    }

    public void render(final Window window,
                       final Scene scene) {
        this.animationRenderer.render(scene, this.renderBuffers);
        this.shadowRenderer.render(scene, this.renderBuffers);
        this.sceneRenderer.render(scene, this.renderBuffers, this.gBuffer);
        lightingRenderStart(window);
        this.lightingRenderer.render(scene, this.shadowRenderer, this.gBuffer);
        this.skyBoxRenderer.render(scene);
        lightingRenderFinish();
        this.guiRenderer.render(scene);
    }

    public void setupData(final Scene scene) {
        this.renderBuffers.loadStaticModels(scene);
        this.renderBuffers.loadAnimatedModels(scene);
        this.sceneRenderer.setupData(scene);
        this.shadowRenderer.setupData(scene);
        new ArrayList<>(scene.getModels().values())
                .stream()
                .map(Model::getMeshData)
                .forEach(List::clear);
    }

    public void resize(final int width,
                       final int height) {
        this.guiRenderer.resize(width, height);
    }
}
