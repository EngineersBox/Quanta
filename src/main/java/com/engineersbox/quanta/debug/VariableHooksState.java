package com.engineersbox.quanta.debug;

import com.engineersbox.quanta.debug.hooks.*;
import com.engineersbox.quanta.debug.tree.VariableTree;
import com.engineersbox.quanta.utils.reflect.VarHandleUtils;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;

import java.lang.invoke.VarHandle;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class VariableHooksState {

    private VariableHooksState() {
        throw new IllegalStateException("Static state class");
    }

    private static final Logger LOGGER = LogManager.getLogger(VariableHooksState.class);

    private static final Reflections REFLECTIONS = new Reflections(new ConfigurationBuilder()
            .addScanners(Scanners.FieldsAnnotated, Scanners.MethodsAnnotated)
            .forPackages("com.engineersbox.quanta")
    );

    public static final Map<Field, List<Object>> FIELD_INSTANCE_MAPPINGS = new HashMap<>();
    public static final String VARIABLE_PATH_DELIMITER = ".";
    public static final VariableTree<HookBinding> HOOKS = new VariableTree<>(VariableHooksState.VARIABLE_PATH_DELIMITER);

    static {
        final Map<VariableHook, Triple<Field, VarHandle, Boolean>> variableHooks = VariableHooksState.resolveVariableHooks();
        final Map<String, Method> hookValidators = VariableHooksState.resolveHookValidators();
        int count = 0;
        for (final Map.Entry<VariableHook, Triple<Field, VarHandle, Boolean>> entry : variableHooks.entrySet()) {
            final String name = entry.getKey().name();
            final Triple<Field, VarHandle, Boolean> hookValue = entry.getValue();
            if (name.isBlank()) {
                VariableHooksState.LOGGER.warn("Invalid variable hook name for varHandle [{}]", hookValue.getLeft());
                continue;
            }
            final String validatorName = entry.getKey().hookValidator();
            Method hookValidator = null;
            if (!Objects.equals(validatorName, "")) {
                hookValidator = hookValidators.get(validatorName);
            }
            if (hookValidator != null && !VariableHooksState.validateValidatorArgs(
                    name,
                    validatorName,
                    hookValidator
            )) {
                continue;
            }
            VariableHooksState.HOOKS.insert(
                    entry.getKey().name(),
                    new HookBinding(
                            hookValue.getLeft(),
                            hookValue.getMiddle(),
                            hookValue.getRight(),
                            hookValidator
                    )
            );
            count++;
        }
        VariableHooksState.LOGGER.debug("Resolved {} variable hook(s)", count);
    }

    private static boolean validateValidatorArgs(final String varHookName,
                                                 final String validatorName,
                                                 final Method method) {
        final int parameterCount = method.getParameterCount();
        if (parameterCount != 1) {
            VariableHooksState.LOGGER.error(
                    "Invalid validator [{}] for variable hook [{}]: expected 1 parameter, got {}",
                    validatorName,
                    varHookName,
                    parameterCount
            );
            return false;
        }
        final Class<?> parameterType = method.getParameterTypes()[0];
        if (!parameterType.isAssignableFrom(String.class)) {
            VariableHooksState.LOGGER.error(
                    "Invalid validator [{}] for variable hook [{}]: expected parameter of type {}, got {}",
                    validatorName,
                    varHookName,
                    String.class.getName(),
                    parameterType.getName()
            );
            return false;
        }
        return true;
    }

    public static Stream<Field> resolveVariableHooksFields(final boolean logBadState) {
        return VariableHooksState.REFLECTIONS.getFieldsAnnotatedWith(VariableHook.class)
                .stream()
                .filter((final Field field) -> {
                    if (!HookBinding.validateField(field)) {
                        if (logBadState) {
                            VariableHooksState.LOGGER.error(
                                    "Invalid @VariableHook usage. Field [{}] is primitive [{}] and marked as [static final] which is inlined during compilation. Field cannot be altered at runtime, skipping.",
                                    field,
                                    field.getType().getSimpleName()
                            );
                        }
                        return false;
                    } else if (Modifier.isStatic(field.getModifiers())) {
                        return true;
                    }
                    return VariableHooksState.hasConstructorWithRegistrationWrapper(field);
                });
    }

    private static Map<VariableHook, Triple<Field, VarHandle, Boolean>> resolveVariableHooks() {
        return VariableHooksState.resolveVariableHooksFields(true)
                .collect(Collectors.toMap(
                        (final Field field) -> field.getAnnotation(VariableHook.class),
                        (final Field field) -> {
                            final boolean isNotStatic = !Modifier.isStatic(field.getModifiers());
                            final VarHandle varHandle;
                            try {
                                varHandle = VarHandleUtils.resolveVarHandle(
                                        field,
                                        isNotStatic
                                );
                            } catch (final IllegalAccessException | NoSuchFieldException e) {
                                throw new RuntimeException(String.format(
                                        "Cannot resolve varHandle for field [%s]:",
                                        field
                                ), e);
                            }
                            return ImmutableTriple.of(
                                    field,
                                    varHandle,
                                    isNotStatic
                            );
                        }
                ));
    }

    private static boolean hasConstructorWithRegistrationWrapper(final Field field) {
        final Constructor<?>[] constructors = field.getDeclaringClass().getDeclaredConstructors();
        if (constructors.length < 1) {
            return false;
        }
        return Arrays.stream(constructors)
                .anyMatch((final Constructor<?> constructor) -> constructor.isAnnotationPresent(RegisterInstanceVariableHooks.class));
    }

    private static Map<String, Method> resolveHookValidators() {
        return VariableHooksState.REFLECTIONS.getMethodsAnnotatedWith(HookValidator.class)
                .stream()
                .filter((final Method method) -> {
                    final StringBuilder errorBuilder = new StringBuilder();
                    final boolean isStatic = Modifier.isStatic(method.getModifiers());
                    if (!isStatic) {
                        errorBuilder.append("\n\t- Not declared static");
                    }
                    final boolean isBooleanReturn = method.getReturnType().isAssignableFrom(Object.class);
                    if (!isBooleanReturn) {
                        errorBuilder.append("\n\t- Does not return an object");
                    }
                    final boolean acceptsSingleObjectArgument = method.getParameterTypes().length == 1
                            || method.getParameterTypes()[0].isAssignableFrom(String.class);
                    if (!acceptsSingleObjectArgument) {
                        errorBuilder.append("\n\t- Does not accept single string argument");
                    }
                    if (!isStatic || !isBooleanReturn || !acceptsSingleObjectArgument) {
                        VariableHooksState.LOGGER.error(
                                "Invalid hook validator [{}]:{}",
                                method.getName(),
                                errorBuilder
                        );
                        return false;
                    }
                    return true;
                }).collect(Collectors.toMap(
                        (final Method method) -> method.getAnnotation(HookValidator.class).name(),
                        Function.identity()
                ));
    }

    public static Optional<Object> getFieldParentInstance(final Field field,
                                                          final String target) {
        final List<Object> instances = FIELD_INSTANCE_MAPPINGS.get(field);
        return instances.stream()
                .filter((final Object instance) -> InstanceIdentifierProvider.deriveInstanceID(instance).equals(target))
                .findFirst();
    }
}
