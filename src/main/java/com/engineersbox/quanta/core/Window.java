package com.engineersbox.quanta.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.system.MemoryUtil;

import java.util.concurrent.Callable;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL11.GL_TRUE;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Window {

    private static final Logger LOGGER = LogManager.getLogger(Window.class);

    public static class Options {
        public boolean compatibleProfile;
        public int fps;
        public int ups = Engine.TARGET_UPS;
        public int width;
        public int height;
    }

    private final long windowHandle;
    private int height;
    private int width;
    private final Callable<Void> resizeHandler;

    public Window(final String title,
                  final Options options,
                  final Callable<Void> resizeHandler) {
        this.resizeHandler = resizeHandler;
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialise GLFW");
        }
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GL_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GL_TRUE);

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 2);
        if (options.compatibleProfile) {
            glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_COMPAT_PROFILE);
        } else {
            glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
            glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);
        }

        if (options.width > 0 && options.height > 0) {
            this.width = options.width;
            this.height = options.height;
        } else {
            glfwWindowHint(GLFW_MAXIMIZED, GLFW_TRUE);
            final GLFWVidMode videoMode = glfwGetVideoMode(glfwGetPrimaryMonitor());
            if (videoMode == null) {
                throw new RuntimeException("Unable to get video mode of primary monitor");
            }
            this.width = videoMode.width();
            this.height = videoMode.height();
        }

        this.windowHandle = glfwCreateWindow(
                this.width,
                this.height,
                title,
                NULL,
                NULL
        );
        if (this.windowHandle == NULL) {
            throw new RuntimeException("Failed to create GLFW window");
        }

        glfwSetFramebufferSizeCallback(
                this.windowHandle,
                (final long window, final int newWidth, final int newHeight) -> resize(newWidth, newHeight)
        );
        glfwSetErrorCallback((final int errorCode, final long msgPtr) -> Window.LOGGER.error("[GLFW] Error {}: {}", errorCode, MemoryUtil.memUTF8(msgPtr)));
        glfwSetKeyCallback(this.windowHandle, (final long window, final int key, final int scancode, final int action, final int mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
                glfwSetWindowShouldClose(window, true);
            }
        });

        glfwMakeContextCurrent(this.windowHandle);
        glfwSwapInterval(options.fps > 0 ? 0 : 1);
        glfwShowWindow(this.windowHandle);

        final int[] fbWidth = new int[1];
        final int[] fbHeight = new int[1];
        glfwGetFramebufferSize(this.windowHandle, fbWidth, fbHeight);
        this.width = fbWidth[0];
        this.height = fbHeight[0];
    }

    public int getHeight() {
        return this.height;
    }

    public int getWidth() {
        return this.width;
    }

    public boolean isKeyPressed(final int keyCode) {
        return glfwGetKey(this.windowHandle, keyCode) == GLFW_PRESS;
    }

    public static void pollEvents() {
        glfwPollEvents();
    }

    protected void resize(final int newWidth, final int newHeight) {
        this.width = newWidth;
        this.height = newHeight;
        try {
            this.resizeHandler.call();
        } catch (final Exception e) {
            Window.LOGGER.error("Unable to invoke resize callback", e);
        }
    }

    public void update() {
        glfwSwapBuffers(this.windowHandle);
    }

    public boolean windowShouldClose() {
        return glfwWindowShouldClose(this.windowHandle);
    }

    public void cleanup() {
        glfwFreeCallbacks(this.windowHandle);
        glfwDestroyWindow(this.windowHandle);
        glfwTerminate();
        final GLFWErrorCallback callback = glfwSetErrorCallback(null);
        if (callback != null) {
            callback.free();
        }
    }
}
