package com.engineersbox.quanta.rendering.renderers.core;

import com.engineersbox.quanta.rendering.RenderContext;
import com.engineersbox.quanta.rendering.handler.RenderHandler;
import com.engineersbox.quanta.rendering.handler.RenderPriority;
import com.engineersbox.quanta.rendering.handler.ShaderRenderHandler;
import com.engineersbox.quanta.rendering.handler.ShaderStage;
import com.engineersbox.quanta.resources.assets.material.Material;
import com.engineersbox.quanta.resources.assets.material.Texture;
import com.engineersbox.quanta.resources.assets.material.TextureCache;
import com.engineersbox.quanta.resources.assets.object.Mesh;
import com.engineersbox.quanta.resources.assets.shader.ShaderModuleData;
import com.engineersbox.quanta.resources.assets.shader.ShaderProgram;
import com.engineersbox.quanta.resources.assets.shader.ShaderType;
import com.engineersbox.quanta.resources.assets.shader.Uniforms;
import com.engineersbox.quanta.scene.SkyBox;
import org.joml.Matrix4f;

import java.util.stream.Stream;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL30.glBindVertexArray;

@RenderHandler(
        name = SkyBoxRenderer.RENDERER_NAME,
        priority = RenderPriority.DEFAULT + 3,
        stage = ShaderStage.CORE
)
public class SkyBoxRenderer extends ShaderRenderHandler {

    public static final String RENDERER_NAME = "@quanta__SKYBOX_RENDERER";
    private final Matrix4f viewMatrix;

    public SkyBoxRenderer() {
        super(new ShaderProgram(
                "Skybox",
                new ShaderModuleData("assets/shaders/scene/skybox.vert", ShaderType.VERTEX),
                new ShaderModuleData("assets/shaders/scene/skybox.frag", ShaderType.FRAGMENT)
        ));
        this.viewMatrix = new Matrix4f();
        createUniforms();
    }

    private void createUniforms() {
        final Uniforms uniforms = super.getUniforms("Skybox");
        Stream.of(
                "projectionMatrix",
                "viewMatrix",
                "modelMatrix",
                "diffuse",
                "textureSampler",
                "hasTexture",
                "brightnessThreshold"
        ).forEach(uniforms::createUniform);
    }

    @Override
    public void render(final RenderContext context) {
        final SkyBox skyBox = context.scene().getSkyBox();
        if (skyBox == null) {
            return;
        }
        super.bind("Skybox");
        final Uniforms uniforms = super.getUniforms("Skybox");
        uniforms.setUniform(
                "projectionMatrix",
                context.scene().getProjection().getProjectionMatrix()
        );
        this.viewMatrix.set(context.scene().getCamera().getViewMatrix());
        this.viewMatrix.m30(0);
        this.viewMatrix.m31(0);
        this.viewMatrix.m32(0);
        uniforms.setUniform(
                "viewMatrix",
                this.viewMatrix
        );
        uniforms.setUniform(
                "textureSampler",
                0
        );
        uniforms.setUniform(
                "brightnessThreshold",
                LightingRenderer.BRIGHTNESS_THRESHOLD
        );
        final Material material = skyBox.getMaterial();
        final Mesh mesh = skyBox.getMesh();
        final Texture texture = context.scene().getTextureCache().getTexture(material.getTexturePath());
        glActiveTexture(GL_TEXTURE0);
        texture.bind();
        uniforms.setUniform(
                "diffuse",
                material.getDiffuseColor()
        );
        uniforms.setUniform(
                "hasTexture",
                texture.getPath().equals(TextureCache.DEFAULT_TEXTURE)
        );
        glBindVertexArray(mesh.getVaoId());
        uniforms.setUniform(
                "modelMatrix",
                skyBox.getEntity().getModelMatrix()
        );
        glDrawElements(
                GL_TRIANGLES,
                mesh.vertexCount(),
                GL_UNSIGNED_INT,
                0
        );
        glBindVertexArray(0);
        super.unbind("Skybox");
        lightingRenderFinish();
    }

    private void lightingRenderFinish() {
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    }

}
