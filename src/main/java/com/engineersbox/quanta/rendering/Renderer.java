package com.engineersbox.quanta.rendering;

import com.engineersbox.quanta.core.Window;
import com.engineersbox.quanta.rendering.deferred.GBuffer;
import com.engineersbox.quanta.rendering.handler.RenderHandler;
import com.engineersbox.quanta.rendering.handler.ShaderRenderHandler;
import com.engineersbox.quanta.rendering.handler.ShaderStage;
import com.engineersbox.quanta.rendering.indirect.RenderBuffers;
import com.engineersbox.quanta.rendering.view.Camera;
import com.engineersbox.quanta.resources.assets.object.Model;
import com.engineersbox.quanta.resources.assets.shader.ShaderProgram;
import com.engineersbox.quanta.resources.assets.shader.ShaderValidationState;
import com.engineersbox.quanta.resources.config.ConfigHandler;
import com.engineersbox.quanta.scene.Scene;
import com.engineersbox.quanta.utils.StreamUtils;
import org.apache.commons.collections4.map.LinkedMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL14.GL_FUNC_ADD;
import static org.lwjgl.opengl.GL14.glBlendEquation;
import static org.lwjgl.opengl.GL30.*;

public class Renderer {

    private static final Logger LOGGER = LogManager.getLogger(Renderer.class);
    private static final Reflections RENDER_HANDLER_REFLECTIONS = new Reflections(new ConfigurationBuilder()
            .addScanners(Scanners.TypesAnnotated, Scanners.SubTypes)
            .forPackages("com.engineersbox.quanta")
    );

    private final SceneRenderer sceneRenderer;
    private final GUIRenderer guiRenderer;
    private final SkyBoxRenderer skyBoxRenderer;
    private final ShadowRenderer shadowRenderer;
    private final GBuffer gBuffer;
    private final LightingRenderer lightingRenderer;
    private final RenderBuffers renderBuffers;
    private final AnimationRenderer animationRenderer;

    private LinkedMap<String, ShaderRenderHandler> preProcessRenderHandlers;
    private LinkedMap<String, ShaderRenderHandler> coreRenderHandlers;
    private LinkedMap<String, ShaderRenderHandler> postProcessRenderHandlers;
    private RenderContext context;
    private int sceneHash;

    public Renderer(final Window window) {
        this.sceneRenderer = new SceneRenderer();
        this.guiRenderer = new GUIRenderer(window);
        this.skyBoxRenderer = new SkyBoxRenderer();
        this.shadowRenderer = new ShadowRenderer();
        this.gBuffer = new GBuffer(window);
        this.lightingRenderer = new LightingRenderer();
        this.renderBuffers = new RenderBuffers();
        this.animationRenderer = new AnimationRenderer();
        this.preProcessRenderHandlers = new LinkedMap<>();
        this.coreRenderHandlers = new LinkedMap<>();
        this.postProcessRenderHandlers = new LinkedMap<>();
    }

    private void resolveShaders() {
        final Set<Class<? extends ShaderRenderHandler>> renderHandlers = Renderer.RENDER_HANDLER_REFLECTIONS.getSubTypesOf(ShaderRenderHandler.class);
        final Set<Class<? extends ShaderRenderHandler>> handlers = renderHandlers.stream()
                .filter((final Class<? extends ShaderRenderHandler> clazz) -> clazz.isAnnotationPresent(RenderHandler.class))
                .collect(Collectors.toSet());
        Renderer.LOGGER.info("Found {} shader render handlers (pre-classification)", handlers.size());
        this.preProcessRenderHandlers = resolveShaderHandlerMapForStream(filterShaderStage(handlers.stream(), ShaderStage.PRE_PROCESS));
        this.coreRenderHandlers = resolveShaderHandlerMapForStream(filterShaderStage(handlers.stream(), ShaderStage.CORE));
        this.postProcessRenderHandlers = resolveShaderHandlerMapForStream(filterShaderStage(handlers.stream(), ShaderStage.POST_PROCESS));
    }

    private LinkedMap<String, ShaderRenderHandler> resolveShaderHandlerMapForStream(final Stream<Class<? extends ShaderRenderHandler>> handlers) {
        return handlers.map((final Class<? extends ShaderRenderHandler> clazz) -> {
            try {
                final Constructor<? extends ShaderRenderHandler> constructor = clazz.getDeclaredConstructor();
                return (ShaderRenderHandler) constructor.newInstance();
            } catch (final IllegalAccessException | NoSuchMethodException | InvocationTargetException | InstantiationException e) {
                throw new RuntimeException(String.format(
                        "Unable to instantiate shader %s render handler:",
                        clazz.getAnnotation(RenderHandler.class).name()
                ), e);
            }
        }).peek((final ShaderRenderHandler handler) -> {
            final RenderHandler annotation = handler.getClass().getAnnotation(RenderHandler.class);
            Renderer.LOGGER.info(
                    "[SHADER STAGE: {}] Found shader render handler \"{}\" with priority {}",
                    annotation.stage(),
                    annotation.name(),
                    annotation.priority()
            );
        }).collect(Collectors.toMap(
                (final ShaderRenderHandler handler) -> handler.getClass().getAnnotation(RenderHandler.class).name(),
                Function.identity(),
                (final ShaderRenderHandler handler1, final ShaderRenderHandler handler2) -> {
                    throw new IllegalStateException("Unexpected duplicate shader: " + handler1.getClass().getAnnotation(RenderHandler.class).name());
                }, LinkedMap::new
        ));
    }

    private static Stream<Class<? extends ShaderRenderHandler>> filterShaderStage(final Stream<Class<? extends ShaderRenderHandler>> handlerStream,
                                                                                  final ShaderStage stage) {
        return handlerStream.filter((final Class<? extends ShaderRenderHandler> clazz) -> {
            final RenderHandler annotation = clazz.getAnnotation(RenderHandler.class);
            return annotation.stage().equals(stage);
        }).sorted((final Class<? extends ShaderRenderHandler> handler1, final Class<? extends ShaderRenderHandler> handler2) -> {
            final RenderHandler handlerAnnotation1 = handler1.getAnnotation(RenderHandler.class);
            final RenderHandler handlerAnnotation2 = handler2.getAnnotation(RenderHandler.class);
            return Integer.compare(handlerAnnotation1.priority(), handlerAnnotation2.priority());
        });
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

    public static void lightingRenderFinish() {
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    }

    public static void lightingRenderStart(final Window window,
                                            final GBuffer gBuffer) {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glViewport(0, 0, window.getWidth(), window.getHeight());
        glEnable(GL_BLEND);
        glBlendEquation(GL_FUNC_ADD);
        glBlendFunc(GL_ONE, GL_ONE);
        glBindFramebuffer(GL_READ_FRAMEBUFFER, gBuffer.getGBufferId());
    }

    public void render(final Window window,
                       final Scene scene) {
        if (this.sceneHash != scene.hashCode()) {
            this.sceneHash = scene.hashCode();
            this.context = new RenderContext(
                    scene,
                    this.renderBuffers,
                    this.gBuffer,
                    this.shadowRenderer
            );
        }
        StreamUtils.zipForEach(
                Arrays.stream(ShaderStage.values()),
                Stream.of(
                        this.preProcessRenderHandlers,
                        this.coreRenderHandlers,
                        this.postProcessRenderHandlers
                ),
                (final ShaderStage stage, final LinkedMap<String, ShaderRenderHandler> handlers) -> {
//                    Renderer.LOGGER.info("Executing {} shaders", stage);
                    handlers.forEach(createRenderHandlerConsumer(stage, this.context));
                }
        );

        this.animationRenderer.render(scene, this.renderBuffers);
        this.shadowRenderer.render(scene, this.renderBuffers);
        this.sceneRenderer.render(scene, this.renderBuffers, this.gBuffer);
        Renderer.lightingRenderStart(window, this.gBuffer);
        this.lightingRenderer.render(scene, this.shadowRenderer, this.gBuffer);
        this.skyBoxRenderer.render(scene);
        Renderer.lightingRenderFinish();
        if (ConfigHandler.CONFIG.engine.features.showAxis) {
            renderAxes(scene.getCamera());
        }
        this.guiRenderer.render(scene);
    }

    private static BiConsumer<String, ShaderRenderHandler> createRenderHandlerConsumer(final ShaderStage stage,
                                                                                       final RenderContext context) {
        return (final String name, final ShaderRenderHandler handler) -> {
            final ShaderProgram shader = handler.provideShader();
            final ShaderValidationState validationState = shader.validate();
            if (!validationState.isValid()) {
                Renderer.LOGGER.warn(
                        "[Shader: {} Stage: {}] Invalid failed during link or validation, skipping {}. Reason: {}",
                        stage.name(),
                        shader.getProgramId(),
                        name,
                        validationState.message()
                );
                return;
            }
//            Renderer.LOGGER.debug("[Shader: {}] Invoking {} shader {}", shader.program(), stage, name);
            handler.render(context);
        };
    }

    private void renderAxes(final Camera camera) {
        glPushMatrix();
        glTranslatef(0, 0, 0);
        glLoadIdentity();
        final float rotX = camera.getRotation().x;
        final float rotY = camera.getRotation().y;
        final float rotZ = 0;
        glRotatef(rotX, 1.0f, 0.0f, 0.0f);
        glRotatef(rotY, 0.0f, 1.0f, 0.0f);
        glRotatef(rotZ, 0.0f, 0.0f, 1.0f);
        glLineWidth(2.0f);

        glBegin(GL_LINES);
        // X Axis
        glColor3f(1.0f, 0.0f, 0.0f);
        glVertex3f(0.0f, 0.0f, 0.0f);
        glVertex3f(0.04f, 0.0f, 0.0f);

        // Y Axis
        glColor3f(0.0f, 1.0f, 0.0f);
        glVertex3f(0.0f, 0.0f, 0.0f);
        glVertex3f(0.0f, 0.04f, 0.0f);

        // Z Axis
        glColor3f(0.0f, 0.0f, 1.0f);
        glVertex3f(0.0f, 0.0f, 0.0f);
        glVertex3f(0.0f, 0.0f, 0.04f);

        glEnd();

        if (ConfigHandler.CONFIG.engine.glOptions.cullface) {
            glDisable(GL_CULL_FACE);
        }

        // Y axis
        glPushMatrix();
        glColor3f(1.0f, 0.0f, 0.0f);
        glRotatef(90.0f, 0.0f,1.0f,0.0f);
        glTranslatef(0.0f, 0.0f, 0.04f);
        glut.glutSolidCone(0.007,0.025, 20, 20); // TODO: Add dependency for basic OpenGL shapes
        glPopMatrix();

        // X axis
        glPushMatrix();
        glColor3f(0.0f, 0.0f, 1.0f);
        glRotatef(90.0f, 0.0f,0.0f,1.0f);
        glTranslatef(0.0f, 0.00f, 0.04f);
        glut.glutSolidCone(0.007,0.025, 20, 20);
        glPopMatrix();

        // Z axis
        glPushMatrix();
        glColor3f(0.0f, 1.0f, 0.0f);
        glRotatef(90.0f, -1.0f,0.0f,0.0f);
        glTranslatef(0.0f, 0.00f, 0.04f);
        glut.glutSolidCone(0.007,0.025, 20, 20);
        glPopMatrix();

        if (ConfigHandler.CONFIG.engine.glOptions.cullface) {
            glEnable(GL_CULL_FACE);
        }
        glPopMatrix();

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
