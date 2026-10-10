package io.ohmvir.plugins.jenkinsaisynapse.api.tools;

import com.fasterxml.jackson.databind.JsonNode;
import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Create a method in your class with the tool arguments that returns a String or void.
 * Must have a public no-arg constructor, no arguments should be taken in the tool class.
 * getName() should then match this method name
 * and getArguments() should match your arguments (in order).
 */
public abstract class Tool implements Describable<Tool>, ExtensionPoint {
    private final Method toolMethod;
    private final Logger logger;

    public Tool() throws NoSuchMethodException {
        logger = Logger.getLogger(getClass().getName());
        toolMethod = getClass()
                .getMethod(
                        getName(),
                        getArguments().stream()
                                .map(ToolArgumentDescription::getType)
                                .toList()
                                .toArray(new Class<?>[0]));
        ToolRegistry.register(this);
    }

    /**
     * If the value returned by this function doesn't match a function on the class, the tool will fail to instantiate.
     * @return The name of the tool. Must also be the name of a method on the class with the arguments for the tool.
     */
    public abstract String getName();

    /**
     * @return The description of the tool. Should state what it does, and be concise.
     */
    public abstract String getDescription();

    /**
     * @return A list of the arguments to the tool function, their names, types and descriptions.
     */
    public abstract List<ToolArgumentDescription> getArguments();

    /**
     * Calls the tool with the arguments the model passed.
     *
     * @return What the tool returned, or an empty string if it returns nothing.
     * @throws IllegalArgumentException If a required argument is missing or an argument has an unsupported type.
     * @throws Exception Whatever the tool threw.
     */
    public String invoke(JsonNode arguments) throws Exception {
        List<ToolArgumentDescription> descriptions = getArguments();
        Object[] values = new Object[descriptions.size()];
        for (int i = 0; i < values.length; i++) {
            ToolArgumentDescription description = descriptions.get(i);
            JsonNode value = arguments == null ? null : arguments.get(description.getName());
            values[i] = convertArgument(description, value);
        }
        Object returnValue;
        try {
            returnValue = toolMethod.invoke(this, values);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception exception) {
                throw exception;
            }
            throw e;
        }
        return returnValue == null ? "" : returnValue.toString();
    }

    private static @Nullable Object convertArgument(ToolArgumentDescription description, @Nullable JsonNode value) {
        Class<?> type = description.getType();
        if (value == null || value.isNull() || value.isMissingNode()) {
            if (description.isRequired()) {
                throw new IllegalArgumentException("Missing required argument " + description.getName());
            }
            if (type == boolean.class) {
                return false;
            }
            if (type == int.class) {
                return 0;
            }
            if (type == long.class) {
                return 0L;
            }
            if (type == double.class) {
                return 0d;
            }
            if (type == float.class) {
                return 0f;
            }
            return null;
        }
        if (type == double.class || type == Double.class) {
            return value.asDouble();
        } else if (type == int.class || type == Integer.class) {
            return value.asInt();
        } else if (type == boolean.class || type == Boolean.class) {
            return value.asBoolean();
        } else if (type == String.class) {
            return value.isValueNode() ? value.asText() : value.toString();
        } else if (type == long.class || type == Long.class) {
            return value.asLong();
        } else if (type == float.class || type == Float.class) {
            return (float) value.asDouble();
        }
        throw new IllegalArgumentException("Argument " + description.getName() + " has unsupported type "
                + type.getName() + "; use String, int, long, float, double or boolean");
    }

    /**
     * @deprecated Use {@link #invoke(JsonNode)}, which reports failures instead of returning a marker string.
     */
    @Deprecated
    public Optional<String> callTool(JsonNode toolCallParameters) {
        try {
            String result = invoke(toolCallParameters);
            return result.isEmpty() ? Optional.empty() : Optional.of(result);
        } catch (Exception e) {
            logger.severe("Failed to call tool with exception: " + e.getMessage());
            return Optional.of("<tool-call-failure>");
        }
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<Tool> {
        @Override
        public @NonNull String getDisplayName() {
            return "Tool";
        }
    }
}
