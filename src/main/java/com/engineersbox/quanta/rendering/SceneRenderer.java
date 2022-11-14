package com.engineersbox.quanta.rendering;

import com.engineersbox.quanta.core.Window;
import com.engineersbox.quanta.resources.assets.material.Material;
import com.engineersbox.quanta.resources.assets.material.Texture;
import com.engineersbox.quanta.resources.assets.material.TextureCache;
import com.engineersbox.quanta.resources.assets.object.Mesh;
import com.engineersbox.quanta.resources.assets.object.Model;
import com.engineersbox.quanta.resources.assets.shader.ShaderModuleData;
import com.engineersbox.quanta.resources.assets.shader.ShaderProgram;
import com.engineersbox.quanta.resources.assets.shader.ShaderType;
import com.engineersbox.quanta.resources.assets.shader.Uniforms;
import com.engineersbox.quanta.scene.Entity;
import com.engineersbox.quanta.scene.Scene;
import com.engineersbox.quanta.scene.atmosphere.Fog;
import com.engineersbox.quanta.scene.lighting.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.List;
import java.util.stream.Stream;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL14.GL_FUNC_ADD;
import static org.lwjgl.opengl.GL14.glBlendEquation;
import static org.lwjgl.opengl.GL30.glBindVertexArray;

public class SceneRenderer {

    private static final int MAX_POINT_LIGHTS = 5;
    private static final int MAX_SPOT_LIGHTS = 5;

    private final ShaderProgram shader;
    private Uniforms uniforms;

    public SceneRenderer() {
        this.shader = new ShaderProgram(
                new ShaderModuleData("assets/shaders/scene/scene.vert", ShaderType.VERTEX),
                new ShaderModuleData("assets/shaders/scene/scene.frag", ShaderType.FRAGMENT)
        );
        this.shader.validate();
        createUniforms();
    }

    private void createUniforms() {
        this.uniforms = new Uniforms(this.shader.getProgramId());
        Stream.of(
                "projectionMatrix",
                "viewMatrix",
                "modelMatrix",
                "texSampler",
                "material.ambient",
                "material.diffuse",
                "material.specular",
                "material.reflectance",
                "ambientLight.factor",
                "ambientLight.color",
                "directionalLight.color",
                "directionalLight.direction",
                "directionalLight.intensity",
                "fog.activeFog",
                "fog.color",
                "fog.density"
        ).forEach(this.uniforms::createUniform);
        for (int i = 0; i < SceneRenderer.MAX_POINT_LIGHTS; i++) {
            final String name = "pointLights[" + i + "]";
            Stream.of(
                    name + ".position",
                    name + ".color",
                    name + ".intensity",
                    name + ".att.constant",
                    name + ".att.linear",
                    name + ".att.exponent"
            ).forEach(this.uniforms::createUniform);
        }
        for (int i = 0; i < SceneRenderer.MAX_SPOT_LIGHTS; i++) {
            final String name = "spotLights[" + i + "]";
            Stream.of(
                    name + ".pl.position",
                    name + ".pl.color",
                    name + ".pl.intensity",
                    name + ".pl.att.constant",
                    name + ".pl.att.linear",
                    name + ".pl.att.exponent",
                    name + ".coneDir",
                    name + ".cutoff"
            ).forEach(this.uniforms::createUniform);
        }
    }

    private void updateLights(final Scene scene) {
        final Matrix4f viewMatrix = scene.getCamera().getViewMatrix();

        final SceneLights sceneLights = scene.getSceneLights();
        final AmbientLight ambientLight = sceneLights.getAmbientLight();
        this.uniforms.setUniform("ambientLight.factor", ambientLight.getIntensity());
        this.uniforms.setUniform("ambientLight.color", ambientLight.getColor());

        final DirectionalLight dirLight = sceneLights.getDirectionalLight();
        final Vector4f auxDir = new Vector4f(dirLight.getDirection(), 0);
        auxDir.mul(viewMatrix);
        final Vector3f dir = new Vector3f(auxDir.x, auxDir.y, auxDir.z);
        this.uniforms.setUniform("directionalLight.color", dirLight.getColor());
        this.uniforms.setUniform("directionalLight.direction", dir);
        this.uniforms.setUniform("directionalLight.intensity", dirLight.getIntensity());

        final List<PointLight> pointLights = sceneLights.getPointLights();
        final int numPointLights = pointLights.size();
        PointLight pointLight;
        for (int i = 0; i < SceneRenderer.MAX_POINT_LIGHTS; i++) {
            if (i < numPointLights) {
                pointLight = pointLights.get(i);
            } else {
                pointLight = null;
            }
            final String name = "pointLights[" + i + "]";
            updatePointLight(pointLight, name, viewMatrix);
        }


        final List<SpotLight> spotLights = sceneLights.getSpotLights();
        final int numSpotLights = spotLights.size();
        SpotLight spotLight;
        for (int i = 0; i < SceneRenderer.MAX_SPOT_LIGHTS; i++) {
            if (i < numSpotLights) {
                spotLight = spotLights.get(i);
            } else {
                spotLight = null;
            }
            final String name = "spotLights[" + i + "]";
            updateSpotLight(spotLight, name, viewMatrix);
        }
    }

    private void updatePointLight(final PointLight pointLight,
                                  final String prefix,
                                  final Matrix4f viewMatrix) {
        final Vector4f aux = new Vector4f();
        final Vector3f lightPosition = new Vector3f();
        final Vector3f color = new Vector3f();
        float intensity = 0.0f;
        float constant = 0.0f;
        float linear = 0.0f;
        float exponent = 0.0f;
        if (pointLight != null) {
            aux.set(pointLight.getPosition(), 1);
            aux.mul(viewMatrix);
            lightPosition.set(aux.x, aux.y, aux.z);
            color.set(pointLight.getColor());
            intensity = pointLight.getIntensity();
            final Attenuation attenuation = pointLight.getAttenuation();
            constant = attenuation.getConstant();
            linear = attenuation.getLinear();
            exponent = attenuation.getExponent();
        }
        this.uniforms.setUniform(prefix + ".position", lightPosition);
        this.uniforms.setUniform(prefix + ".color", color);
        this.uniforms.setUniform(prefix + ".intensity", intensity);
        this.uniforms.setUniform(prefix + ".att.constant", constant);
        this.uniforms.setUniform(prefix + ".att.linear", linear);
        this.uniforms.setUniform(prefix + ".att.exponent", exponent);
    }

    private void updateSpotLight(final SpotLight spotLight, final String prefix, final Matrix4f viewMatrix) {
        PointLight pointLight = null;
        Vector3f coneDirection = new Vector3f();
        float cutoff = 0.0f;
        if (spotLight != null) {
            coneDirection = spotLight.getConeDirection();
            cutoff = spotLight.getCutOff();
            pointLight = spotLight.getPointLight();
        }
        this.uniforms.setUniform(prefix + ".coneDir", coneDirection);
        this.uniforms.setUniform(prefix + ".coneDir", cutoff);
        updatePointLight(pointLight, prefix + ".pl", viewMatrix);
    }

    public void render(final Window window,
                       final Scene scene) {
        glEnable(GL_BLEND);
        glBlendEquation(GL_FUNC_ADD);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        this.shader.bind();
        this.uniforms.setUniform(
                "projectionMatrix",
                scene.getProjection().getProjectionMatrix()
        );
        this.uniforms.setUniform(
                "viewMatrix",
                scene.getCamera().getViewMatrix()
        );
        this.uniforms.setUniform(
                "texSampler",
                0
        );
        final Fog fog = scene.getFog();
        this.uniforms.setUniform("fog.activeFog", fog.isActive());
        this.uniforms.setUniform("fog.color", fog.getColor());
        this.uniforms.setUniform("fog.density", fog.getDensity());
        updateLights(scene);
        final TextureCache textureCache = scene.getTextureCache();
        for (final Model model : scene.getModels().values()) {
            final List<Entity> entities = model.getEntities();
            for (final Material material : model.getMaterials()) {
                this.uniforms.setUniform("material.ambient", material.getAmbientColor());
                this.uniforms.setUniform("material.diffuse", material.getDiffuseColor());
                this.uniforms.setUniform("material.specular", material.getSpecularColor());
                this.uniforms.setUniform("material.reflectance", material.getReflectance());
                this.uniforms.setUniform("material.diffuse", material.getDiffuseColor());
                final Texture texture = textureCache.getTexture(material.getTexturePath());
                glActiveTexture(GL_TEXTURE0);
                texture.bind();
                for (final Mesh mesh : material.getMeshes()) {
                    glBindVertexArray(mesh.getVaoId());
                    for (final Entity entity : entities) {
                        this.uniforms.setUniform("modelMatrix", entity.getModelMatrix());
                        glDrawElements(GL_TRIANGLES, mesh.getVertexCount(), GL_UNSIGNED_INT, 0);
                    }
                }
            }
        }
        glBindVertexArray(0);
        this.shader.unbind();
        glDisable(GL_BLEND);
    }

    public void cleanup() {
        this.shader.cleanup();
    }

}
