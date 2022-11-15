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

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
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

    private static final TreeModel HOOKS = new DefaultTreeModel(new DefaultMutableTreeNode());

    static {
        final Map<VariableHook, Field> variableHooks = resolveVariableHooks();
        final Map<String, Method> hookValidators = resolveHookValidators();
        int count = 0;
        for (final Map.Entry<VariableHook, Field> entry : variableHooks.entrySet()) {
            final String name = entry.getKey().name();
            if (name.isBlank()) {
                LOGGER.warn("Invalid variable hook name for field [{}]", entry.getValue().getName());
                continue;
            }
            final String[] path = entry.getKey().name().split("\\.");
            final String validatorName = entry.getKey().hookValidator();
            Method hookValidator = null;
            if (!Objects.equals(validatorName, "")) {
                hookValidator = hookValidators.get(validatorName);
            }
            if (hookValidator != null && validateValidatorArgs(
                    name,
                    validatorName,
                    entry.getValue().getType(),
                    hookValidator
            )) {
                continue;
            }
            HOOKS.valueForPathChanged(
                    new TreePath(path),
                    ImmutablePair.of(
                            entry.getValue(),
                            hookValidator
                    )
            );
            count++;
        }
        LOGGER.debug("Resolved {} variable hooks", count);
    }

    private static boolean validateValidatorArgs(final String varHookName,
                                              final String validatorName,
                                              final Class<?> type,
                                              final Method method) {
        final int parameterCount = method.getParameterCount();
        if (parameterCount != 1) {
            LOGGER.error(
                    "Invalid validator [{}] for variable hook [{}]: expected 1 parameter, got {}",
                    validatorName,
                    varHookName,
                    parameterCount
            );
            return false;
        }
        final Class<?> parameterType = method.getParameterTypes()[0];
        if (!parameterType.isAssignableFrom(type)) {
            LOGGER.error(
                    "Invalid validator [{}] for variable hook [{}]: expected parameter of type {}, got {}",
                    validatorName,
                    varHookName,
                    type.getName(),
                    parameterType.getName()
            );
            return false;
        }
        return true;
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
    public boolean handleGUIInput(final Scene scene,
                                  final Window window) {
        return false;
    }
}
