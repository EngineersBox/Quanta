package com.engineersbox.quanta.test;

import com.engineersbox.quanta.core.Window;
import com.engineersbox.quanta.gui.IGUIInstance;
import com.engineersbox.quanta.input.MouseInput;
import com.engineersbox.quanta.scene.Scene;
import com.engineersbox.quanta.scene.lighting.*;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.flag.ImGuiCond;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class LightControls implements IGUIInstance {

    private final float[] ambientColor;
    private final float[] ambientFactor;
    private final float[] directionalConeX;
    private final float[] directionalConeY;
    private final float[] directionalConeZ;
    private final float[] directionalLightColor;
    private final float[] directionalLightIntensity;
    private final float[] directionalLightX;
    private final float[] directionalLightY;
    private final float[] directionalLightZ;
    private final float[] pointLightColor;
    private final float[] pointLightIntensity;
    private final float[] pointLightX;
    private final float[] pointLightY;
    private final float[] pointLightZ;
    private final float[] spotLightColor;
    private final float[] spotLightCutoff;
    private final float[] spotLightIntensity;
    private final float[] spotLightX;
    private final float[] spotLightY;
    private final float[] spotLightZ;

    public LightControls(final Scene scene) {
        final SceneLights sceneLights = scene.getSceneLights();
        final AmbientLight ambientLight = sceneLights.getAmbientLight();
        Vector3f color = ambientLight.getColor();
        this.ambientFactor = new float[]{ambientLight.getIntensity()};
        this.ambientColor = new float[]{color.x, color.y, color.z};
        PointLight pointLight = sceneLights.getPointLights().get(0);
        color = pointLight.getColor();
        Vector3f pos = pointLight.getPosition();
        this.pointLightColor = new float[]{color.x, color.y, color.z};
        this.pointLightX = new float[]{pos.x};
        this.pointLightY = new float[]{pos.y};
        this.pointLightZ = new float[]{pos.z};
        this.pointLightIntensity = new float[]{pointLight.getIntensity()};
        final SpotLight spotLight = sceneLights.getSpotLights().get(0);
        pointLight = spotLight.getPointLight();
        color = pointLight.getColor();
        pos = pointLight.getPosition();
        this.spotLightColor = new float[]{color.x, color.y, color.z};
        this.spotLightX = new float[]{pos.x};
        this.spotLightY = new float[]{pos.y};
        this.spotLightZ = new float[]{pos.z};
        this.spotLightIntensity = new float[]{pointLight.getIntensity()};
        this.spotLightCutoff = new float[]{spotLight.getCutOffAngle()};
        final Vector3f coneDir = spotLight.getConeDirection();
        this.directionalConeX = new float[]{coneDir.x};
        this.directionalConeY = new float[]{coneDir.y};
        this.directionalConeZ = new float[]{coneDir.z};
        final DirectionalLight dirLight = sceneLights.getDirectionalLight();
        color = dirLight.getColor();
        pos = dirLight.getDirection();
        this.directionalLightColor = new float[]{color.x, color.y, color.z};
        this.directionalLightX = new float[]{pos.x};
        this.directionalLightY = new float[]{pos.y};
        this.directionalLightZ = new float[]{pos.z};
        this.directionalLightIntensity = new float[]{dirLight.getIntensity()};
    }

    @Override
    public void drawGUI() {
        ImGui.newFrame();
        ImGui.setNextWindowPos(0, 0, ImGuiCond.Always);
        ImGui.setNextWindowSize(450, 400);

        ImGui.begin("Lights controls");
        if (ImGui.collapsingHeader("Ambient Light")) {
            ImGui.sliderFloat("Ambient factor", this.ambientFactor, 0.0f, 1.0f, "%.2f");
            ImGui.colorEdit3("Ambient color", this.ambientColor);
        }
        if (ImGui.collapsingHeader("Point Light")) {
            ImGui.sliderFloat("Point Light - x", this.pointLightX, -10.0f, 10.0f, "%.2f");
            ImGui.sliderFloat("Point Light - y", this.pointLightY, -10.0f, 10.0f, "%.2f");
            ImGui.sliderFloat("Point Light - z", this.pointLightZ, -10.0f, 10.0f, "%.2f");
            ImGui.colorEdit3("Point Light color", this.pointLightColor);
            ImGui.sliderFloat("Point Light Intensity", this.pointLightIntensity, 0.0f, 1.0f, "%.2f");
        }
        if (ImGui.collapsingHeader("Spot Light")) {
            ImGui.sliderFloat("Spot Light - x", this.spotLightX, -10.0f, 10.0f, "%.2f");
            ImGui.sliderFloat("Spot Light - y", this.spotLightY, -10.0f, 10.0f, "%.2f");
            ImGui.sliderFloat("Spot Light - z", this.spotLightZ, -10.0f, 10.0f, "%.2f");
            ImGui.colorEdit3("Spot Light color", this.spotLightColor);
            ImGui.sliderFloat("Spot Light Intensity", this.spotLightIntensity, 0.0f, 1.0f, "%.2f");
            ImGui.separator();
            ImGui.sliderFloat("Spot Light cutoff", this.spotLightCutoff, 0.0f, 360.0f, "%2.f");
            ImGui.sliderFloat("Dir cone - x", this.directionalConeX, -1.0f, 1.0f, "%.2f");
            ImGui.sliderFloat("Dir cone - y", this.directionalConeY, -1.0f, 1.0f, "%.2f");
            ImGui.sliderFloat("Dir cone - z", this.directionalConeZ, -1.0f, 1.0f, "%.2f");
        }
        if (ImGui.collapsingHeader("Dir Light")) {
            ImGui.sliderFloat("Dir Light - x", this.directionalLightX, -1.0f, 1.0f, "%.2f");
            ImGui.sliderFloat("Dir Light - y", this.directionalLightY, -1.0f, 1.0f, "%.2f");
            ImGui.sliderFloat("Dir Light - z", this.directionalLightZ, -1.0f, 1.0f, "%.2f");
            ImGui.colorEdit3("Dir Light color", this.directionalLightColor);
            ImGui.sliderFloat("Dir Light Intensity", this.directionalLightIntensity, 0.0f, 1.0f, "%.2f");
        }
        ImGui.end();
        ImGui.endFrame();
        ImGui.render();
    }

    @Override
    public boolean handleGUIInput(final Scene scene, final Window window) {
        final ImGuiIO imGuiIO = ImGui.getIO();
        final MouseInput mouseInput = window.getMouseInput();
        final Vector2f mousePos = mouseInput.getCurrentPos();
        imGuiIO.setMousePos(mousePos.x, mousePos.y);
        imGuiIO.setMouseDown(0, mouseInput.isLeftButtonPressed());
        imGuiIO.setMouseDown(1, mouseInput.isRightButtonPressed());
        final boolean consumed = imGuiIO.getWantCaptureMouse() || imGuiIO.getWantCaptureKeyboard();
        if (consumed) {
            final SceneLights sceneLights = scene.getSceneLights();
            final AmbientLight ambientLight = sceneLights.getAmbientLight();
            ambientLight.setIntensity(this.ambientFactor[0]);
            ambientLight.setColor(this.ambientColor[0], this.ambientColor[1], this.ambientColor[2]);
            PointLight pointLight = sceneLights.getPointLights().get(0);
            pointLight.setPosition(this.pointLightX[0], this.pointLightY[0], this.pointLightZ[0]);
            pointLight.setColor(this.pointLightColor[0], this.pointLightColor[1], this.pointLightColor[2]);
            pointLight.setIntensity(this.pointLightIntensity[0]);
            final SpotLight spotLight = sceneLights.getSpotLights().get(0);
            pointLight = spotLight.getPointLight();
            pointLight.setPosition(this.spotLightX[0], this.spotLightY[0], this.spotLightZ[0]);
            pointLight.setColor(this.spotLightColor[0], this.spotLightColor[1], this.spotLightColor[2]);
            pointLight.setIntensity(this.spotLightIntensity[0]);
            spotLight.setCutOffAngle(this.spotLightColor[0]);
            spotLight.setConeDirection(this.directionalConeX[0], this.directionalConeY[0], this.directionalConeZ[0]);
            final DirectionalLight dirLight = sceneLights.getDirectionalLight();
            dirLight.setPosition(this.directionalLightX[0], this.directionalLightY[0], this.directionalLightZ[0]);
            dirLight.setColor(this.directionalLightColor[0], this.directionalLightColor[1], this.directionalLightColor[2]);
            dirLight.setIntensity(this.directionalLightIntensity[0]);
        }
        return consumed;
    }
}