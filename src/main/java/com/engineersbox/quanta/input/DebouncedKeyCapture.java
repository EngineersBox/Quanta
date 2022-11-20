package com.engineersbox.quanta.input;

import com.engineersbox.quanta.core.Window;

import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

public class DebouncedKeyCapture {

    private boolean isPressed;
    private final int keyCode;
    private Runnable onPressHandler;
    private Runnable onReleaseHandler;

    public DebouncedKeyCapture(final int keyCode) {
        this.keyCode = keyCode;
        this.isPressed = false;
    }

    public DebouncedKeyCapture withOnPressHandler(final Runnable onPressHandler) {
        this.onPressHandler = onPressHandler;
        return this;
    }

    public DebouncedKeyCapture withOnReleaseHandler(final Runnable onReleaseHandler) {
        this.onReleaseHandler = onReleaseHandler;
        return this;
    }

    public KeyState update(final Window window) {
        if (window.isKeyPressed(this.keyCode) && !this.isPressed) {
            this.isPressed = true;
            if (this.onPressHandler != null) {
                this.onPressHandler.run();
            }
            return KeyState.PRESSED;
        }
        if (window.isKeyReleased(this.keyCode) && this.isPressed) {
            this.isPressed = false;
            if (this.onReleaseHandler != null) {
                this.onReleaseHandler.run();
            }
            return KeyState.RELEASED;
        }
        return KeyState.NONE;
    }

}
