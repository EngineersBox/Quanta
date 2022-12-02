package com.engineersbox.quanta.resources.config.shader.provide;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface ShaderDefine {
    /**
     * Name to supply in {@code #define <name> <value>} statement, where
     * the value is supplied by the value of the annotated field. Can be
     * supplemented with a transformer to apply a transformation to the
     * value stored in the field before being included in the define
     * statement.
     */
    String name() default "";

    /**
     * Name of an {@code @}{@link ShaderDefineTransformer} annotated method to
     * invoke with the value of the annotated field.
     */
    String transformer() default "";
}
