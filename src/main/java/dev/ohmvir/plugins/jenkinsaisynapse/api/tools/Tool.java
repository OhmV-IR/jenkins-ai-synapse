package dev.ohmvir.plugins.jenkinsaisynapse.api.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.ToolCallResponseContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.ToolCallContent;
import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Annotate your tool class with {@link Extension} so it is registered with Jenkins.
 * Create a method in your class with the tool arguments that returns a String or void.
 * Supported argument types are String, int, long, double, float, boolean and their boxed equivalents.
 * Optional arguments the model omits are passed as null (or the primitive default value).
 * Must have a public no-arg constructor, no arguments should be taken in the tool class.
 * getName() should then match this method name
 * and getArguments() should match your arguments (in order).
 *
 * <h2>Security model</h2>
 * A tool runs with the authentication of the build that made the request, and the model decides when to call it.
 * That authentication is {@link hudson.security.ACL#SYSTEM2} (full administrator rights) unless the Authorize Project
 * plugin or another {@code QueueItemAuthenticator} is configured, so a tool must not assume that its caller is trusted
 * or even a person. The model's choices, including the arguments it passes, can be influenced by untrusted content in
 * the prompt such as pull request diffs, issue text or build logs (prompt injection). Treat every argument as untrusted
 * input, do the least that the tool is for, and check permissions explicitly, for example with
 * {@code Jenkins.get().checkPermission(...)} or {@code item.checkPermission(...)}, instead of relying on the
 * caller being limited. Under {@code SYSTEM2} those checks pass, so a tool that is dangerous should not be offered to
 * builds that run as SYSTEM. The build log warns when tools are attached to a request that runs as SYSTEM.
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
     * Validates the model supplied arguments against {@link #getArguments()} and invokes the tool method.
     * Never throws: any failure (missing or mistyped argument, exception in the tool) is returned as an unsuccessful
     * response so that it can be reported back to the model.
     * @param toolCall The model's call to this tool
     * @return The tool output, or an unsuccessful response describing why the call failed
     */
    public ToolCallResponseContent callTool(ToolCallContent toolCall) {
        String toolUseId = toolCall.getToolUseId();
        JsonNode toolCallParameters = toolCall.getToolArguments();
        if (toolCallParameters == null || toolCallParameters.isNull() || toolCallParameters.isMissingNode()) {
            toolCallParameters = JsonNodeFactory.instance.objectNode();
        }
        if (!toolCallParameters.isObject()) {
            return ToolCallResponseContent.error(toolUseId, this, "Tool arguments must be a JSON object.");
        }
        List<ToolArgumentDescription> arguments = getArguments();
        Set<String> knownNames =
                arguments.stream().map(ToolArgumentDescription::getName).collect(Collectors.toSet());
        Iterator<String> suppliedNames = toolCallParameters.fieldNames();
        while (suppliedNames.hasNext()) {
            String suppliedName = suppliedNames.next();
            if (!knownNames.contains(suppliedName)) {
                return ToolCallResponseContent.error(
                        toolUseId, this, "Unknown argument '" + suppliedName + "' for tool " + getName() + ".");
            }
        }

        Object[] values = new Object[arguments.size()];
        for (int i = 0; i < arguments.size(); i++) {
            ToolArgumentDescription argument = arguments.get(i);
            JsonNode jsonArg = toolCallParameters.get(argument.getName());
            if (jsonArg == null || jsonArg.isNull()) {
                if (argument.isRequired()) {
                    return ToolCallResponseContent.error(
                            toolUseId, this, "Missing required argument '" + argument.getName() + "'.");
                }
                values[i] = defaultValue(argument.getType());
                continue;
            }
            Optional<Object> value = convertArgument(jsonArg, argument.getType());
            if (value.isEmpty()) {
                return ToolCallResponseContent.error(
                        toolUseId,
                        this,
                        "Argument '" + argument.getName() + "' must be of type "
                                + argument.getType().getSimpleName() + ".");
            }
            values[i] = value.get();
        }

        Object returnValue;
        try {
            returnValue = toolMethod.invoke(this, values);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            logger.log(Level.WARNING, "Tool " + getName() + " threw an exception", cause);
            return ToolCallResponseContent.error(
                    toolUseId,
                    this,
                    "Tool " + getName() + " failed: " + cause.getClass().getSimpleName()
                            + (cause.getMessage() == null ? "" : ": " + cause.getMessage()));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to invoke tool " + getName(), e);
            return ToolCallResponseContent.error(toolUseId, this, "Tool " + getName() + " could not be invoked.");
        }
        if (returnValue == null) {
            return ToolCallResponseContent.success(toolUseId, this, "");
        }
        if (returnValue instanceof String returnValueStr) {
            return ToolCallResponseContent.success(toolUseId, this, returnValueStr);
        }
        logger.severe("Tool " + getName() + " returned unsupported type "
                + returnValue.getClass().getName() + ", only String is supported.");
        return ToolCallResponseContent.error(
                toolUseId, this, "Tool " + getName() + " returned an unsupported result type.");
    }

    private static @Nullable Object defaultValue(Class<?> type) {
        if (type.equals(int.class)) {
            return 0;
        } else if (type.equals(long.class)) {
            return 0L;
        } else if (type.equals(double.class)) {
            return 0d;
        } else if (type.equals(float.class)) {
            return 0f;
        } else if (type.equals(boolean.class)) {
            return false;
        }
        return null;
    }

    private Optional<Object> convertArgument(JsonNode jsonArg, Class<?> type) {
        if (type.equals(String.class)) {
            return jsonArg.isTextual() ? Optional.of(jsonArg.textValue()) : Optional.empty();
        } else if (type.equals(int.class) || type.equals(Integer.class)) {
            return jsonArg.isIntegralNumber() && jsonArg.canConvertToInt()
                    ? Optional.of(jsonArg.intValue())
                    : Optional.empty();
        } else if (type.equals(long.class) || type.equals(Long.class)) {
            return jsonArg.isIntegralNumber() && jsonArg.canConvertToLong()
                    ? Optional.of(jsonArg.longValue())
                    : Optional.empty();
        } else if (type.equals(double.class) || type.equals(Double.class)) {
            return jsonArg.isNumber() ? Optional.of(jsonArg.doubleValue()) : Optional.empty();
        } else if (type.equals(float.class) || type.equals(Float.class)) {
            return jsonArg.isNumber() ? Optional.of(jsonArg.floatValue()) : Optional.empty();
        } else if (type.equals(boolean.class) || type.equals(Boolean.class)) {
            return jsonArg.isBoolean() ? Optional.of(jsonArg.booleanValue()) : Optional.empty();
        }
        logger.severe("Tool " + getClass().getName() + " had an argument of type " + type.getName()
                + " which was not supported. Please use one of the supported types.");
        return Optional.empty();
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<Tool> {
        @Override
        public @NonNull String getDisplayName() {
            return "Tool";
        }
    }
}
