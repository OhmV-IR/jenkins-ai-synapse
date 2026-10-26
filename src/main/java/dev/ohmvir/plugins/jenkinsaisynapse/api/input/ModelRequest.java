package dev.ohmvir.plugins.jenkinsaisynapse.api.input;

import dev.ohmvir.plugins.jenkinsaisynapse.api.ModelContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.client.ModelClient;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelData;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelInputType;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelOutputType;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.ModelOutput;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.ModelOutputDescriptor;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.ModelResponse;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.ToolCallContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.skills.ReadSkillFileTool;
import dev.ohmvir.plugins.jenkinsaisynapse.api.skills.SkillData;
import dev.ohmvir.plugins.jenkinsaisynapse.api.tools.Tool;
import dev.ohmvir.plugins.jenkinsaisynapse.api.tools.ToolRegistry;
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
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import jenkins.model.Jenkins;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;

public class ModelRequest implements Cloneable, Describable<ModelRequest>, ExtensionPoint {
    private static final Logger LOGGER = Logger.getLogger(ModelRequest.class.getName());
    private static final int MAX_LOGGED_LENGTH = 2000;

    private final Set<Class<? extends ModelOutput>> requestedOutputTypes = new HashSet<>();
    private final @Getter List<ModelInput> modelInputs = new ArrayList<>();
    /**
     * The {@code allowed-tools} of the skills {@link #expandSkillInputs()} replaced with the skill tool, so that they keep
     * restricting the available tools after the skills are removed from the inputs.
     */
    private final Set<String> expandedSkillsAllowedTools = new HashSet<>();

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
        if (skill == null) {
            throw new IllegalArgumentException("skill parameter should not be null");
        }
        addInput(new InputSkillContent(skill));
    }

    /**
     * Attach the conversation from a previous response so the model can continue it. Conversations are not stored
     * anywhere by this plugin, so the caller is responsible for keeping the previous response around.
     * @param previousResponse The response whose conversation should be continued
     */
    public void continueConversation(ModelResponse previousResponse) {
        addInput(new InputConversationContent(previousResponse));
    }

    public void attachSkill(String skillName) {
        attachSkill(SkillData.getSkillData(skillName));
    }

    /**
     * The tools the model may call while executing this request: the tools attached to this request, further limited
     * to the {@code allowed-tools} of the attached skills when any attached skill declares them. The built-in
     * {@link ReadSkillFileTool} is never restricted, as the model needs it to read the skills themselves.
     * Model clients should advertise exactly these tools to the model, any other tool call is rejected.
     * @return The tools the model may call, keyed by name
     */
    public Map<String, Tool> getAvailableTools() {
        Map<String, Tool> attachedTools = new LinkedHashMap<>();
        Set<String> skillAllowedTools = new HashSet<>(expandedSkillsAllowedTools);
        for (ModelInput input : modelInputs) {
            if (input instanceof InputToolContent toolContent && toolContent.getTool() != null) {
                attachedTools.put(toolContent.getTool().getName(), toolContent.getTool());
            } else if (input instanceof InputSkillContent skillContent) {
                skillAllowedTools.addAll(getAllowedTools(skillContent.getSkill()));
            }
        }
        if (!skillAllowedTools.isEmpty()) {
            skillAllowedTools.add(ReadSkillFileTool.NAME);
            attachedTools.keySet().retainAll(skillAllowedTools);
        }
        return attachedTools;
    }

    private static List<String> getAllowedTools(@Nullable SkillData skill) {
        List<String> allowedTools = skill == null ? null : skill.getAllowedTools();
        if (allowedTools == null) {
            return List.of();
        }
        return allowedTools.stream().filter(name -> !name.isBlank()).toList();
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
            expandSkillInputs();
            List<ModelOutput> currentOutputs = new ArrayList<>();
            List<ModelContent> conversation = new ArrayList<>();
            modelInputs.stream()
                    .filter(InputConversationContent.class::isInstance)
                    .map(InputConversationContent.class::cast)
                    .forEach(history -> conversation.addAll(history.getHistory()));
            modelInputs.stream()
                    .filter(input -> !(input instanceof InputConversationContent))
                    .forEach(conversation::add);
            while (true) {
                List<ModelOutput> stepOutputs = client.takeStep(
                        model, model.getModelConfiguration(), model.getClientConfiguration(), this, modelInputs);
                if (stepOutputs.isEmpty()) {
                    break;
                }
                currentOutputs.addAll(stepOutputs);
                conversation.addAll(stepOutputs);
                List<ModelInput> newInputs = generateNewInputsForOutputs(stepOutputs, toolAuthentication, listener);
                conversation.addAll(newInputs);
                modelInputs.addAll(newInputs);
            }
            if (requestedOutputTypes.stream()
                    .allMatch(requestedType -> currentOutputs.stream().anyMatch(requestedType::isInstance))) {
                return null;
            }
            return new ModelResponse(currentOutputs, conversation);
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
     * Skills are implemented in the core rather than by model providers: replaces every attached skill with a listing
     * of the skills in the system prompt, and attaches the built-in tool the model uses to read the skills' files.
     * The skills' {@code allowed-tools} keep restricting {@link #getAvailableTools()}.
     */
    void expandSkillInputs() {
        List<SkillData> skills = modelInputs.stream()
                .filter(InputSkillContent.class::isInstance)
                .map(input -> ((InputSkillContent) input).getSkill())
                .distinct()
                .toList();
        if (skills.isEmpty()) {
            return;
        }
        skills.forEach(skill -> expandedSkillsAllowedTools.addAll(getAllowedTools(skill)));
        modelInputs.removeIf(InputSkillContent.class::isInstance);
        String skillsPrompt = ReadSkillFileTool.buildSkillsSystemPrompt(skills);
        Optional<ModelInput> systemPrompt = modelInputs.stream()
                .filter(SystemPromptContent.class::isInstance)
                .findFirst();
        if (systemPrompt.isPresent()) {
            String existingPrompt = ((SystemPromptContent) systemPrompt.get()).getSystemPrompt();
            String combinedPrompt = existingPrompt == null || existingPrompt.isBlank()
                    ? skillsPrompt
                    : existingPrompt + "\n\n" + skillsPrompt;
            modelInputs.set(modelInputs.indexOf(systemPrompt.get()), new SystemPromptContent(combinedPrompt));
        } else {
            modelInputs.add(0, new SystemPromptContent(skillsPrompt));
        }
        boolean skillToolAttached = modelInputs.stream()
                .anyMatch(input -> input instanceof InputToolContent toolInput
                        && ReadSkillFileTool.NAME.equals(toolInput.getTool().getName()));
        if (!skillToolAttached) {
            attachTool(ReadSkillFileTool.get());
        }
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
                newInputs.add(ToolCallResponseContent.error(
                        toolCallOutput.getToolUseId(), null, "Tool '" + toolName + "' is not available."));
                continue;
            }
            logToBuild(
                    listener,
                    "Model called tool '" + toolName + "' as " + toolAuthentication.getName() + " with arguments "
                            + truncate(sanitizeForLog(toolCallOutput.getArgumentsPrettyJson())));
            ToolCallResponseContent response;
            try (ACLContext ignored = ACL.as2(toolAuthentication)) {
                response = tool.callTool(toolCallOutput);
            }
            if (response.isSuccessful()) {
                logToBuild(listener, "Tool '" + toolName + "' succeeded");
            } else {
                LOGGER.warning("Call to tool '" + toolName + "' failed: " + response.getResponseContent());
                logToBuild(
                        listener,
                        "Tool '" + toolName + "' failed: " + truncate(sanitizeForLog(response.getResponseContent())));
            }
            newInputs.add(response);
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
