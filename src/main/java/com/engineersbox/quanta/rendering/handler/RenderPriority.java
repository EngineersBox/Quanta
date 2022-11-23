package com.engineersbox.quanta.rendering.handler;

public abstract class RenderPriority {
    public static final int HARD_MIN = Integer.MIN_VALUE;
    public static final int SOFT_MIN = Integer.MIN_VALUE / 2;
    public static final int DEFAULT = 0;
    public static final int SOFT_MAX = Integer.MAX_VALUE / 2;
    public static final int HARD_MAX = Integer.MAX_VALUE;

    private RenderPriority() {
        throw new IllegalStateException("Utility class");
    }

    public static String convertToName(final int value) {
        return switch (value) {
            case HARD_MIN -> "HARD MIN";
            case SOFT_MIN -> "SOFT MIN";
            case DEFAULT -> "DEFAULT";
            case SOFT_MAX -> "SOFT MAX";
            case HARD_MAX -> "HARD MAX";
            default -> null;
        };
    }

}
