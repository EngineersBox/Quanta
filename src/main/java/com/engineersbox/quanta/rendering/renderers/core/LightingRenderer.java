package com.engineersbox.quanta.rendering.renderers.core;

import com.engineersbox.quanta.core.Window;
import com.engineersbox.quanta.debug.hooks.HookValidationException;
import com.engineersbox.quanta.debug.hooks.HookValidator;
import com.engineersbox.quanta.debug.hooks.VariableHook;
import com.engineersbox.quanta.rendering.RenderContext;
import com.engineersbox.quanta.rendering.deferred.GBuffer;
import com.engineersbox.quanta.rendering.handler.RenderHandler;
import com.engineersbox.quanta.rendering.handler.RenderPriority;
import com.engineersbox.quanta.rendering.handler.ShaderRenderHandler;
import com.engineersbox.quanta.rendering.handler.ShaderStage;
import com.engineersbox.quanta.rendering.hdr.HDRBuffer;
import com.engineersbox.quanta.rendering.renderers.preprocess.ShadowRenderer;
import com.engineersbox.quanta.rendering.shadow.ShadowCascade;
import com.engineersbox.quanta.resources.assets.object.QuadMesh;
import com.engineersbox.quanta.resources.assets.shader.ShaderModuleData;
import com.engineersbox.quanta.resources.assets.shader.ShaderProgram;
import com.engineersbox.quanta.resources.assets.shader.ShaderType;
import com.engineersbox.quanta.resources.assets.shader.Uniforms;
import com.engineersbox.quanta.resources.config.ConfigHandler;
import com.engineersbox.quanta.scene.Scene;
import com.engineersbox.quanta.scene.atmosphere.Fog;
import com.engineersbox.quanta.scene.lighting.*;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.List;
import java.util.stream.Stream;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL14.GL_FUNC_ADD;
import static org.lwjgl.opengl.GL14.glBlendEquation;
import static org.lwjgl.opengl.GL30.*;

@RenderHandler(
        name = LightingRenderer.RENDERER_NAME,
        priority = RenderPriority.DEFAULT + 1,
        stage = ShaderStage.CORE
)
public class LightingRenderer extends ShaderRenderHandler {

    public static final String RENDERER_NAME = "@quanta__LIGHTING_RENDERER";
    private static final int MAX_POINT_LIGHTS = 5;
    private static final int MAX_SPOT_LIGHTS = 5;
    @VariableHook(name = "lighting.shadows.show_cascades")
    private static boolean SHOW_CASCADES = false;
    @VariableHook(name = "renderer.show_depth")
    private static boolean SHOW_DEPTH = false;
    @VariableHook(name = "lighting.shadows.only")
    private static boolean SHOW_SHADOWS = false;
    @VariableHook(name = "lighting.shadows.factor")
    private static float SHADOW_FACTOR = 0.25f;
    @VariableHook(name = "lighting.shadows.bias")
    private static float SHADOW_BIAS = 0.0005f;
    @VariableHook(
            name = "lighting.bloom.brightness_threshold",
            hookValidator = "brightnessThresholdValidator"
    )
    public static Vector3f BRIGHTNESS_THRESHOLD = new Vector3f(0.2126f, 0.7152f, 0.0722f);

    @HookValidator(name = "brightnessThresholdValidator")
    public static Object scaleHookValidator(final String value) throws HookValidationException {
        if (value == null) {
            throw new HookValidationException("Expected non-null value");
        }
        final String[] splitValue = value.split(",");
        if (splitValue.length != 3) {
            throw new HookValidationException("Invalid vector values, expected format: <float>,<float>,<float>");
        }
        try {
            return new Vector3f(
                    Float.parseFloat(splitValue[0]),
                    Float.parseFloat(splitValue[1]),
                    Float.parseFloat(splitValue[2])
            );
        } catch (final NumberFormatException e) {
            throw new HookValidationException(e.getMessage());
        }
    }

    private final QuadMesh quadMesh;

    public LightingRenderer() {
        super(new ShaderProgram(
                "Lighting",
                new ShaderModuleData("assets/shaders/lighting/lighting.vert", ShaderType.VERTEX),
                new ShaderModuleData("assets/shaders/lighting/lighting.frag", ShaderType.FRAGMENT)
        ));
        this.quadMesh = new QuadMesh();
        createUniforms();
    }

    private void createUniforms() {
        final Uniforms uniforms = super.getUniforms("Lighting");
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
                "showShadows",
                "farPlane",
                "shadowFactor",
                "shadowBias",
                "brightnessThreshold"
        ).forEach(uniforms::createUniform);

        for (int i = 0; i < LightingRenderer.MAX_POINT_LIGHTS; i++) {
            final String name = "pointLights[" + i + "]";
            Stream.of(
                    name + ".position",
                    name + ".color",
                    name + ".intensity",
                    name + ".att.constant",
                    name + ".att.linear",
                    name + ".att.exponent"
            ).forEach(uniforms::createUniform);
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
            ).forEach(uniforms::createUniform);
        }
        for (int i = 0; i < ShadowCascade.SHADOW_MAP_CASCADE_COUNT; i++) {
            Stream.of(
                    "shadowMap_" + i,
                    "shadowCascade[" + i + "]" + ".projectionViewMatrix",
                    "shadowCascade[" + i + "]" + ".splitDistance"
            ).forEach(uniforms::createUniform);
        }
    }

    private void lightingRenderStart(final Window window,
                                     final GBuffer gBuffer,
                                     final HDRBuffer hdrBuffer) {
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, hdrBuffer.getFboId());
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glViewport(0, 0, window.getWidth(), window.getHeight());
        glEnable(GL_BLEND);
        glBlendEquation(GL_FUNC_ADD);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glBindFramebuffer(GL_READ_FRAMEBUFFER, gBuffer.getGBufferId());
    }

    @Override
    public void render(final RenderContext context) {
        lightingRenderStart(
                context.window(),
                context.gBuffer(),
                context.hdrBuffer()
        );
        super.bind("Lighting");
        final Uniforms uniforms = super.getUniforms("Lighting");
        updateLights(context.scene());

        // Bind the G-Buffer textures
        final int[] textureIds = context.gBuffer().getTextureIds();
        final int numTextures = textureIds != null ? textureIds.length : 0;
        for (int i = 0; i < numTextures; i++) {
            glActiveTexture(GL_TEXTURE0 + i);
            glBindTexture(GL_TEXTURE_2D, textureIds[i]);
        }
        uniforms.setUniform(
                "farPlane",
                (float) ConfigHandler.CONFIG.render.camera.zFar
        );
        uniforms.setUniform(
                "showCascades",
                this.SHOW_CASCADES
        );
        uniforms.setUniform(
                "showDepth",
                this.SHOW_DEPTH
        );
        uniforms.setUniform(
                "showShadows",
                this.SHOW_SHADOWS
        );
        uniforms.setUniform(
                "shadowBias",
                this.SHADOW_BIAS
        );
        uniforms.setUniform(
                "shadowFactor",
                this.SHADOW_FACTOR
        );
        uniforms.setUniform(
                "brightnessThreshold",
                this.BRIGHTNESS_THRESHOLD
        );
        uniforms.setUniform(
                "albedoSampler",
                0
        );
        uniforms.setUniform(
                "normalSampler",
                1
        );
        uniforms.setUniform(
                "specularSampler",
                2
        );
        uniforms.setUniform(
                "depthSampler",
                3
        );
        final Fog fog = context.scene().getFog();
        uniforms.setUniform(
                "fog.activeFog",
                fog.isActive() ? 1 : 0
        );
        uniforms.setUniform(
                "fog.color",
                fog.getColor()
        );
        uniforms.setUniform(
                "fog.density",
                fog.getDensity()
        );
        final ShadowRenderer shadowRenderer = (ShadowRenderer) context.attributes().get(ShadowRenderer.RENDERER_NAME);
        if (shadowRenderer == null) {
            throw new IllegalStateException("Unbound shadow renderer, cannot invoke lighting renderer");
        }
        final List<ShadowCascade> cascadeShadows = shadowRenderer.getShadowCascades();
        for (int i = 0; i < ShadowCascade.SHADOW_MAP_CASCADE_COUNT; i++) {
            glActiveTexture(GL_TEXTURE0 + GBuffer.TOTAL_TEXTURES + i);
            uniforms.setUniform("shadowMap_" + i, GBuffer.TOTAL_TEXTURES + i);
            final ShadowCascade cascadeShadow = cascadeShadows.get(i);
            uniforms.setUniform(
                    "shadowCascade[" + i + "]" + ".projectionViewMatrix",
                    cascadeShadow.getProjectionViewMatrix()
            );
            uniforms.setUniform(
                    "shadowCascade[" + i + "]" + ".splitDistance",
                    cascadeShadow.getSplitDistance()
            );
        }
        shadowRenderer.getShadowBuffer().bindTextures(GL_TEXTURE0 + GBuffer.TOTAL_TEXTURES);
        uniforms.setUniform(
                "inverseProjectionMatrix",
                context.scene().getProjection().getInverseProjectionMatrix()
        );
        uniforms.setUniform(
                "inverseViewMatrix",
                context.scene().getCamera().getInverseViewMatrix()
        );
        this.quadMesh.render();
        super.unbind("Lighting");
    }

    private void updateLights(final Scene scene) {
        final Uniforms uniforms = super.getUniforms("Lighting");
        final Matrix4f viewMatrix = scene.getCamera().getViewMatrix();
        final SceneLights sceneLights = scene.getSceneLights();
        final AmbientLight ambientLight = sceneLights.getAmbientLight();
        uniforms.setUniform(
                "ambientLight.factor",
                ambientLight.getIntensity()
        );
        uniforms.setUniform(
                "ambientLight.color",
                ambientLight.getColor()
        );
        final DirectionalLight directionalLight = sceneLights.getDirectionalLight();
        final Vector4f auxDir = new Vector4f(directionalLight.getDirection(), 0);
        auxDir.mul(viewMatrix);
        final Vector3f dir = new Vector3f(auxDir.x, auxDir.y, auxDir.z);
        uniforms.setUniform(
                "directionalLight.color",
                directionalLight.getColor()
        );
        uniforms.setUniform(
                "directionalLight.direction",
                dir
        );
        uniforms.setUniform(
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
        final Uniforms uniforms = super.getUniforms("Lighting");
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
        uniforms.setUniform(
                prefix + ".position",
                lightPosition
        );
        uniforms.setUniform(
                prefix + ".color",
                color
        );
        uniforms.setUniform(
                prefix + ".intensity",
                intensity
        );
        uniforms.setUniform(
                prefix + ".att.constant",
                constant
        );
        uniforms.setUniform(
                prefix + ".att.linear",
                linear
        );
        uniforms.setUniform(
                prefix + ".att.exponent",
                exponent
        );
    }

    private void updateSpotLight(final SpotLight spotLight,
                                 final String prefix,
                                 final Matrix4f viewMatrix) {
        final Uniforms uniforms = super.getUniforms("Lighting");
        PointLight pointLight = null;
        Vector3f coneDirection = new Vector3f();
        float cutoff = 0.0f;
        if (spotLight != null) {
            coneDirection = spotLight.getConeDirection();
            cutoff = spotLight.getCutOff();
            pointLight = spotLight.getPointLight();
        }
        uniforms.setUniform(
                prefix + ".coneDir",
                coneDirection
        );
        uniforms.setUniform(
                prefix + ".coneDir",
                cutoff
        );
        updatePointLight(pointLight, prefix + ".pl", viewMatrix);
    }

    @Override
    public void cleanup() {
        super.cleanup();
        this.quadMesh.cleanup();
    }

}
