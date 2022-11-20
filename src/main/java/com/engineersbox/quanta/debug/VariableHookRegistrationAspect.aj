package com.engineersbox.quanta.debug;

import com.engineersbox.quanta.debug.hooks.VariableHook;
import com.engineersbox.quanta.gui.console.ConsoleWidget;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;

public final aspect VariableHookRegistrationAspect {

    pointcut constructorVisit(final Object instance):
            execution(@com.engineersbox.quanta.debug.hooks.RegisterInstanceVariableHooks *.new(..)) && this(instance);

    after (final Object instance): constructorVisit(instance) {
        for (final Field field : getAnnotatedFields(instance)) {
            VariableHooksState.FIELD_INSTANCE_MAPPINGS.computeIfAbsent(
                    field,
                    (final Field ignored) -> new ArrayList<>()
            ).add(instance);
        }
    }

    private Field[] getAnnotatedFields(final Object instance) {
        return Arrays.stream(instance.getClass().getDeclaredFields())
                .filter((final Field field) -> field.isAnnotationPresent(VariableHook.class))
                .filter((final Field field) -> !Modifier.isStatic(field.getModifiers()))
                .toArray(Field[]::new);
    }

}
