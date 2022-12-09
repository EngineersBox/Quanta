package com.engineersbox.quanta.rendering;

import com.engineersbox.quanta.core.Window;
import com.engineersbox.quanta.rendering.buffers.GBuffer;
import com.engineersbox.quanta.rendering.buffers.SSAOBuffer;
import com.engineersbox.quanta.rendering.handler.RenderHandler;
import com.engineersbox.quanta.rendering.handler.RenderPriority;
import com.engineersbox.quanta.rendering.handler.ShaderRenderHandler;
import com.engineersbox.quanta.rendering.handler.ShaderStage;
import com.engineersbox.quanta.rendering.buffers.HDRBuffer;
import com.engineersbox.quanta.rendering.indirect.AnimationRenderBuffers;
import com.engineersbox.quanta.rendering.view.Camera;
import com.engineersbox.quanta.resources.assets.object.Model;
import com.engineersbox.quanta.resources.assets.shader.ShaderProgram;
import com.engineersbox.quanta.resources.assets.shader.ShaderValidationState;
import com.engineersbox.quanta.resources.config.ConfigHandler;
import com.engineersbox.quanta.resources.config.shader.ShaderConfig;
import com.engineersbox.quanta.resources.config.shader.ShaderIncludes;
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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.lwjgl.opengl.GL11.*;

public class Renderer {

    private static final Logger LOGGER = LogManager.getLogger(Renderer.class);
    private static final Reflections RENDER_HANDLER_REFLECTIONS = new Reflections(new ConfigurationBuilder()
            .addScanners(Scanners.TypesAnnotated, Scanners.SubTypes)
            .forPackages("com.engineersbox.quanta")
    );

    private LinkedMap<String, ShaderRenderHandler> preProcessRenderHandlers;
    private LinkedMap<String, ShaderRenderHandler> coreRenderHandlers;
    private LinkedMap<String, ShaderRenderHandler> postProcessRenderHandlers;
    private RenderContext context;
    private int sceneHash;
    private final ShaderConfig shaderConfig;
    private final ShaderIncludes shaderIncludes;

    public Renderer() {
        this.preProcessRenderHandlers = new LinkedMap<>();
        this.coreRenderHandlers = new LinkedMap<>();
        this.postProcessRenderHandlers = new LinkedMap<>();
        this.shaderConfig = new ShaderConfig();
        this.shaderConfig.createNamedString();
        this.shaderIncludes = new ShaderIncludes();
        this.shaderIncludes.createIncludes();
        resolveShaders();
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
        }).map(StreamUtils.passThrough((final ShaderRenderHandler handler) -> {
            final RenderHandler annotation = handler.getClass().getAnnotation(RenderHandler.class);
            final String priorityName = RenderPriority.convertToName(annotation.priority());
            Renderer.LOGGER.info(
                    "[SHADER STAGE: {}] Found shader render handler \"{}\" with priority {}{}",
                    annotation.stage(),
                    annotation.name(),
                    annotation.priority(),
                    priorityName == null ? "" : " (" + priorityName + ")"
            );
        })).collect(Collectors.toMap(
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

    private void instancedShaderRenderHandlerCleanup(final ShaderRenderHandler handler) {
        handler.cleanup(this.context);
    }

    public void cleanup() {
        this.preProcessRenderHandlers.values().forEach(this::instancedShaderRenderHandlerCleanup);
        this.coreRenderHandlers.values().forEach(this::instancedShaderRenderHandlerCleanup);
        this.postProcessRenderHandlers.values().forEach(this::instancedShaderRenderHandlerCleanup);
    }

    public void render(final Scene scene,
                       final Window window) {
        updateContext(scene, window);
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
        if (ConfigHandler.CONFIG.engine.features.showAxis
            && ConfigHandler.CONFIG.engine.glOptions.compatProfile) {
            renderAxes(scene);
        }
    }

    private static BiConsumer<String, ShaderRenderHandler> createRenderHandlerConsumer(final ShaderStage stage,
                                                                                       final RenderContext context) {
        return (final String name, final ShaderRenderHandler handler) -> {
            for (final ShaderProgram shader : handler.provideShaders()) {
                final ShaderValidationState validationState = shader.validate();
                if (!validationState.isValid()) {
                    Renderer.LOGGER.warn(
                            "[Handler: {} Stage: {}] [Shader: {} Id: {}] Failed to validate shader, skipping {}. Reason: {}",
                            handler.getName(),
                            stage,
                            shader.getName(),
                            shader.getProgramId(),
                            name,
                            validationState.message()
                    );
                    return;
                }
            }
//            Renderer.LOGGER.debug("[Shader: {}, Stage: {}] Invoking render handler {}", shader.program(), stage, name);
            handler.render(context);
        };
    }

    private void renderAxes(final Scene scene) {
        final Camera camera = scene.getCamera();
        glPushMatrix();
        glLoadIdentity();
        final double rotX = Math.toDegrees(camera.getRotation().x);
        final double rotY = Math.toDegrees(camera.getRotation().y);
        final double rotZ = Math.toDegrees(camera.getRotation().z);
        glRotated(rotX, 1.0f, 0.0f, 0.0f);
        glRotated(rotY, 0.0f, 1.0f, 0.0f);
        glRotated(rotZ, 0.0f, 0.0f, 1.0f);
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

//        if (ConfigHandler.CONFIG.engine.glOptions.cullface) {
//            glDisable(GL_CULL_FACE);
//        }
//
//        // Y axis
//        glPushMatrix();
//        glColor3f(1.0f, 0.0f, 0.0f);
//        glRotatef(90.0f, 0.0f,1.0f,0.0f);
//        glTranslatef(0.0f, 0.0f, 0.04f);
//        glut.glutSolidCone(0.007,0.025, 20, 20); // TODO: Add dependency for basic OpenGL shapes
//        glPopMatrix();
//
//        // X axis
//        glPushMatrix();
//        glColor3f(0.0f, 0.0f, 1.0f);
//        glRotatef(90.0f, 0.0f,0.0f,1.0f);
//        glTranslatef(0.0f, 0.00f, 0.04f);
//        glut.glutSolidCone(0.007,0.025, 20, 20);
//        glPopMatrix();
//
//        // Z axis
//        glPushMatrix();
//        glColor3f(0.0f, 1.0f, 0.0f);
//        glRotatef(90.0f, -1.0f,0.0f,0.0f);
//        glTranslatef(0.0f, 0.00f, 0.04f);
//        glut.glutSolidCone(0.007,0.025, 20, 20);
//        glPopMatrix();
//
//        if (ConfigHandler.CONFIG.engine.glOptions.cullface) {
//            glEnable(GL_CULL_FACE);
//        }
        glPopMatrix();

    }

    public void setupData(final Scene scene,
                          final Window window) {
        updateContext(scene, window);
        StreamUtils.zipForEach(
                Arrays.stream(ShaderStage.values()),
                Stream.of(
                        this.preProcessRenderHandlers,
                        this.coreRenderHandlers,
                        this.postProcessRenderHandlers
                ),
                (final ShaderStage stage, final LinkedMap<String, ShaderRenderHandler> handlers) -> {
                    Renderer.LOGGER.info("[SHADER STAGE: {}] Invoking data setup for render handlers", stage);
                    handlers.forEach((final String name, final ShaderRenderHandler handler) -> handler.setupData(this.context));
                }
        );
        new ArrayList<>(scene.getModels().values())
                .stream()
                .map(Model::getMeshData)
                .forEach(List::clear);
    }

    private void updateContext(final Scene scene,
                               final Window window) {
        if (this.sceneHash != scene.hashCode()) {
            this.sceneHash = scene.hashCode();
            this.context = new RenderContext(
                    scene,
                    window
            );
        }
    }

    public void resize(final int width,
                       final int height) {
        this.preProcessRenderHandlers.values().forEach((final ShaderRenderHandler handler) -> handler.resize(width, height));
        this.coreRenderHandlers.values().forEach((final ShaderRenderHandler handler) -> handler.resize(width, height));
        this.postProcessRenderHandlers.values().forEach((final ShaderRenderHandler handler) -> handler.resize(width, height));
    }
}
