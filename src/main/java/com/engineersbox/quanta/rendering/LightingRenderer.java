package com.engineersbox.quanta.rendering;

import com.engineersbox.quanta.debug.hooks.VariableHook;
import com.engineersbox.quanta.rendering.deferred.GBuffer;
import com.engineersbox.quanta.rendering.shadow.ShadowCascade;
import com.engineersbox.quanta.resources.assets.object.QuadMesh;
import com.engineersbox.quanta.resources.assets.shader.ShaderModuleData;
import com.engineersbox.quanta.resources.assets.shader.ShaderProgram;
import com.engineersbox.quanta.resources.assets.shader.ShaderType;
import com.engineersbox.quanta.resources.assets.shader.Uniforms;
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
import static org.lwjgl.opengl.GL30.glBindVertexArray;

public class LightingRenderer {

    private static final int MAX_POINT_LIGHTS = 5;
    private static final int MAX_SPOT_LIGHTS = 5;
    @VariableHook(name = "renderer.show_cascades")
    private static boolean SHOW_CASCADES = false;
    @VariableHook(name = "renderer.show_depth")
    private static boolean SHOW_DEPTH = false;
    @VariableHook(name = "renderer.show_shadows")
    private static boolean SHOW_SHADOWS = false;
    private final ShaderProgram shader;

    private final QuadMesh quadMesh;
    private Uniforms uniforms;

    public LightingRenderer() {
        this.shader = new ShaderProgram(
                new ShaderModuleData("assets/shaders/lighting/lighting.vert", ShaderType.VERTEX),
                new ShaderModuleData("assets/shaders/lighting/lighting.frag", ShaderType.FRAGMENT)
        );
        this.quadMesh = new QuadMesh();
        createUniforms();
    }

    private void createUniforms() {
        this.uniforms = new Uniforms(this.shader.getProgramId());
        Stream.of(
                "albedoSampler",
                "normalSampler",
                "specularSampler",
                "depthSampler",
                "inverseProjectionMatrix",
                "inverseViewMatrix",
                "ambientLight.factor",
                "ambientLight.color",
                "directionalLight.color",
                "directionalLight.direction",
                "directionalLight.intensity",
                "fog.activeFog",
                "fog.color",
                "fog.density",
                "showCascades",
                "showDepth",
                "showShadows"
        ).forEach(this.uniforms::createUniform);

        for (int i = 0; i < LightingRenderer.MAX_POINT_LIGHTS; i++) {
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
        for (int i = 0; i < LightingRenderer.MAX_SPOT_LIGHTS; i++) {
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
        for (int i = 0; i < ShadowCascade.SHADOW_MAP_CASCADE_COUNT; i++) {
            Stream.of(
                    "shadowMap_" + i,
                    "shadowCascade[" + i + "]" + ".projectionViewMatrix",
                    "shadowCascade[" + i + "]" + ".splitDistance"
            ).forEach(this.uniforms::createUniform);
        }
    }

    public void render(final Scene scene, final ShadowRenderer shadowRender, final GBuffer gBuffer) {
        this.shader.bind();

        updateLights(scene);

        // Bind the G-Buffer textures
        final int[] textureIds = gBuffer.getTextureIds();
        final int numTextures = textureIds != null ? textureIds.length : 0;
        for (int i = 0; i < numTextures; i++) {
            glActiveTexture(GL_TEXTURE0 + i);
            glBindTexture(GL_TEXTURE_2D, textureIds[i]);
        }
        this.uniforms.setUniform(
                "showCascades",
                this.SHOW_CASCADES
        );
        this.uniforms.setUniform(
                "showDepth",
                this.SHOW_DEPTH
        );
        this.uniforms.setUniform(
                "showShadows",
                this.SHOW_SHADOWS
        );
        this.uniforms.setUniform(
                "albedoSampler",
                0
        );
        this.uniforms.setUniform(
                "normalSampler",
                1
        );
        this.uniforms.setUniform(
                "specularSampler",
                2
        );
        this.uniforms.setUniform(
                "depthSampler",
                3
        );
        final Fog fog = scene.getFog();
        this.uniforms.setUniform(
                "fog.activeFog",
                fog.isActive() ? 1 : 0
        );
        this.uniforms.setUniform(
                "fog.color",
                fog.getColor()
        );
        this.uniforms.setUniform(
                "fog.density",
                fog.getDensity()
        );
        final int start = 4;
        final List<ShadowCascade> cascadeShadows = shadowRender.getShadowCascades();
        for (int i = 0; i < ShadowCascade.SHADOW_MAP_CASCADE_COUNT; i++) {
            glActiveTexture(GL_TEXTURE0 + start + i);
            this.uniforms.setUniform("shadowMap_" + i, start + i);
            final ShadowCascade cascadeShadow = cascadeShadows.get(i);
            this.uniforms.setUniform(
                    "shadowCascade[" + i + "]" + ".projectionViewMatrix",
                    cascadeShadow.getProjectionViewMatrix()
            );
            this.uniforms.setUniform(
                    "shadowCascade[" + i + "]" + ".splitDistance",
                    cascadeShadow.getSplitDistance()
            );
        }
        shadowRender.getShadowBuffer().bindTextures(GL_TEXTURE0 + start);
        this.uniforms.setUniform(
                "inverseProjectionMatrix",
                scene.getProjection().getInverseProjectionMatrix()
        );
        this.uniforms.setUniform(
                "inverseViewMatrix",
                scene.getCamera().getInverseViewMatrix()
        );
        glBindVertexArray(this.quadMesh.getVaoId());
        glDrawElements(
                GL_TRIANGLES,
                this.quadMesh.getVertexCount(),
                GL_UNSIGNED_INT,
                0
        );
        this.shader.unbind();
    }

    private void updateLights(final Scene scene) {
        final Matrix4f viewMatrix = scene.getCamera().getViewMatrix();
        final SceneLights sceneLights = scene.getSceneLights();
        final AmbientLight ambientLight = sceneLights.getAmbientLight();
        this.uniforms.setUniform(
                "ambientLight.factor",
                ambientLight.getIntensity()
        );
        this.uniforms.setUniform(
                "ambientLight.color",
                ambientLight.getColor()
        );
        final DirectionalLight directionalLight = sceneLights.getDirectionalLight();
        final Vector4f auxDir = new Vector4f(directionalLight.getDirection(), 0);
        auxDir.mul(viewMatrix);
        final Vector3f dir = new Vector3f(auxDir.x, auxDir.y, auxDir.z);
        this.uniforms.setUniform(
                "directionalLight.color",
                directionalLight.getColor()
        );
        this.uniforms.setUniform(
                "directionalLight.direction",
                dir
        );
        this.uniforms.setUniform(
                "directionalLight.intensity",
                directionalLight.getIntensity()
        );
        final List<PointLight> pointLights = sceneLights.getPointLights();
        final int numPointLights = pointLights.size();
        PointLight pointLight;
        for (int i = 0; i < LightingRenderer.MAX_POINT_LIGHTS; i++) {
            pointLight = i < numPointLights ? pointLights.get(i) : null;
            final String name = "pointLights[" + i + "]";
            updatePointLight(pointLight, name, viewMatrix);
        }
        final List<SpotLight> spotLights = sceneLights.getSpotLights();
        final int numSpotLights = spotLights.size();
        SpotLight spotLight;
        for (int i = 0; i < LightingRenderer.MAX_SPOT_LIGHTS; i++) {
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
        this.uniforms.setUniform(
                prefix + ".position",
                lightPosition
        );
        this.uniforms.setUniform(
                prefix + ".color",
                color
        );
        this.uniforms.setUniform(
                prefix + ".intensity",
                intensity
        );
        this.uniforms.setUniform(
                prefix + ".att.constant",
                constant
        );
        this.uniforms.setUniform(
                prefix + ".att.linear",
                linear
        );
        this.uniforms.setUniform(
                prefix + ".att.exponent",
                exponent
        );
    }

    private void updateSpotLight(final SpotLight spotLight,
                                 final String prefix,
                                 final Matrix4f viewMatrix) {
        PointLight pointLight = null;
        Vector3f coneDirection = new Vector3f();
        float cutoff = 0.0f;
        if (spotLight != null) {
            coneDirection = spotLight.getConeDirection();
            cutoff = spotLight.getCutOff();
            pointLight = spotLight.getPointLight();
        }
        this.uniforms.setUniform(
                prefix + ".coneDir",
                coneDirection
        );
        this.uniforms.setUniform(
                prefix + ".coneDir",
                cutoff
        );
        updatePointLight(pointLight, prefix + ".pl", viewMatrix);
    }

    public void cleanup() {
        this.quadMesh.cleanup();
        this.shader.cleanup();
    }

}
