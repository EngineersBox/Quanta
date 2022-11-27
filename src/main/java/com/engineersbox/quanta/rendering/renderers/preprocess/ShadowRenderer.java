package com.engineersbox.quanta.rendering.renderers.preprocess;

import com.engineersbox.quanta.rendering.RenderContext;
import com.engineersbox.quanta.rendering.handler.RenderHandler;
import com.engineersbox.quanta.rendering.handler.RenderPriority;
import com.engineersbox.quanta.rendering.handler.ShaderRenderHandler;
import com.engineersbox.quanta.rendering.handler.ShaderStage;
import com.engineersbox.quanta.rendering.indirect.AnimMeshDrawData;
import com.engineersbox.quanta.rendering.indirect.MeshDrawData;
import com.engineersbox.quanta.rendering.renderers.core.SceneRenderer;
import com.engineersbox.quanta.rendering.shadow.ShadowBuffer;
import com.engineersbox.quanta.rendering.shadow.ShadowCascade;
import com.engineersbox.quanta.resources.assets.object.Model;
import com.engineersbox.quanta.resources.assets.shader.ShaderModuleData;
import com.engineersbox.quanta.resources.assets.shader.ShaderProgram;
import com.engineersbox.quanta.resources.assets.shader.ShaderType;
import com.engineersbox.quanta.resources.assets.shader.Uniforms;
import com.engineersbox.quanta.resources.config.ConfigHandler;
import com.engineersbox.quanta.scene.Entity;
import com.engineersbox.quanta.scene.Scene;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL40.GL_DRAW_INDIRECT_BUFFER;
import static org.lwjgl.opengl.GL43.glMultiDrawElementsIndirect;

@RenderHandler(
        name = ShadowRenderer.RENDERER_NAME,
        priority = RenderPriority.DEFAULT + 1,
        stage = ShaderStage.PRE_PROCESS
)
public class ShadowRenderer extends ShaderRenderHandler {

    public static final String RENDERER_NAME = "@quanta__SHADOW_RENDERER";

    private static final int COMMAND_SIZE = 5 * 4;
    private final List<ShadowCascade> shadowCascades;
    private final Map<String, Integer> entitiesIdxMap;
    private final ShadowBuffer shadowBuffer;
    private int staticDrawCount;
    private int staticRenderBufferHandle;
    private int animDrawCount;
    private int animRenderBufferHandle;

    public ShadowRenderer() {
        super(new ShaderProgram(
                "Shadow",
                new ShaderModuleData("assets/shaders/shadow/shadow.vert", ShaderType.VERTEX)
        ));
        this.shadowBuffer = new ShadowBuffer();
        this.shadowCascades = new ArrayList<>();
        this.entitiesIdxMap = new HashMap<>();
        createUniforms();
    }

    private void createUniforms() {
        for (int i = 0; i < ShadowCascade.SHADOW_MAP_CASCADE_COUNT; i++) {
            final ShadowCascade shadowCascade = new ShadowCascade();
            this.shadowCascades.add(shadowCascade);
        }
        final Uniforms uniforms = super.getUniforms("Shadow");
        uniforms.createUniform("projectionViewMatrix");
        for (int i = 0; i < SceneRenderer.MAX_DRAW_ELEMENTS; i++) {
            final String name = "drawElements[" + i + "]";
            uniforms.createUniform(name + ".modelMatrixIdx");
        }
        for (int i = 0; i < SceneRenderer.MAX_ENTITIES; i++) {
            uniforms.createUniform("modelMatrices[" + i + "]");
        }
    }

    @Override
    public void cleanup() {
        super.cleanup();
        this.shadowBuffer.cleanup();
        glDeleteBuffers(this.staticRenderBufferHandle);
        glDeleteBuffers(this.animRenderBufferHandle);
    }

    public List<ShadowCascade> getShadowCascades() {
        return this.shadowCascades;
    }

    public ShadowBuffer getShadowBuffer() {
        return this.shadowBuffer;
    }

    @Override
    public void render(final RenderContext context) {
        if (ConfigHandler.CONFIG.engine.glOptions.shadowFaceCulling) {
            if (!ConfigHandler.CONFIG.engine.glOptions.geometryFaceCulling) {
                glEnable(GL_CULL_FACE);
            }
            glCullFace(GL_FRONT);
        }
        context.attributes().putIfAbsent(ShadowRenderer.RENDERER_NAME, this);
        ShadowCascade.updateCascadeShadows(this.shadowCascades, context.scene());
        glBindFramebuffer(GL_FRAMEBUFFER, this.shadowBuffer.getDepthMapFBO());
        glViewport(0, 0, ShadowBuffer.SHADOW_MAP_WIDTH, ShadowBuffer.SHADOW_MAP_HEIGHT);
        super.bind("Shadow");
        final Uniforms uniforms = super.getUniforms("Shadow");
        int entityIdx = 0;
        for (final Model model : context.scene().getModels().values()) {
            final List<Entity> entities = model.getEntities();
            for (final Entity entity : entities) {
                uniforms.setUniform("modelMatrices[" + entityIdx + "]", entity.getModelMatrix());
                entityIdx++;
            }
        }
        for (int i = 0; i < ShadowCascade.SHADOW_MAP_CASCADE_COUNT; i++) {
            glFramebufferTexture2D(
                    GL_FRAMEBUFFER,
                    GL_DEPTH_ATTACHMENT,
                    GL_TEXTURE_2D,
                    this.shadowBuffer.getDepthMapTexture().getIds()[i],
                    0
            );
            glClear(GL_DEPTH_BUFFER_BIT);
        }
        // Static meshes
        int drawElement = 0;
        List<Model> modelList = context.scene().getModels()
                .values()
                .stream()
                .filter(Predicate.not(Model::isAnimated))
                .toList();
        for (final Model model : modelList) {
            final List<Entity> entities = model.getEntities();
            for (final MeshDrawData ignored : model.getMeshDrawData()) {
                for (final Entity entity : entities) {
                    final String name = "drawElements[" + drawElement + "]";
                    uniforms.setUniform(
                            name + ".modelMatrixIdx",
                            this.entitiesIdxMap.get(entity.getId())
                    );
                    drawElement++;
                }
            }
        }
        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, this.staticRenderBufferHandle);
        glBindVertexArray(context.renderBuffers().getStaticVaoId());
        for (int i = 0; i < ShadowCascade.SHADOW_MAP_CASCADE_COUNT; i++) {
            glFramebufferTexture2D(
                    GL_FRAMEBUFFER,
                    GL_DEPTH_ATTACHMENT,
                    GL_TEXTURE_2D,
                    this.shadowBuffer.getDepthMapTexture().getIds()[i],
                    0
            );
            final ShadowCascade shadowCascade = this.shadowCascades.get(i);
            uniforms.setUniform(
                    "projectionViewMatrix",
                    shadowCascade.getProjectionViewMatrix()
            );
            glMultiDrawElementsIndirect(
                    GL_TRIANGLES,
                    GL_UNSIGNED_INT,
                    0,
                    this.staticDrawCount,
                    0
            );
        }
        // Animated meshes
        drawElement = 0;
        modelList = context.scene().getModels()
                .values()
                .stream()
                .filter(Model::isAnimated)
                .toList();
        for (final Model model : modelList) {
            for (final MeshDrawData meshDrawData : model.getMeshDrawData()) {
                final AnimMeshDrawData animMeshDrawData = meshDrawData.animMeshDrawData();
                final Entity entity = animMeshDrawData.entity();
                final String name = "drawElements[" + drawElement + "]";
                uniforms.setUniform(
                        name + ".modelMatrixIdx",
                        this.entitiesIdxMap.get(entity.getId())
                );
                drawElement++;
            }
        }
        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, this.animRenderBufferHandle);
        glBindVertexArray(context.renderBuffers().getAnimVaoId());
        for (int i = 0; i < ShadowCascade.SHADOW_MAP_CASCADE_COUNT; i++) {
            glFramebufferTexture2D(
                    GL_FRAMEBUFFER,
                    GL_DEPTH_ATTACHMENT,
                    GL_TEXTURE_2D,
                    this.shadowBuffer.getDepthMapTexture().getIds()[i],
                    0
            );
            final ShadowCascade shadowCascade = this.shadowCascades.get(i);
            uniforms.setUniform(
                    "projectionViewMatrix",
                    shadowCascade.getProjectionViewMatrix()
            );
            glMultiDrawElementsIndirect(
                    GL_TRIANGLES,
                    GL_UNSIGNED_INT,
                    0,
                    this.animDrawCount,
                    0
            );
        }
        glBindVertexArray(0);
        super.unbind("Shadow");
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        if (ConfigHandler.CONFIG.engine.glOptions.shadowFaceCulling) {
            glCullFace(GL_BACK);
            if (!ConfigHandler.CONFIG.engine.glOptions.geometryFaceCulling) {
                glDisable(GL_CULL_FACE);
            }
        }
    }

    private void setupAnimCommandBuffer(final Scene scene) {
        final List<Model> modelList = scene.getModels()
                .values()
                .stream()
                .filter(Model::isAnimated)
                .toList();
        int numMeshes = 0;
        for (final Model model : modelList) {
            numMeshes += model.getMeshDrawData().size();
        }
        int firstIndex = 0;
        int baseInstance = 0;
        final ByteBuffer commandBuffer = MemoryUtil.memAlloc(numMeshes * ShadowRenderer.COMMAND_SIZE);
        for (final Model model : modelList) {
            for (final MeshDrawData meshDrawData : model.getMeshDrawData()) {
                // count
                commandBuffer.putInt(meshDrawData.vertices());
                // instanceCount
                commandBuffer.putInt(1);
                commandBuffer.putInt(firstIndex);
                // baseVertex
                commandBuffer.putInt(meshDrawData.offset());
                commandBuffer.putInt(baseInstance);

                firstIndex += meshDrawData.vertices();
                baseInstance++;
            }
        }
        commandBuffer.flip();
        this.animDrawCount = commandBuffer.remaining() / ShadowRenderer.COMMAND_SIZE;
        this.animRenderBufferHandle = glGenBuffers();
        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, this.animRenderBufferHandle);
        glBufferData(GL_DRAW_INDIRECT_BUFFER, commandBuffer, GL_DYNAMIC_DRAW);
        MemoryUtil.memFree(commandBuffer);
    }

    @Override
    public void setupData(final RenderContext context) {
        final Scene scene = context.scene();
        setupEntitiesData(scene);
        setupStaticCommandBuffer(scene);
        setupAnimCommandBuffer(scene);
    }

    private void setupEntitiesData(final Scene scene) {
        this.entitiesIdxMap.clear();
        int entityIdx = 0;
        for (final Model model : scene.getModels().values()) {
            final List<Entity> entities = model.getEntities();
            for (final Entity entity : entities) {
                this.entitiesIdxMap.put(entity.getId(), entityIdx);
                entityIdx++;
            }
        }
    }

    private void setupStaticCommandBuffer(final Scene scene) {
        final List<Model> modelList = scene.getModels()
                .values()
                .stream()
                .filter((final Model model) -> !model.isAnimated())
                .toList();
        int numMeshes = 0;
        for (final Model model : modelList) {
            numMeshes += model.getMeshDrawData().size();
        }
        int firstIndex = 0;
        int baseInstance = 0;
        final ByteBuffer commandBuffer = MemoryUtil.memAlloc(numMeshes * ShadowRenderer.COMMAND_SIZE);
        for (final Model model : modelList) {
            final List<Entity> entities = model.getEntities();
            final int numEntities = entities.size();
            for (final MeshDrawData meshDrawData : model.getMeshDrawData()) {
                // count
                commandBuffer.putInt(meshDrawData.vertices());
                // instanceCount
                commandBuffer.putInt(numEntities);
                commandBuffer.putInt(firstIndex);
                // baseVertex
                commandBuffer.putInt(meshDrawData.offset());
                commandBuffer.putInt(baseInstance);
                firstIndex += meshDrawData.vertices();
                baseInstance += entities.size();
            }
        }
        commandBuffer.flip();
        this.staticDrawCount = commandBuffer.remaining() / ShadowRenderer.COMMAND_SIZE;
        this.staticRenderBufferHandle = glGenBuffers();
        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, this.staticRenderBufferHandle);
        glBufferData(GL_DRAW_INDIRECT_BUFFER, commandBuffer, GL_DYNAMIC_DRAW);
        MemoryUtil.memFree(commandBuffer);
    }
}
