package com.engineersbox.quanta.resources.config.shader.provide;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface ShaderIncludeLiteral {
    /**
     * Literal data to concatenate into named string for inclusion. Is only
     * available annotated types. Does not support the use of a transformer.
     */
    String value();
}
