package com.engineersbox.quanta.core;

import com.engineersbox.quanta.debug.OpenGLInfo;
import com.engineersbox.quanta.rendering.Renderer;
import com.engineersbox.quanta.resources.config.ConfigHandler;
import com.engineersbox.quanta.scene.Scene;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.GL_SHADING_LANGUAGE_VERSION;
import static org.lwjgl.opengl.GL30.GL_NUM_EXTENSIONS;

public class Engine {

    private static final Logger LOGGER = LogManager.getLogger(Engine.class);

    private final OpenGLInfo info;

    private final IAppLogic appLogic;
    private final Window window;
    private final Renderer renderer;
    private boolean running;
    private final Scene scene;
    private final int targetFPS;
    private final int targetUPS;

    public Engine(final String title,
                  final IAppLogic appLogic) {
        this.window = new Window(title, () -> {
            resize();
            return null;
        });
        Engine.init();
        this.info = Engine.saturateOpenGLInfo();
        Engine.LOGGER.info("[OPENGL] Created context");
        this.info.log(false);
        this.targetFPS = ConfigHandler.CONFIG.video.fps;
        this.targetUPS = ConfigHandler.CONFIG.video.ups;
        this.appLogic = appLogic;
        this.renderer = new Renderer();
        this.scene = new Scene(
                this.window.getWidth(),
                this.window.getHeight()
        );
        this.appLogic.init(this.window, this.scene, this.renderer);
        this.running = true;
    }

    private static OpenGLInfo saturateOpenGLInfo() {
        final int[] supportedExtensionsCount = new int[1];
        glGetIntegerv(GL_NUM_EXTENSIONS, supportedExtensionsCount);
        return new OpenGLInfo(
                glGetString(GL_VERSION),
                glGetString(GL_SHADING_LANGUAGE_VERSION),
                glGetString(GL_VENDOR),
                glGetString(GL_RENDERER),
                supportedExtensionsCount[0]
        );
    }

    private static void init() {
        GL.createCapabilities();
        glEnable(GL_DEPTH_TEST);
    }

    public void start() {
        this.running = true;
        run();
    }

    public void stop() {
        this.running = false;
    }

    private void run() {
        long initialTime = System.currentTimeMillis();
        final float timeU = 1000.0f / this.targetUPS;
        final float timeR = this.targetFPS > 0 ? 1000.0f / this.targetFPS : 0;
        float deltaUpdate = 0;
        float deltaFps = 0;

        long updateTime = initialTime;
        while (this.running && !this.window.windowShouldClose()) {
            this.window.pollEvents();

            final long now = System.currentTimeMillis();
            deltaUpdate += (now - initialTime) / timeU;
            deltaFps += (now - initialTime) / timeR;

            if (this.targetFPS <= 0 || deltaFps >= 1) {
                this.appLogic.input(this.window, this.scene, now - initialTime);
            }

            if (deltaUpdate >= 1) {
                final long diffTimeMillis = now - updateTime;
                this.appLogic.update(this.window, this.scene, diffTimeMillis);
                updateTime = now;
                deltaUpdate--;
            }

            if (this.targetFPS <= 0 || deltaFps >= 1) {
                this.renderer.render(this.window, this.scene);
                deltaFps--;
                this.window.update();
            }
            initialTime = now;
        }

        cleanup();
    }

    private void cleanup() {
        this.appLogic.cleanup();
        this.renderer.cleanup();
        this.scene.cleanup();
        this.window.cleanup();
    }

    public void resize() {
        this.scene.resize(
                this.window.getWidth(),
                this.window.getHeight()
        );
    }

}
