package com.engineersbox.quanta.resources.config.shader;

import com.engineersbox.quanta.debug.GLVersion;
import com.engineersbox.quanta.resources.config.shader.provide.ShaderDefine;
import com.engineersbox.quanta.resources.config.shader.provide.ShaderDefineProvider;
import com.engineersbox.quanta.resources.config.shader.provide.ShaderDefineTransformer;
import com.engineersbox.quanta.resources.config.shader.provide.ShaderIncludeLiteral;
import com.engineersbox.quanta.utils.UncheckedThrowsAdapter;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.engineersbox.quanta.utils.UncheckedThrowsAdapter.uncheckedBiConsumer;
import static org.lwjgl.opengl.ARBShadingLanguageInclude.*;

public class ShaderConfig {
    private static final Logger LOGGER = LogManager.getLogger(ShaderConfig.class);
    public static final String SHADER_CONFIG_INCLUDE_NAME = "/quanta/shader_config.incl";
    private static final Reflections REFLECTIONS = new Reflections(new ConfigurationBuilder()
            .addScanners(
                    Scanners.FieldsAnnotated,
                    Scanners.MethodsAnnotated,
                    Scanners.TypesAnnotated
            ).forPackages("com.engineersbox.quanta")
    );
    private final Map<String, Method> definesTransformers;
    private final Map<String, Field> definesFields;
    private final List<Class<?>> definesClasses;
    private final List<Method> definesProviders;

    private boolean bound;

    public ShaderConfig() {
        checkCompatibility();
        this.bound = false;
        this.definesTransformers = retrieveTransformers();
        this.definesFields = retrieveFields();
        this.definesClasses = retrieveClasses();
        this.definesProviders = retrieveProviders();
    }

    private Map<String, Method> retrieveTransformers() {
        return ShaderConfig.REFLECTIONS.getMethodsAnnotatedWith(ShaderDefineTransformer.class)
                .stream()
                .filter((final Method method) -> {
                    if (!Modifier.isStatic(method.getModifiers())) {
                        ShaderConfig.LOGGER.error(
                                "Method {} annotated with @ShaderDefineTransformer is not defined static, skipping",
                                method.getName()
                        );
                        return false;
                    } else if (method.getParameterTypes().length != 1) {
                        ShaderConfig.LOGGER.error(
                                "Method {} expects {} parameters, however @ShaderDefineTransformer methods should take 1",
                                method.getName(),
                                method.getParameterTypes().length
                        );
                        return false;
                    } else if (method.getReturnType().equals(void.class)) {
                        ShaderConfig.LOGGER.error(
                                "Method {} annotated with @ShaderDefineProvider returns void, should return an object",
                                method.getName()
                        );
                        return false;
                    }
                    return true;
                }).collect(Collectors.toMap(
                        (final Method method) -> method.getAnnotation(ShaderDefineTransformer.class).value(),
                        Function.identity()
                ));
    }

    private Map<String, Field> retrieveFields() {
        return ShaderConfig.REFLECTIONS.getFieldsAnnotatedWith(ShaderDefine.class)
                .stream()
                .filter((final Field field) -> {
                    if (!Modifier.isStatic(field.getModifiers())) {
                        ShaderConfig.LOGGER.error(
                                "Field {} annotated with @ShaderDefine is not defined static, skipping",
                                field.getName()
                        );
                        return false;
                    }
                    return true;
                }).collect(Collectors.toMap(
                        (final Field field) -> field.getAnnotation(ShaderDefine.class).name(),
                        Function.identity()
                ));
    }

    private List<Class<?>> retrieveClasses() {
        return ShaderConfig.REFLECTIONS.getTypesAnnotatedWith(ShaderIncludeLiteral.class)
                .stream()
                .filter((final Class<?> clazz) -> {
                    final ShaderIncludeLiteral annotation = clazz.getAnnotation(ShaderIncludeLiteral.class);
                    if (annotation.value().isBlank()) {
                        ShaderConfig.LOGGER.error(
                                "Class {} annotated with @ShaderDefine does not provider any value in data() method",
                                clazz.getName()
                        );
                        return false;
                    }
                    return true;
                }).toList();
    }

    private List<Method> retrieveProviders() {
        return ShaderConfig.REFLECTIONS.getMethodsAnnotatedWith(ShaderDefineProvider.class)
                .stream()
                .filter((final Method method) -> {
                    if (!Modifier.isStatic(method.getModifiers())) {
                        ShaderConfig.LOGGER.error(
                                "Method {} annotated with @ShaderDefineProvider is not defined static, skipping",
                                method.getName()
                        );
                        return false;
                    } else if (method.getParameterTypes().length != 0) {
                        ShaderConfig.LOGGER.error(
                                "Method {} expects {} parameters, however @ShaderDefineProvider methods should take none",
                                method.getName(),
                                method.getParameterTypes().length
                        );
                        return false;
                    } else if (!method.getReturnType().equals(void.class)) {
                        ShaderConfig.LOGGER.error(
                                "Method {} annotated with @ShaderDefineProvider returns {}, should return {}",
                                method.getName(),
                                method.getReturnType().getName(),
                                String.class.getName()
                        );
                        return false;
                    }
                    return true;
                }).toList();
    }

    private void checkCompatibility() {
        if (!GLVersion.isExtensionSupported(ShaderIncludes.GLSL_INCLUDE_EXTENSION_ARB)) {
            throw new IllegalStateException("Shader includes are not supported on this version of OpenGL");
            // TODO: Revert to default defines in shaders
        }
    }

    private String formatConfigString() {
        final StringBuilder sb = new StringBuilder();
        this.definesFields.forEach(uncheckedBiConsumer((final String defineName, final Field field) -> {
            final ShaderDefine annotation = field.getAnnotation(ShaderDefine.class);
            Object value = FieldUtils.readStaticField(field, true);
            if (!annotation.transformer().isBlank()) {
                final Method transformer = this.definesTransformers.get(annotation.transformer());
                transformer.invoke(
                        null,
                        value
                );
                value = transformer.invoke(
                        null,
                        value
                );
            }
            sb.append("#define ")
                    .append(defineName)
                    .append(" ")
                    .append(value)
                    .append("\n");
        }));
        this.definesClasses.forEach(UncheckedThrowsAdapter.uncheckedConsumer((final Class<?> clazz) -> {
            final ShaderIncludeLiteral annotation = clazz.getAnnotation(ShaderIncludeLiteral.class);
            sb.append(annotation.value()).append("\n");
        }));
        this.definesProviders.forEach(UncheckedThrowsAdapter.uncheckedConsumer((final Method method) -> {
            final String value = (String) method.invoke(null);
            sb.append(value).append("\n");
        }));
        return sb.toString();
    }

    public void createNamedString() {
        if (this.bound) {
            throw new IllegalStateException("Config is already bound as a named string");
        }
        final String formattedInclude = formatConfigString();
        glNamedStringARB(
                GL_SHADER_INCLUDE_ARB,
                ShaderConfig.SHADER_CONFIG_INCLUDE_NAME,
                formattedInclude
        );
        ShaderConfig.LOGGER.debug("Created config shader include with name {}", ShaderConfig.SHADER_CONFIG_INCLUDE_NAME);
        this.bound = true;
        // TODO: Need to recompile all shaders if named string is re-created
    }

    public void destroyNamedString() {
        if (!this.bound) {
            throw new IllegalStateException("Cannot destroy unbound config named string");
        }
        glDeleteNamedStringARB(ShaderConfig.SHADER_CONFIG_INCLUDE_NAME);
        this.bound = false;
    }

}
