package com.engineersbox.quanta.utils.reflect;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class VarHandleUtils {

    private VarHandleUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static VarHandle resolveVarHandle(final Field field,
                                             final boolean requiresInstance) throws IllegalAccessException, NoSuchFieldException {
        final MethodHandles.Lookup lookup;
        if (Modifier.isPrivate(field.getModifiers())) {
            lookup = MethodHandles.privateLookupIn(
                    field.getDeclaringClass(),
                    MethodHandles.lookup()
            );
        } else {
            lookup = MethodHandles.lookup().in(field.getDeclaringClass());
        }
        if (requiresInstance) {
            return lookup.findVarHandle(
                    field.getDeclaringClass(),
                    field.getName(),
                    field.getType()
            );
        }
        return lookup.findStaticVarHandle(
                field.getDeclaringClass(),
                field.getName(),
                field.getType()
        );
    }

    public static void setValue(final VarHandle varHandle,
                                final Object instance,
                                final Object value,
                                final boolean requiresInstance) {
        if (requiresInstance) {
            varHandle.set(instance, value);
        } else {
            varHandle.set(value);
        }
    }

    public static Object getValue(final VarHandle varHandle,
                                  final Object instance,
                                  final boolean requiresInstance) {
        if (requiresInstance) {
            return varHandle.get(instance);
        }
        return varHandle.get();
    }

}
