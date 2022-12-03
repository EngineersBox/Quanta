package com.engineersbox.quanta.resources.config.shader;

import com.engineersbox.quanta.debug.GLVersion;
import com.engineersbox.quanta.resources.config.shader.provide.ShaderIncludePathProvider;
import com.engineersbox.quanta.utils.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.engineersbox.quanta.utils.UncheckedThrowsAdapter.uncheckedFunction;
import static org.lwjgl.opengl.ARBShadingLanguageInclude.GL_SHADER_INCLUDE_ARB;
import static org.lwjgl.opengl.ARBShadingLanguageInclude.glNamedStringARB;

public class ShaderIncludes {

    private static final Logger LOGGER = LogManager.getLogger(ShaderIncludes.class);
    private static final String NATIVE_PATH_SEPARATOR = System.getProperty("file.separator");
    private static final String PATH_SEPARATOR = "/";
    private static final boolean SEPARATORS_EQUIVALENT = NATIVE_PATH_SEPARATOR == PATH_SEPARATOR;
    public static final String GLSL_INCLUDE_EXTENSION_ARB = "GL_ARB_shading_language_include";
    private static final Reflections REFLECTIONS = new Reflections(new ConfigurationBuilder()
            .addScanners(Scanners.MethodsAnnotated)
            .forPackages("com.engineersbox.quanta")
    );
    private static final List<String> GLSL_FILE_EXTENSIONS = List.of(
            ".glsl",
            ".frag",
            ".vert",
            ".geom",
            ".tess",
            ".comp"
    );

    private final List<String> includeNames;
    private final List<Method> methods;
    private boolean bound;

    public ShaderIncludes() {
        checkCompatibility();
        this.methods = retrieveMethods();
        this.includeNames = new ArrayList<>();
        this.bound = false;
    }

    private void checkCompatibility() {
        if (!GLVersion.isExtensionSupported(ShaderIncludes.GLSL_INCLUDE_EXTENSION_ARB)) {
            throw new IllegalStateException("Shader includes are not supported on this version of OpenGL");
            // TODO: Revert to default defines in shaders
        }
    }

    private List<Method> retrieveMethods() {
        return ShaderIncludes.REFLECTIONS.getMethodsAnnotatedWith(ShaderIncludePathProvider.class)
                .stream()
                .filter((final Method method) -> {
                    if (!Modifier.isStatic(method.getModifiers())) {
                        ShaderIncludes.LOGGER.error(
                                "Method {} annotated with @IncludePathProvider is not static",
                                method.getName()
                        );
                        return false;
                    } else if (method.getParameterTypes().length != 0) {
                        ShaderIncludes.LOGGER.error(
                                "Method {} annotated with @IncludePathProvider should not take any arguments",
                                method.getName()
                        );
                        return false;
                    } else if (!method.getReturnType().isAssignableFrom(String[].class)) {
                        ShaderIncludes.LOGGER.error(
                                "Method {} annotated with @IncludePathProvider should return an array of strings",
                                method.getName()
                        );
                        return false;
                    }
                    return true;
                }).toList();
    }

    private Optional<String> getMatchingFile(final File path) {
        final String pathLiteral = path.getPath();
        if (path.isDirectory() && !path.isFile()
            || GLSL_FILE_EXTENSIONS.stream().noneMatch(pathLiteral::endsWith)) {
            return Optional.empty();
        }
        return Optional.of(pathLiteral);
    }

    private void buildIncludeNames() {
        final List<File> includeFiles = this.methods.stream()
                .map(uncheckedFunction((final Method method) -> (String[]) method.invoke(null)))
                .flatMap(Arrays::stream)
                .map(File::new)
                .toList();
        for (final File file : includeFiles) {
            final Optional<String> glslFile = getMatchingFile(file);
            if (glslFile.isPresent()) {
                this.includeNames.add(glslFile.get());
                continue;
            } else if (!file.isDirectory()) {
                continue;
            }
            try (final Stream<Path> walkStream = Files.walk(file.toPath())) {
                walkStream.map(Path::toFile)
                        .filter(File::isFile)
                        .map(this::getMatchingFile)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .forEach(this.includeNames::add);
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

    private String formatPath(final String path) {
        if (SEPARATORS_EQUIVALENT) {
            return PATH_SEPARATOR + path;
        }
        return String.format(
                "%s%s",
                PATH_SEPARATOR,
                path.replace(NATIVE_PATH_SEPARATOR, PATH_SEPARATOR)
        );
    }

    public void createIncludes() {
        buildIncludeNames();
        for (final String path : this.includeNames) {
            final String fileContents = FileUtils.readFile(path);
            final String formattedPath = formatPath(path);
            glNamedStringARB(
                    GL_SHADER_INCLUDE_ARB,
                    formattedPath,
                    fileContents
            );
            ShaderIncludes.LOGGER.debug("Created named string include for {}", formattedPath);
        }
    }

}
