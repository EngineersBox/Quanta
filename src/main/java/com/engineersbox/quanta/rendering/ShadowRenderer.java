package com.engineersbox.quanta.rendering;

import com.engineersbox.quanta.rendering.indirect.MeshDrawData;
import com.engineersbox.quanta.rendering.indirect.RenderBuffer;
import com.engineersbox.quanta.rendering.shadow.ShadowBuffer;
import com.engineersbox.quanta.rendering.shadow.ShadowCascade;
import com.engineersbox.quanta.resources.assets.object.Model;
import com.engineersbox.quanta.resources.assets.shader.ShaderModuleData;
import com.engineersbox.quanta.resources.assets.shader.ShaderProgram;
import com.engineersbox.quanta.resources.assets.shader.ShaderType;
import com.engineersbox.quanta.resources.assets.shader.Uniforms;
import com.engineersbox.quanta.scene.Entity;
import com.engineersbox.quanta.scene.Scene;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL40.GL_DRAW_INDIRECT_BUFFER;
import static org.lwjgl.opengl.GL43.glMultiDrawElementsIndirect;

public class ShadowRenderer {

    private static final int COMMAND_SIZE = 5 * 4;
    private final ArrayList<ShadowCascade> shadowCascades;
    private final Map<String, Integer> entitiesIdxMap;
    private final ShaderProgram shaderProgram;
    private final ShadowBuffer shadowBuffer;
    private int staticDrawCount;
    private int staticRenderBufferHandle;
    private Uniforms uniformsMap;

    public ShadowRenderer() {
        this.shaderProgram = new ShaderProgram(
                new ShaderModuleData("assets/shaders/shadow/shadow.vert", ShaderType.VERTEX)
        );
        this.shadowBuffer = new ShadowBuffer();
        this.shadowCascades = new ArrayList<>();
        for (int i = 0; i < ShadowCascade.SHADOW_MAP_CASCADE_COUNT; i++) {
            final ShadowCascade shadowCascade = new ShadowCascade();
            this.shadowCascades.add(shadowCascade);
        }
        createUniforms();
        this.entitiesIdxMap = new HashMap<>();
    }

    public void cleanup() {
        this.shaderProgram.cleanup();
        this.shadowBuffer.cleanup();
        glDeleteBuffers(this.staticRenderBufferHandle);
    }

    private void createUniforms() {
        this.uniformsMap = new Uniforms(this.shaderProgram.getProgramId());
        this.uniformsMap.createUniform("projectionViewMatrix");
        for (int i = 0; i < SceneRenderer.MAX_DRAW_ELEMENTS; i++) {
            final String name = "drawElements[" + i + "]";
            this.uniformsMap.createUniform(name + ".modelMatrixIdx");
        }
        for (int i = 0; i < SceneRenderer.MAX_ENTITIES; i++) {
            this.uniformsMap.createUniform("modelMatrices[" + i + "]");
        }
    }

    public List<ShadowCascade> getShadowCascades() {
        return this.shadowCascades;
    }

    public ShadowBuffer getShadowBuffer() {
        return this.shadowBuffer;
    }

    public void render(final Scene scene, final RenderBuffer renderBuffers) {
        ShadowCascade.updateCascadeShadows(this.shadowCascades, scene);
        glBindFramebuffer(GL_FRAMEBUFFER, this.shadowBuffer.getDepthMapFBO());
        glViewport(0, 0, ShadowBuffer.SHADOW_MAP_WIDTH, ShadowBuffer.SHADOW_MAP_HEIGHT);
        this.shaderProgram.bind();
        int entityIdx = 0;
        for (final Model model : scene.getModels().values()) {
            final List<Entity> entities = model.getEntities();
            for (final Entity entity : entities) {
                this.uniformsMap.setUniform("modelMatrices[" + entityIdx + "]", entity.getModelMatrix());
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
        final List<Model> modelList = scene.getModels()
                .values()
                .stream()
                .filter((final Model model) -> !model.isAnimated())
                .toList();
        for (final Model model : modelList) {
            final List<Entity> entities = model.getEntities();
            for (final MeshDrawData meshDrawData : model.getMeshDrawDataList()) {
                for (final Entity entity : entities) {
                    final String name = "drawElements[" + drawElement + "]";
                    this.uniformsMap.setUniform(
                            name + ".modelMatrixIdx",
                            this.entitiesIdxMap.get(entity.getId())
                    );
                    drawElement++;
                }
            }
        }
        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, this.staticRenderBufferHandle);
        glBindVertexArray(renderBuffers.getStaticVaoId());
        for (int i = 0; i < ShadowCascade.SHADOW_MAP_CASCADE_COUNT; i++) {
            glFramebufferTexture2D(
                    GL_FRAMEBUFFER,
                    GL_DEPTH_ATTACHMENT,
                    GL_TEXTURE_2D,
                    this.shadowBuffer.getDepthMapTexture().getIds()[i],
                    0
            );
            final ShadowCascade shadowCascade = this.shadowCascades.get(i);
            this.uniformsMap.setUniform(
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
        glBindVertexArray(0);
        this.shaderProgram.unbind();
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }
    public void setupData(final Scene scene) {
        setupEntitiesData(scene);
        setupStaticCommandBuffer(scene);
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
            numMeshes += model.getMeshDrawDataList().size();
        }
        int firstIndex = 0;
        int baseInstance = 0;
        final ByteBuffer commandBuffer = MemoryUtil.memAlloc(numMeshes * ShadowRenderer.COMMAND_SIZE);
        for (final Model model : modelList) {
            final List<Entity> entities = model.getEntities();
            final int numEntities = entities.size();
            for (final MeshDrawData meshDrawData : model.getMeshDrawDataList()) {
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
