package com.engineersbox.quanta.rendering.renderers.core;

import com.engineersbox.quanta.debug.hooks.VariableHook;
import com.engineersbox.quanta.rendering.RenderContext;
import com.engineersbox.quanta.rendering.handler.RenderHandler;
import com.engineersbox.quanta.rendering.handler.RenderPriority;
import com.engineersbox.quanta.rendering.handler.ShaderRenderHandler;
import com.engineersbox.quanta.rendering.handler.ShaderStage;
import com.engineersbox.quanta.rendering.indirect.AnimMeshDrawData;
import com.engineersbox.quanta.rendering.indirect.MeshDrawData;
import com.engineersbox.quanta.resources.assets.material.Material;
import com.engineersbox.quanta.resources.assets.material.MaterialCache;
import com.engineersbox.quanta.resources.assets.material.Texture;
import com.engineersbox.quanta.resources.assets.material.TextureCache;
import com.engineersbox.quanta.resources.assets.object.Model;
import com.engineersbox.quanta.resources.assets.shader.ShaderModuleData;
import com.engineersbox.quanta.resources.assets.shader.ShaderProgram;
import com.engineersbox.quanta.resources.assets.shader.ShaderType;
import com.engineersbox.quanta.resources.assets.shader.Uniforms;
import com.engineersbox.quanta.resources.config.ConfigHandler;
import com.engineersbox.quanta.scene.Entity;
import com.engineersbox.quanta.scene.Scene;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL40.GL_DRAW_INDIRECT_BUFFER;
import static org.lwjgl.opengl.GL43.glMultiDrawElementsIndirect;

@RenderHandler(
        name = SceneRenderer.RENDERER_NAME,
        priority = RenderPriority.DEFAULT,
        stage = ShaderStage.CORE
)
public class SceneRenderer extends ShaderRenderHandler {

    private static final Logger LOGGER = LogManager.getLogger(SceneRenderer.class);
    public static final String RENDERER_NAME = "@quanta__SCENE_RENDERER";
    public static final int MAX_DRAW_ELEMENTS = 100;
    public static final int MAX_ENTITIES = 50;
    private static final int COMMAND_SIZE = 5 * 4;
    private static final int MAX_MATERIALS = 100;
    private static final int MAX_TEXTURES = 100;
    @VariableHook(name = "renderer.show_normals")
    private static boolean SHOW_NORMALS = false;
    private final Map<String, Integer> entitiesIdxMap;
    private int staticDrawCount;
    private int staticRenderBufferHandle;
    private int animDrawCount;
    private int animRenderBufferHandle;

    public SceneRenderer() {
        super(new ShaderProgram(
                "Scene",
                new ShaderModuleData("assets/shaders/scene/scene.vert", ShaderType.VERTEX),
                new ShaderModuleData("assets/shaders/scene/scene.frag", ShaderType.FRAGMENT)
        ));
        createUniforms();
        this.entitiesIdxMap = new HashMap<>();
    }

    @Override
    public void cleanup() {
        super.cleanup();
        glDeleteBuffers(this.staticRenderBufferHandle);
        glDeleteBuffers(this.animRenderBufferHandle);
    }

    private void createUniforms() {
        final Uniforms uniforms = super.getUniforms("Scene");
        Stream.of(
                "projectionMatrix",
                "viewMatrix",
                "showNormals"
        ).forEach(uniforms::createUniform);
        for (int i = 0; i < SceneRenderer.MAX_TEXTURES; i++) {
            uniforms.createUniform("textureSampler[" + i + "]");
        }
        for (int i = 0; i < SceneRenderer.MAX_MATERIALS; i++) {
            final String name = "materials[" + i + "]";
            Stream.of(
                    name + ".diffuse",
                    name + ".specular",
                    name + ".reflectance",
                    name + ".normalMapIdx",
                    name + ".textureIdx"
            ).forEach(uniforms::createUniform);
        }
        for (int i = 0; i < SceneRenderer.MAX_DRAW_ELEMENTS; i++) {
            final String name = "drawElements[" + i + "]";
            Stream.of(
                    name + ".modelMatrixIdx",
                    name + ".materialIdx"
            ).forEach(uniforms::createUniform);
        }
        for (int i = 0; i < SceneRenderer.MAX_ENTITIES; i++) {
            uniforms.createUniform("modelMatrices[" + i + "]");
        }
    }

    @Override
    public void render(final RenderContext context) {
        if (ConfigHandler.CONFIG.engine.glOptions.wireframe) {
            glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
        }
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, context.gBuffer().getGBufferId());
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glViewport(0, 0, context.gBuffer().getWidth(), context.gBuffer().getHeight());
        glDisable(GL_BLEND);
        super.bind("Scene");
        final Uniforms uniforms = super.getUniforms("Scene");
        uniforms.setUniform(
                "projectionMatrix",
                context.scene().getProjection().getProjectionMatrix()
        );
        uniforms.setUniform(
                "viewMatrix",
                context.scene().getCamera().getViewMatrix()
        );
        uniforms.setUniform(
                "showNormals",
                SHOW_NORMALS
        );
        final TextureCache textureCache = context.scene().getTextureCache();
        final List<Texture> textures = textureCache.getAll().stream().toList();
        final int numTextures = textures.size();
        if (numTextures > SceneRenderer.MAX_TEXTURES) {
            SceneRenderer.LOGGER.warn("Only " + SceneRenderer.MAX_TEXTURES + " textures can be used");
        }
        for (int i = 0; i < Math.min(SceneRenderer.MAX_TEXTURES, numTextures); i++) {
            uniforms.setUniform("textureSampler[" + i + "]", i);
            final Texture texture = textures.get(i);
            glActiveTexture(GL_TEXTURE0 + i);
            texture.bind();
        }
        int entityIdx = 0;
        for (final Model model : context.scene().getModels().values()) {
            final List<Entity> entities = model.getEntities();
            for (final Entity entity : entities) {
                uniforms.setUniform(
                        "modelMatrices[" + entityIdx + "]",
                        entity.getModelMatrix()
                );
                entityIdx++;
            }
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
            for (final MeshDrawData meshDrawData : model.getMeshDrawData()) {
                for (final Entity entity : entities) {
                    final String name = "drawElements[" + drawElement + "]";
                    uniforms.setUniform(
                            name + ".modelMatrixIdx",
                            this.entitiesIdxMap.get(entity.getId())
                    );
                    uniforms.setUniform(
                            name + ".materialIdx",
                            meshDrawData.materialIdx()
                    );
                    drawElement++;
                }
            }
        }
        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, this.staticRenderBufferHandle);
        glBindVertexArray(context.animationRenderBuffers().getStaticVaoId());
        glMultiDrawElementsIndirect(
                GL_TRIANGLES,
                GL_UNSIGNED_INT,
                0,
                this.staticDrawCount,
                0
        );
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
                uniforms.setUniform(
                        name + ".materialIdx",
                        meshDrawData.materialIdx()
                );
                drawElement++;
            }
        }
        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, this.animRenderBufferHandle);
        glBindVertexArray(context.animationRenderBuffers().getAnimVaoId());
        glMultiDrawElementsIndirect(
                GL_TRIANGLES,
                GL_UNSIGNED_INT,
                0,
                this.animDrawCount,
                0
        );
        glBindVertexArray(0);
        glEnable(GL_BLEND);
        super.unbind("Scene");
        if (ConfigHandler.CONFIG.engine.glOptions.wireframe) {
            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
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
        final ByteBuffer commandBuffer = MemoryUtil.memAlloc(numMeshes * SceneRenderer.COMMAND_SIZE);
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
        this.animDrawCount = commandBuffer.remaining() / SceneRenderer.COMMAND_SIZE;
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
        setupMaterialsUniform(scene.getTextureCache(), scene.getMaterialCache());
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

    private void setupMaterialsUniform(final TextureCache textureCache,
                                       final MaterialCache materialCache) {
        final List<Texture> textures = textureCache.getAll().stream().toList();
        final int numTextures = textures.size();
        if (numTextures > SceneRenderer.MAX_TEXTURES) {
            SceneRenderer.LOGGER.warn("Only " + SceneRenderer.MAX_TEXTURES + " textures can be used");
        }
        final Map<String, Integer> texturePosMap = new HashMap<>();
        for (int i = 0; i < Math.min(SceneRenderer.MAX_TEXTURES, numTextures); i++) {
            texturePosMap.put(textures.get(i).getPath(), i);
        }
        super.bind("Scene");
        final Uniforms uniforms = super.getUniforms("Scene");
        final List<Material> materialList = materialCache.getMaterials();
        final int numMaterials = materialList.size();
        for (int i = 0; i < numMaterials; i++) {
            final Material material = materialCache.getMaterial(i);
            final String name = "materials[" + i + "]";
            uniforms.setUniform(
                    name + ".diffuse",
                    material.getDiffuseColor()
            );
            uniforms.setUniform(
                    name + ".specular",
                    material.getSpecularColor()
            );
            uniforms.setUniform(
                    name + ".reflectance",
                    material.getReflectance()
            );
            final String normalMapPath = material.getNormalMapPath();
            int idx = 0;
            if (normalMapPath != null) {
                idx = texturePosMap.computeIfAbsent(normalMapPath, k -> 0);
            }
            uniforms.setUniform(
                    name + ".normalMapIdx",
                    idx
            );
            final Texture texture = textureCache.getTexture(material.getTexturePath());
            idx = texturePosMap.computeIfAbsent(texture.getPath(), (final String ignored) -> 0);
            uniforms.setUniform(
                    name + ".textureIdx",
                    idx
            );
        }
        super.unbind("Scene");
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
        final ByteBuffer commandBuffer = MemoryUtil.memAlloc(numMeshes * SceneRenderer.COMMAND_SIZE);
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
        this.staticDrawCount = commandBuffer.remaining() / SceneRenderer.COMMAND_SIZE;
        this.staticRenderBufferHandle = glGenBuffers();
        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, this.staticRenderBufferHandle);
        glBufferData(GL_DRAW_INDIRECT_BUFFER, commandBuffer, GL_DYNAMIC_DRAW);
        MemoryUtil.memFree(commandBuffer);
    }

}
