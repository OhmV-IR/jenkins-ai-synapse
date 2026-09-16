package io.ohmvir.plugins.jenkinsaisynapse.api.tools;

import com.google.gson.JsonObject;
import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import org.jspecify.annotations.NonNull;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

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

    public Optional<String> callTool(JsonObject toolCallParameters) {
        try {
            Object returnValue = toolMethod.invoke(
                    this,
                    getArguments().stream()
                            .map(argument -> {
                                var jsonArg = toolCallParameters.get(argument.getName());
                                Class<?> tt = argument.getType();
                                if (tt.equals(double.class)) {
                                    return jsonArg.getAsDouble();
                                } else if (tt.equals(int.class)) {
                                    return jsonArg.getAsInt();
                                } else if (tt.equals(boolean.class)) {
                                    return jsonArg.getAsBoolean();
                                } else if (tt.equals(String.class)) {
                                    return jsonArg.getAsString();
                                } else if (tt.equals(long.class)) {
                                    return jsonArg.getAsLong();
                                } else if (tt.equals(float.class)) {
                                    return jsonArg.getAsFloat();
                                } else {
                                    logger.severe(
                                            "Tool " + getClass().getName() + " had an argument of type " + tt.getName()
                                                    + " which was not supported. Please use one of the supported types.");
                                    return null;
                                }
                            })
                            .toArray());
            if (returnValue instanceof String returnValueStr) {
                return returnValueStr.describeConstable();
            } else if (returnValue != null) {
                logger.severe("Tool call returned unsupported return type, only String is supported.");
                return "<tool-call-failure>".describeConstable();
            }
            return Optional.empty();
        } catch (Exception e) {
            logger.severe("Failed to call tool with exception: " + e.getMessage());
            return "<tool-call-failure>".describeConstable();
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
