package com.engineersbox.quanta.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EnumSetUtils {

    private EnumSetUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static <T extends Enum<T>> Collector<T, ?, Map<T, Long>> counting(final Class<T> type) {
        return Collectors.toMap(
                Function.identity(),
                (final T ignored) -> 1L,
                Long::sum,
                () -> new HashMap<>(Stream.of(type.getEnumConstants()).collect(Collectors.toMap(Function.identity(), (final T ignored) -> 0L)))
        );
    }

}
