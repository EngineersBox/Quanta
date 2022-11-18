package com.engineersbox.quanta.gui.console.hooks;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public record HookBinding(Field field,
                          boolean requiresInstance,
                          Method validator) {

    public static boolean validateField(final Field field) {
        final int modifiers = field.getModifiers();
        return !(Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers) && field.getType().isPrimitive());
    }

}
