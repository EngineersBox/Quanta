package com.engineersbox.quanta.utils.reflect;

import org.apache.commons.lang3.ClassUtils;

import javax.lang.model.type.TypeKind;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.lang.reflect.Type;

public class TypeConversionUtils {

    private TypeConversionUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static Object tryCoercePrimitive(final Class<?> targetType, final Object value) {
        if (!(value instanceof String stringValue)) {
            return value;
        }
        final PropertyEditor editor = PropertyEditorManager.findEditor(targetType);
        if (editor == null) {
            throw new IllegalArgumentException("No matching property editor for type " + targetType.getName());
        }
        editor.setAsText(stringValue);
        return editor.getValue();
    }

}
