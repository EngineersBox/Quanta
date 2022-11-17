package com.engineersbox.quanta.gui.console.hooks;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public record HookBinding(Field field,
                          boolean requiresInstance,
                          Method validator) {
}
