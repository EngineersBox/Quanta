package com.engineersbox.quanta.gui.console;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Type;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface VariableHook {
    String name();
    Class<? extends Type> type();
    String hookValidator() default "";
}
