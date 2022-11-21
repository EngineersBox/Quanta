package com.engineersbox.quanta.utils;

import java.util.function.Consumer;
import java.util.function.Function;

public interface UncheckedThrowsAdapter {

    public interface ThrowsFunction<T, R> {
        R apply(final T t) throws Exception;
    }

    public interface ThrowsConsumer<T> {
        void accept(final T t) throws Exception;
    }

    public static <T, R> Function<T, R> unchecked(final ThrowsFunction<T, R> function) {
        return (final T t) -> {
            try {
                return function.apply(t);
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    public static <T> Consumer<T> unchecked(final ThrowsConsumer<T> consumer) {
        return (final T t) -> {
            try {
                consumer.accept(t);
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        };
    }
}
