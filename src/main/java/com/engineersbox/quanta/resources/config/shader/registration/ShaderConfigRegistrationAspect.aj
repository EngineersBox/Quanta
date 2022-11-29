package com.engineersbox.quanta.resources.config.shader.registration;

import com.engineersbox.quanta.resources.config.shader.ShaderConfig;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;

public final aspect ShaderConfigRegistrationAspect {

    pointcut constructorVisit(final Object instance):
            execution(@com.engineersbox.quanta.debug.hooks.RegisterInstanceVariableHooks *.new(..)) && this(instance);

    after (final Object instance): constructorVisit(instance) {
        for (final Field field : getAnnotatedFields(instance)) {
            ShaderConfig.FIELD_INSTANCE_MAPPINGS.computeIfAbsent(
                    field,
                    (final Field ignored) -> new ArrayList<>()
            ).add(instance);
        }
    }

    private Field[] getAnnotatedFields(final Object instance) {
        return Arrays.stream(instance.getClass().getDeclaredFields())
                .filter((final Field field) -> field.isAnnotationPresent(ShaderDefine.class))
                .filter((final Field field) -> !Modifier.isStatic(field.getModifiers()))
                .toArray(Field[]::new);
    }

}
