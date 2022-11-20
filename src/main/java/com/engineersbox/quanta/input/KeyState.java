package com.engineersbox.quanta.input;

import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

public enum KeyState {
    RELEASED(GLFW_RELEASE),
    PRESSED(GLFW_PRESS),
    NONE(-1);

    private final int glfwState;

    KeyState(final int glfwState) {
        this.glfwState = glfwState;
    }

    public int getGLFWState() {
        return this.glfwState;
    }
}
