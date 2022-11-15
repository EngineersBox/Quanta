package com.engineersbox.quanta.utils;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class BufferUtils {

    private BufferUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static void putFloats(final FloatBuffer buffer, final float ...values) {
        for (final float floatValue : values) {
            buffer.put(floatValue);
        }
    }

    public static void putFloats(final ByteBuffer buffer, final float ...values) {
        for (final float floatValue : values) {
            buffer.putFloat(floatValue);
        }
    }

    public static void putFloats(final ByteBuffer buffer, final int ...values) {
        for (final float floatValue : values) {
            buffer.putFloat(floatValue);
        }
    }

}
