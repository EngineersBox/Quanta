package com.engineersbox.quanta.utils;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Iterator;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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

    public static <A, B> void zipForEach(final Stream<A> a,
                                         final Stream<B> b,
                                         final BiConsumer<A, B> consumer) {
        final Iterator<A> i1 = a.iterator();
        final Iterator<B> i2 = b.iterator();
        final Iterable<Pair<A, B>> i = () -> new Iterator<>() {
            @Override
            public boolean hasNext() {
                return i1.hasNext() && i2.hasNext();
            }

            @Override
            public Pair<A, B> next() {
                return ImmutablePair.of(i1.next(), i2.next());
            }
        };
        StreamSupport.stream(i.spliterator(), false).forEach((final Pair<A, B> pair) -> consumer.accept(pair.getLeft(), pair.getRight()));
    }

}
