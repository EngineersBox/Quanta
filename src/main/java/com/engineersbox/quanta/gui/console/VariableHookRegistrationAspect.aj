package com.engineersbox.quanta.gui.console;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;

public final aspect VariableHookRegistrationAspect {

    pointcut constructorVisit(final Object instance):
            execution(@com.engineersbox.quanta.gui.console.RegisterVariableMembers *.new(..)) && this(instance);

    after(final Object instance): constructorVisit(instance) {
        for (final Field field : getAnnotatedFields(instance)) {
            Console.FIELD_INSTANCE_MAPPINGS.computeIfAbsent(
                    field,
                    (final Field k) -> new ArrayList<>()
            ).add(instance.toString());
        }
    }

    private Field[] getAnnotatedFields(final Object instance) {
        return Arrays.stream(instance.getClass().getDeclaredFields())
                .filter((final Field field) -> field.isAnnotationPresent(VariableHook.class))
                .filter((final Field field) -> !field.getAnnotation(VariableHook.class).isStatic())
                .toArray(Field[]::new);
    }

}
