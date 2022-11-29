package com.engineersbox.quanta.resources.config.shader;

import com.engineersbox.quanta.debug.GLVersion;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.ARBShadingLanguageInclude.*;

public class ShaderConfig {

    private static final String GLSL_INCLUDE_EXTENSION_ARB = "GL_ARB_shading_language_include";
    private static final String SHADER_CONFIG_INCLUDE_NAME = "@quanta__SHADER_CONFIG";
    private static final Reflections REFLECTIONS = new Reflections(new ConfigurationBuilder()
            .addScanners(Scanners.FieldsAnnotated, Scanners.MethodsAnnotated)
            .forPackages("com.engineersbox.quanta")
    );
    public static final Map<Field, List<Object>> FIELD_INSTANCE_MAPPINGS = new HashMap<>();
    private boolean bound;

    public ShaderConfig() {
        checkCompatibility();
        this.bound = false;
    }

    private void checkCompatibility() {
        if (!GLVersion.isExtensionSupported(ShaderConfig.GLSL_INCLUDE_EXTENSION_ARB)) {
            throw new IllegalStateException("Shader includes are not supported on this version of OpenGL");
            // TODO: Revert to default defines in shaders
        }
    }

    private String formatConfigString() {
        final StringBuilder sb = new StringBuilder();
        // TODO: create config
        return sb.toString();
    }

    public void createNamedString() {
        if (this.bound) {
            throw new IllegalStateException("Config is already bound as a named string");
        }
        glNamedStringARB(
                GL_SHADER_INCLUDE_ARB,
                ShaderConfig.SHADER_CONFIG_INCLUDE_NAME,
                formatConfigString()
        );
        this.bound = true;
    }

    public void destroyNamedString() {
        if (!this.bound) {
            throw new IllegalStateException("Cannot destroy unbound config named string");
        }
        glDeleteNamedStringARB(ShaderConfig.SHADER_CONFIG_INCLUDE_NAME);
        this.bound = false;
    }

}
