package com.engineersbox.quanta.resources.config.shader.provide;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface ShaderDefine {
    String name() default "";
    String data() default "";
    String transformer() default "";
}
