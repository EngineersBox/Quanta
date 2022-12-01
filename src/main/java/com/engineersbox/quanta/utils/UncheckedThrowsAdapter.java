package com.engineersbox.quanta.utils;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public interface UncheckedThrowsAdapter {

    interface ThrowsFunction<T, R> {
        R apply(final T t) throws Exception;
    }

    interface ThrowsConsumer<T> {
        void accept(final T t) throws Exception;
    }

    interface ThrowsBiConsumer<T, U> {
        void accept(final T t, final U u) throws Exception;
    }

    static <T, R> Function<T, R> uncheckedFunction(final ThrowsFunction<T, R> function) {
        return (final T t) -> {
            try {
                return function.apply(t);
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    static <T> Consumer<T> uncheckedConsumer(final ThrowsConsumer<T> consumer) {
        return (final T t) -> {
            try {
                consumer.accept(t);
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    static <T, U> BiConsumer<T, U> uncheckedBiConsumer(final ThrowsBiConsumer<T, U> consumer) {
        return (final T t, final U u) -> {
            try {
                consumer.accept(t, u);
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

}
