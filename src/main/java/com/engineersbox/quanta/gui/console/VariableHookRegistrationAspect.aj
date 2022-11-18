package com.engineersbox.quanta.gui.console;

import com.engineersbox.quanta.gui.console.hooks.HookBinding;
import com.engineersbox.quanta.gui.console.hooks.VariableHook;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;

public final aspect VariableHookRegistrationAspect {

    pointcut constructorVisit(final Object instance):
            execution(@com.engineersbox.quanta.gui.console.hooks.RegisterInstanceVariableHooks *.new(..)) && this(instance);

    after(final Object instance): constructorVisit(instance) {
        for (final Field field : getAnnotatedFields(instance)) {
            ConsoleWidget.FIELD_INSTANCE_MAPPINGS.computeIfAbsent(
                    field,
                    (final Field k) -> new ArrayList<>()
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
