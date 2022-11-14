package com.engineersbox.quanta.rendering;

import com.engineersbox.quanta.core.Window;
import com.engineersbox.quanta.rendering.deferred.GBuffer;
import com.engineersbox.quanta.scene.Scene;

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

    public Renderer(final Window window) {
        this.sceneRenderer = new SceneRenderer();
        this.guiRenderer = new GUIRenderer(window);
        this.skyBoxRenderer = new SkyBoxRenderer();
        this.shadowRenderer = new ShadowRenderer();
        this.gBuffer = new GBuffer(window);
        this.lightingRenderer = new LightingRenderer();
    }

    public void cleanup() {
        this.sceneRenderer.cleanup();
        this.guiRenderer.cleanup();
        this.skyBoxRenderer.cleanup();
        this.shadowRenderer.cleanup();
        this.lightingRenderer.cleanup();
        this.gBuffer.cleanup();
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
        this.shadowRenderer.render(scene);
        this.sceneRenderer.render(scene, this.gBuffer);
        lightingRenderStart(window);
        this.lightingRenderer.render(scene, this.shadowRenderer, this.gBuffer);
        this.skyBoxRenderer.render(scene);
        lightingRenderFinish();
        this.guiRenderer.render(scene);
    }

    public void resize(final int width,
                       final int height) {
        this.guiRenderer.resize(width, height);
    }
}
