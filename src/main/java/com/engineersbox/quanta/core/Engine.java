package com.engineersbox.quanta.core;

import com.engineersbox.quanta.debug.LoggerCompat;
import com.engineersbox.quanta.debug.OpenGLInfo;
import com.engineersbox.quanta.debug.PipelineStatistics;
import com.engineersbox.quanta.debug.hooks.RegisterInstanceVariableHooks;
import com.engineersbox.quanta.debug.hooks.VariableHook;
import com.engineersbox.quanta.gui.IGUIInstance;
import com.engineersbox.quanta.rendering.Renderer;
import com.engineersbox.quanta.resources.config.ConfigHandler;
import com.engineersbox.quanta.scene.Scene;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLUtil;
import org.lwjgl.system.Callback;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_MULTISAMPLE;

public class Engine {

    private static final Logger LOGGER = LogManager.getLogger(Engine.class);

    private final IAppLogic appLogic;
    private final Window window;
    private final Renderer renderer;
    private boolean running;
    @VariableHook(name = "engine.scene")
    private Scene scene;
    private final int targetFPS;
    private final int targetUPS;
    private final PipelineStatistics pipelineStatistics;
    @VariableHook(name = "engine.capture_pipeline_stats")
    private boolean capturePipelineStats = false;
    private Callback debugCallback;

    @RegisterInstanceVariableHooks
    public Engine(final String title,
                  final IAppLogic appLogic) {
        this.window = new Window(title, () -> {
            resize();
            return null;
        });
        init();
        Engine.LOGGER.info("[OPENGL] Created context");
        final OpenGLInfo info = OpenGLInfo.retrieve();
        info.log(false);
        this.targetFPS = ConfigHandler.CONFIG.video.fps;
        this.targetUPS = ConfigHandler.CONFIG.video.ups;
        this.appLogic = appLogic;
        this.renderer = new Renderer(this.window);
        this.scene = new Scene(
                this.window.getWidth(),
                this.window.getHeight()
        );
//        this.scene = Scene.deserialize("saves/scene.json");
//        this.scene.getProjection().updateProjectionMatrix(
//                this.window.getWidth(),
//                this.window.getHeight()
//        );
        this.pipelineStatistics = new PipelineStatistics();
        this.pipelineStatistics.init();
        this.appLogic.init(new EngineInitContext(
                this.window,
                this.scene,
                this.renderer,
                this.pipelineStatistics,
                info
        ));
        this.running = true;
    }

    private void init() {
        GL.createCapabilities();
        if (ConfigHandler.CONFIG.engine.glOptions.debug) {
            this.debugCallback = LoggerCompat.registerGLErrorLogger(
                    Engine.LOGGER,
                    Level.ERROR
            );
        }
        glEnable(GL_DEPTH_TEST);
        if (ConfigHandler.CONFIG.engine.glOptions.antialiasing) {
            glEnable(GL_MULTISAMPLE);
        }
        if (ConfigHandler.CONFIG.engine.glOptions.geometryFaceCulling) {
            glEnable(GL_CULL_FACE);
            glCullFace(GL_BACK);
        }
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
        final IGUIInstance guiInstance = this.scene.getGUIInstance();
        while (this.running && !this.window.windowShouldClose()) {
            this.window.pollEvents();
            final long now = System.currentTimeMillis();
            deltaUpdate += (now - initialTime) / timeU;
            deltaFps += (now - initialTime) / timeR;
            if (this.targetFPS <= 0 || deltaFps >= 1) {
                final boolean inputConsumed = guiInstance != null && guiInstance.handleGUIInput(this.scene, this.window);
                this.appLogic.input(this.window, this.scene, now - initialTime, inputConsumed);
            }
            if (deltaUpdate >= 1) {
                final long diffTimeMillis = now - updateTime;
                this.appLogic.update(this.window, this.scene, diffTimeMillis);
                updateTime = now;
                deltaUpdate--;
            }
            if (this.targetFPS <= 0 || deltaFps >= 1) {
                if (this.capturePipelineStats) {
                    this.pipelineStatistics.begin();
                }
                this.renderer.render(this.scene, this.window);
                if (this.capturePipelineStats) {
                    this.pipelineStatistics.end();
                }
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
        this.window.cleanup();
        if (ConfigHandler.CONFIG.engine.glOptions.debug && this.debugCallback != null) {
            this.debugCallback.free();
        }
    }

    public void resize() {
        final int width = this.window.getWidth();
        final int height = this.window.getHeight();
        this.scene.resize(width, height);
        this.renderer.resize(width, height);
    }

}
