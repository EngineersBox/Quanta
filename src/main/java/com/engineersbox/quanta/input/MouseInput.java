package com.engineersbox.quanta.input;

import org.joml.Vector2f;

import static org.lwjgl.glfw.GLFW.*;

public class MouseInput {

    private final Vector2f currentPos;
    private final Vector2f displayVec;
    private boolean inWindow;
    private boolean leftButtonPressed;
    private final Vector2f previousPos;
    private boolean rightButtonPressed;

    public MouseInput(final long windowHandle) {
        this.previousPos = new Vector2f(-1, -1);
        this.currentPos = new Vector2f();
        this.displayVec = new Vector2f();
        this.leftButtonPressed = false;
        this.rightButtonPressed = false;
        this.inWindow = false;

        glfwSetCursorPosCallback(windowHandle, (handle, xpos, ypos) -> {
            this.currentPos.x = (float) xpos;
            this.currentPos.y = (float) ypos;
        });
        glfwSetCursorEnterCallback(windowHandle, (handle, entered) -> this.inWindow = entered);
        glfwSetMouseButtonCallback(windowHandle, (handle, button, action, mode) -> {
            this.leftButtonPressed = button == GLFW_MOUSE_BUTTON_1 && action == GLFW_PRESS;
            this.rightButtonPressed = button == GLFW_MOUSE_BUTTON_2 && action == GLFW_PRESS;
        });
    }

    public Vector2f getCurrentPos() {
        return this.currentPos;
    }

    public Vector2f getDisplayVec() {
        return this.displayVec;
    }

    public void input() {
        this.displayVec.x = 0;
        this.displayVec.y = 0;
        if (this.previousPos.x > 0 && this.previousPos.y > 0 && this.inWindow) {
            final double deltaX = this.currentPos.x - this.previousPos.x;
            final double deltaY = this.currentPos.y - this.previousPos.y;
            if (deltaX != 0) {
                this.displayVec.y = (float) deltaX;
            }
            if (deltaY != 0) {
                this.displayVec.x = (float) deltaY;
            }
        }
        this.previousPos.x = this.currentPos.x;
        this.previousPos.y = this.currentPos.y;
    }

    public boolean isLeftButtonPressed() {
        return this.leftButtonPressed;
    }

    public boolean isRightButtonPressed() {
        return this.rightButtonPressed;
    }
}
