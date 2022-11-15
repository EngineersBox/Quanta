package com.engineersbox.quanta.gui.console;

import com.engineersbox.quanta.core.Window;
import com.engineersbox.quanta.gui.IGUIInstance;
import com.engineersbox.quanta.scene.Scene;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Console implements IGUIInstance {

    private static final Logger LOGGER = LogManager.getLogger(Console.class);
    private static final Reflections REFLECTIONS = new Reflections(new ConfigurationBuilder()
            .addScanners(Scanners.FieldsAnnotated, Scanners.MethodsAnnotated)
            .forPackages("com.engineersbox.quanta")
    );

    private static final Map<String, Pair<Field, Method>> VARIABLE_HOOKS;

    static {
        final Map<VariableHook, Field> variableHooks = resolveVariableHooks();
        final Map<String, Method> hookValidators = resolveHookValidators();
        VARIABLE_HOOKS = variableHooks.entrySet()
                .stream()
                .collect(Collectors.toMap(
                        (final Map.Entry<VariableHook, Field> entry) -> entry.getKey().name(),
                        (final Map.Entry<VariableHook, Field> entry) -> {
                            final String validatorName = entry.getKey().hookValidator();
                            Method hookValidator = null;
                            if (!Objects.equals(validatorName, "")) {
                                hookValidator = hookValidators.get(validatorName);
                            }
                            return ImmutablePair.of(
                                    entry.getValue(),
                                    hookValidator
                            );
                        }
                ));
        LOGGER.debug("Resolved {} variable hooks", VARIABLE_HOOKS.size());
    }

    private static Map<VariableHook, Field> resolveVariableHooks() {
        return REFLECTIONS.getFieldsAnnotatedWith(VariableHook.class)
                .stream()
                .collect(Collectors.toMap(
                        (final Field field) -> field.getAnnotation(VariableHook.class),
                        Function.identity()
                ));
    }

    private static Map<String, Method> resolveHookValidators() {
        return REFLECTIONS.getMethodsAnnotatedWith(HookValidator.class)
                .stream()
                .collect(Collectors.toMap(
                        (final Method method) -> method.getAnnotation(HookValidator.class).name(),
                        Function.identity()
                ));
    }

    public Console() {
    }

    @Override
    public void drawGUI() {

    }

    @Override
    public boolean handleGUIInput(final Scene scene, final Window window) {
        return false;
    }
}
