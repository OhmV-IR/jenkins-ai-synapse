package io.ohmvir.plugins.jenkinsaisynapse.api.input;

import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Queue;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.queue.Tasks;
import hudson.security.ACL;
import hudson.security.ACLContext;
import io.ohmvir.plugins.jenkinsaisynapse.api.ModelContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.ModelConversation;
import io.ohmvir.plugins.jenkinsaisynapse.api.client.ModelClient;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelData;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelInputType;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelOutputType;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.ModelOutput;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.ModelOutputDescriptor;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.ModelResponse;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.ToolCallContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.skills.SkillData;
import io.ohmvir.plugins.jenkinsaisynapse.api.tools.Tool;
import io.ohmvir.plugins.jenkinsaisynapse.api.tools.ToolRegistry;
import io.ohmvir.plugins.jenkinsaisynapse.api.tools.ToolResult;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import jenkins.model.Jenkins;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;

public class ModelRequest implements Cloneable, Describable<ModelRequest>, ExtensionPoint {
    private static final Logger LOGGER = Logger.getLogger(ModelRequest.class.getName());
    private static final int MAX_LOGGED_LENGTH = 2000;

    private final Set<Class<? extends ModelOutput>> requestedOutputTypes = new HashSet<>();
    private final @Getter List<ModelInput> modelInputs = new ArrayList<>();

    @Setter
    private ModelConversation associatedConversation = null;

    public static Set<ModelInputType> getInputTypesFromClass(Class<? extends ModelInput> inputType) {
        ModelInputDescriptor desc = (ModelInputDescriptor) Jenkins.get().getDescriptor(inputType);
        if (desc == null) {
            throw new IllegalArgumentException(inputType.getName() + " is not a valid input type.");
        }
        return desc.getRequiredInputTypes();
    }

    public static Set<ModelOutputType> getOutputTypesFromClass(Class<? extends ModelOutput> outputType) {
        ModelOutputDescriptor desc = (ModelOutputDescriptor) Jenkins.get().getDescriptor(outputType);
        if (desc == null) {
            throw new IllegalArgumentException(outputType.getName() + " is not a valid output type.");
        }
        return desc.getRequiredOutputTypes();
    }

    public static Set<ModelCapability> getRequiredInputCapabilities(Class<? extends ModelInput> inputType) {
        ModelInputDescriptor desc = (ModelInputDescriptor) Jenkins.get().getDescriptor(inputType);
        if (desc == null) {
            throw new IllegalArgumentException(inputType.getName() + " is not a valid input type.");
        }
        return desc.getRequiredCapabilities();
    }

    public static Set<ModelCapability> getRequiredOutputCapabilities(Class<? extends ModelOutput> outputType) {
        ModelOutputDescriptor desc = (ModelOutputDescriptor) Jenkins.get().getDescriptor(outputType);
        if (desc == null) {
            throw new IllegalArgumentException(outputType.getName() + " is not a valid output type.");
        }
        return desc.getRequiredCapabilities();
    }

    public ModelRequest() {}

    public Set<ModelInputType> getInputTypes() {
        return getInputClasses().stream()
                .map(ModelRequest::getInputTypesFromClass)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
    }

    private Set<Class<? extends ModelInput>> getInputClasses() {
        return modelInputs.stream().map(ModelInput::getClass).collect(Collectors.toSet());
    }

    public Set<ModelOutputType> getOutputTypes() {
        return requestedOutputTypes.stream()
                .map(ModelRequest::getOutputTypesFromClass)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
    }

    public Set<Class<? extends ModelOutput>> getOutputClasses() {
        return Collections.unmodifiableSet(requestedOutputTypes);
    }

    public Set<ModelCapability> getRequiredCapabilities() {
        Set<ModelCapability> capabilities = modelInputs.stream()
                .map(ModelInput::getClass)
                .map(ModelRequest::getRequiredInputCapabilities)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
        capabilities.addAll(requestedOutputTypes.stream()
                .map(ModelRequest::getRequiredOutputCapabilities)
                .flatMap(Set::stream)
                .toList());
        return capabilities;
    }

    public void requestOutputType(Class<? extends ModelOutput> outputType) {
        requestedOutputTypes.add(outputType);
    }

    public void addInput(ModelInput input) {
        modelInputs.add(input);
    }

    public void removeOutputTypeRequest(Class<ModelOutput> outputType) {
        requestedOutputTypes.remove(outputType);
    }

    public void removeOutputTypeRequest(ModelOutputType outputType) {
        requestedOutputTypes.removeIf(requestedOutputType ->
                getOutputTypesFromClass(requestedOutputType).contains(outputType));
    }

    public void attachTool(Tool tool) {
        if (tool == null) {
            throw new IllegalArgumentException("tool parameter should not be null");
        }
        addInput(new InputToolContent(tool));
    }

    public boolean hasInputType(Class<?> clazz) {
        return modelInputs.stream().anyMatch(clazz::isInstance);
    }

    @SuppressWarnings("unchecked")
    public <T> @Nullable T getInput(Class<T> clazz) {
        return (T) modelInputs.stream().filter(clazz::isInstance).findFirst().get();
    }

    public void attachTool(String toolName) {
        attachTool(ToolRegistry.getTool(toolName));
    }

    public void attachSkill(SkillData skill) {
        addInput(new InputSkillContent(skill));
    }

    /**
     * The tools the model may call while executing this request: the tools attached to this request, further limited
     * to the {@code allowed-tools} of the attached skills when any attached skill declares them.
     * Model clients should advertise exactly these tools to the model, any other tool call is rejected.
     * @return The tools the model may call, keyed by name
     */
    public Map<String, Tool> getAvailableTools() {
        Map<String, Tool> attachedTools = new LinkedHashMap<>();
        Set<String> skillAllowedTools = new HashSet<>();
        boolean skillsRestrictTools = false;
        for (ModelInput input : modelInputs) {
            if (input instanceof InputToolContent toolContent && toolContent.getTool() != null) {
                attachedTools.put(toolContent.getTool().getName(), toolContent.getTool());
            } else if (input instanceof InputSkillContent skillContent && skillContent.getSkill() != null) {
                List<String> allowedTools = skillContent.getSkill().getAllowedTools();
                if (allowedTools != null && allowedTools.stream().anyMatch(name -> !name.isBlank())) {
                    skillsRestrictTools = true;
                    skillAllowedTools.addAll(allowedTools);
                }
            }
        }
        if (skillsRestrictTools) {
            attachedTools.keySet().retainAll(skillAllowedTools);
        }
        return attachedTools;
    }

    public @Nullable ModelResponse execute() {
        return execute(ModelData.findModelForRequest(this));
    }

    public @Nullable ModelResponse execute(ModelData model) {
        return execute(model, null, null);
    }

    /**
     * Executes the request on behalf of a build. Tools called by the model run as the build's authentication
     * and every tool call is written to the build log.
     * @param run The build this request is executed for
     * @param listener The build's listener, tool calls are logged to it
     */
    public @Nullable ModelResponse execute(@NonNull Run<?, ?> run, @NonNull TaskListener listener) {
        return execute(ModelData.findModelForRequest(this), run, listener);
    }

    /**
     * @param model The model to execute the request with
     * @param run The build this request is executed for. When set, tools called by the model run as the build's
     *            authentication, otherwise as the authentication of the calling thread.
     * @param listener When set, every tool call is written to it
     */
    public @Nullable ModelResponse execute(ModelData model, @Nullable Run<?, ?> run, @Nullable TaskListener listener) {
        try {
            ModelClient<?, ?> client = model.getClient();
            if (client == null) {
                return null;
            }
            Authentication toolAuthentication = getToolAuthentication(run);
            List<ModelOutput> currentOutputs = new ArrayList<>();
            List<ModelContent> finalContent = new ArrayList<>(modelInputs);
            while (true) {
                List<ModelOutput> stepOutputs = client.takeStep(
                        model, model.getModelConfiguration(), model.getClientConfiguration(), this, modelInputs);
                if (stepOutputs.isEmpty()) {
                    break;
                }
                currentOutputs.addAll(stepOutputs);
                finalContent.addAll(stepOutputs);
                List<ModelInput> newInputs = generateNewInputsForOutputs(stepOutputs, toolAuthentication, listener);
                finalContent.addAll(newInputs);
                modelInputs.addAll(newInputs);
            }
            if (requestedOutputTypes.stream()
                    .allMatch(requestedType -> currentOutputs.stream().anyMatch(requestedType::isInstance))) {
                return null;
            }
            if (associatedConversation != null) {
                associatedConversation.addAllContent(finalContent);
            }
            return new ModelResponse(currentOutputs);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to execute ModelRequest", e);
            return null;
        }
    }

    private static Authentication getToolAuthentication(@Nullable Run<?, ?> run) {
        if (run != null && run.getParent() instanceof Queue.Task task) {
            return Tasks.getAuthenticationOf2(task);
        }
        return Jenkins.getAuthentication2();
    }

    /**
     * Used within request execution to generate all necessary inputs from anything the model outputs. Namely, executing tool calls.
     * Only tools returned by {@link #getAvailableTools()} can be called.
     * @param stepOutputs The outputs to generate inputs for
     * @param toolAuthentication The authentication tools are run as
     * @param listener If set, every tool call is logged to it
     * @return The inputs to append to the internal model inputs list
     */
    private List<ModelInput> generateNewInputsForOutputs(
            List<ModelOutput> stepOutputs, Authentication toolAuthentication, @Nullable TaskListener listener) {
        List<ModelInput> newInputs = new ArrayList<>();
        Map<String, Tool> availableTools = getAvailableTools();
        for (ModelOutput modelOutput : stepOutputs) {
            if (!(modelOutput instanceof ToolCallContent toolCallOutput)) {
                continue;
            }
            String toolName = sanitizeForLog(toolCallOutput.getName());
            Tool tool = toolCallOutput.getName() == null ? null : availableTools.get(toolCallOutput.getName());
            if (tool == null) {
                LOGGER.warning("Rejected call to tool '" + toolName + "' which is not available to this request");
                logToBuild(listener, "Rejected call to tool '" + toolName + "': not available to this request");
                newInputs.add(new ToolCallResponseContent(
                        toolCallOutput.getToolUseId(),
                        ToolResult.error("Tool '" + toolName + "' is not available."),
                        null));
                continue;
            }
            logToBuild(
                    listener,
                    "Model called tool '" + toolName + "' as " + toolAuthentication.getName() + " with arguments "
                            + truncate(sanitizeForLog(toolCallOutput.getArgumentsPrettyJson())));
            ToolResult result;
            try (ACLContext ignored = ACL.as2(toolAuthentication)) {
                result = tool.callTool(toolCallOutput.getToolArguments());
            }
            if (result.isError()) {
                LOGGER.warning("Call to tool '" + toolName + "' failed: " + result.content());
                logToBuild(listener, "Tool '" + toolName + "' failed: " + truncate(sanitizeForLog(result.content())));
            } else {
                logToBuild(listener, "Tool '" + toolName + "' succeeded");
            }
            newInputs.add(new ToolCallResponseContent(toolCallOutput.getToolUseId(), result, tool));
        }
        return newInputs;
    }

    private static void logToBuild(@Nullable TaskListener listener, String message) {
        if (listener != null) {
            listener.getLogger().println("[AI Synapse] " + message);
        }
    }

    private static String sanitizeForLog(@Nullable String text) {
        return text == null ? "" : text.replaceAll("[\\p{Cntrl}&&[^\\n\\t]]", "?");
    }

    private static String truncate(String text) {
        return text.length() <= MAX_LOGGED_LENGTH ? text : text.substring(0, MAX_LOGGED_LENGTH) + "... (truncated)";
    }

    /**
     * For convenience, generally flow will be to make a base request with all tools, skills and such and then clone it and add
     * the specific prompt text
     *
     * @return A shallow clone of the prompt. File byte data will be copied, but Skills and tool data which are stored in classes will continue to refer to the old data.
     */
    @Override
    public ModelRequest clone() throws CloneNotSupportedException {
        return (ModelRequest) super.clone();
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<ModelRequest> {
        @Override
        public @NonNull String getDisplayName() {
            return "Model Request";
        }
    }
}
