package com.engineersbox.quanta.utils;

import java.util.function.Consumer;
import java.util.function.Function;

public class StreamUtils {

    private StreamUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static <T> Function<T, T> passThrough(final Consumer<T> consumer) {
        return (final T value) -> {
            consumer.accept(value);
            return value;
        };
    }

}
