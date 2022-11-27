package com.engineersbox.quanta.debug;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.io.IoBuilder;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GLUtil;
import org.lwjgl.system.Callback;

import java.io.PrintStream;

public abstract class LoggerCompat {

    private LoggerCompat() {
        throw new IllegalStateException("Utility class");
    }

    public static PrintStream asPrintStream(final Logger logger,
                                            final Level level) {
        return new PrintStream(IoBuilder.forLogger(logger).setLevel(level).buildOutputStream());
    }

    @SuppressWarnings("java:S2095")
    public static void registerGLFWErrorLogger(final Logger logger,
                                               final Level level) {
        GLFWErrorCallback.createPrint(LoggerCompat.asPrintStream(logger, level)).set();
    }

    public static Callback registerGLErrorLogger(final Logger logger,
                                                 final Level level) {
        return GLUtil.setupDebugMessageCallback(LoggerCompat.asPrintStream(logger, level));
    }
}
