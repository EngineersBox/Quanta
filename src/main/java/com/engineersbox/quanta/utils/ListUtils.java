package com.engineersbox.quanta.utils;

import java.util.List;

public class ListUtils {

    private ListUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static float[] floatListToArray(final List<Float> list) {
        final int size = list != null ? list.size() : 0;
        final float[] floatArr = new float[size];
        for (int i = 0; i < size; i++) {
            floatArr[i] = list.get(i);
        }
        return floatArr;
    }

    public static int[] intListToArray(final List<Integer> list) {
        return list.stream().mapToInt((Integer v) -> v).toArray();
    }

}
