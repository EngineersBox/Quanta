package com.engineersbox.quanta.test;

import com.engineersbox.quanta.core.Window;
import com.engineersbox.quanta.gui.IGUIInstance;
import com.engineersbox.quanta.gui.console.ConsoleWidget;
import com.engineersbox.quanta.input.MouseInput;
import com.engineersbox.quanta.scene.Scene;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.flag.ImGuiCond;
import org.joml.Vector2f;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_GRAVE_ACCENT;

public class TestConsole implements IGUIInstance {

    private boolean show;
    private boolean tildePressed;
    private final ConsoleWidget console;

    public TestConsole() {
        this.show = false;
        this.tildePressed = false;
        this.console = new ConsoleWidget();
    }

    @Override
    public void drawGUI() {
        if (!this.show) {
            ImGui.newFrame();
            ImGui.endFrame();
            ImGui.render();
            return;
        }
        ImGui.newFrame();
        ImGui.setNextWindowPos(0, 0, ImGuiCond.Always);
        ImGui.setNextWindowSize(450, 400);
        this.console.drawGUI();
        ImGui.endFrame();
        ImGui.render();
    }

    @Override
    public boolean handleGUIInput(final Scene scene,
                                  final Window window) {
        this.console.handleGUIInput(scene, window);
        if (window.isKeyPressed(GLFW_KEY_GRAVE_ACCENT) && !this.tildePressed) {
            this.show = !this.show;
            this.tildePressed = true;
        }
        if (window.isKeyReleased(GLFW_KEY_GRAVE_ACCENT) && this.tildePressed) {
            this.tildePressed = false;
        }
        final ImGuiIO imGuiIO = ImGui.getIO();
        final MouseInput mouseInput = window.getMouseInput();
        final Vector2f mousePos = mouseInput.getCurrentPos();
        imGuiIO.setMousePos(mousePos.x, mousePos.y);
        imGuiIO.setMouseDown(0, mouseInput.isLeftButtonPressed());
        imGuiIO.setMouseDown(1, mouseInput.isRightButtonPressed());
        final boolean consumed = imGuiIO.getWantCaptureMouse() || imGuiIO.getWantCaptureKeyboard();
        if (consumed) {
            // TODO: DO STUFF
        }
        return consumed;
    }
}
