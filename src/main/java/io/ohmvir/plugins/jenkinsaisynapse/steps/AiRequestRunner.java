package io.ohmvir.plugins.jenkinsaisynapse.steps;

import hudson.AbortException;
import hudson.ExtensionList;
import hudson.Functions;
import hudson.model.Item;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.ListBoxModel;
import io.ohmvir.plugins.jenkinsaisynapse.api.ModelConversation;
import io.ohmvir.plugins.jenkinsaisynapse.api.ModelConversationsAction;
import io.ohmvir.plugins.jenkinsaisynapse.api.client.ModelRequestException;
import io.ohmvir.plugins.jenkinsaisynapse.api.input.InputTextContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.input.MaxOutputTokensContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.input.ModelInput;
import io.ohmvir.plugins.jenkinsaisynapse.api.input.ModelRequest;
import io.ohmvir.plugins.jenkinsaisynapse.api.input.SystemPromptContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.input.TemperatureContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelData;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.FinishReasonContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.ModelResponse;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.OutputTextContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.ThinkingContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.TokenUtilizationContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.ToolCallContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.skills.SkillData;
import io.ohmvir.plugins.jenkinsaisynapse.api.tools.Tool;
import io.ohmvir.plugins.jenkinsaisynapse.api.tools.ToolRegistry;
import io.ohmvir.plugins.jenkinsaisynapse.configuration.PromptsConfiguration;
import io.ohmvir.plugins.jenkinsaisynapse.configuration.prompts.PromptConfiguration;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;
import jenkins.model.Jenkins;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

/**
 * Builds and executes the request for {@link AiRequestStep} and the freestyle
 * {@link io.ohmvir.plugins.jenkinsaisynapse.ModelRequestExecutionStep}. A new request is built for every execution,
 * so nothing is shared between builds.
 */
@Setter
public final class AiRequestRunner {
    private @Nullable String prompt;
    private @Nullable String promptId;
    private @Nullable String model;
    private @Nullable String systemPrompt;
    private @Nullable Double temperature;
    private @Nullable Long maxOutputTokens;
    private List<String> tools = List.of();
    private List<String> skills = List.of();
    private boolean quiet;

    /**
     * Executes the request, logging progress to the listener, and records the conversation on the build.
     *
     * @throws AbortException if the request is invalid or fails
     * @throws InterruptedException if the build is aborted
     */
    public Result execute(@Nullable Run<?, ?> run, TaskListener listener) throws IOException, InterruptedException {
        ModelRequest request = buildRequest();
        ModelData modelData = selectModel(request);
        PrintStream logger = listener.getLogger();
        if (!quiet) {
            logger.println("[AI] Sending request to " + modelData.getModelId());
        }
        ModelConversation conversation = ModelConversation.inMemory();
        conversation.setModelId(modelData.getModelId());
        request.setAssociatedConversation(conversation);
        ModelResponse response;
        try {
            response = request.executeOrThrow(modelData, quiet ? null : new BuildLogListener(logger));
        } catch (ModelRequestException e) {
            if (e.getCause() != null) {
                Functions.printStackTrace(e.getCause(), logger);
            }
            throw new AbortException("AI request failed: " + e.getMessage());
        }
        if (run != null) {
            ModelConversationsAction.record(run, conversation);
        }
        return new Result(response, modelData.getModelId(), conversation.getConversationId());
    }

    ModelRequest buildRequest() throws AbortException {
        if (prompt == null && promptId == null) {
            throw new AbortException("An AI request needs a prompt, a promptId, or both");
        }
        ModelRequest request;
        if (promptId != null) {
            PromptConfiguration basePrompt = PromptsConfiguration.get().getPrompt(promptId);
            if (basePrompt == null) {
                throw new AbortException("No base prompt with id '" + promptId + "' is configured");
            }
            request = basePrompt.createRequest();
        } else {
            request = new ModelRequest();
        }
        if (prompt != null) {
            request.addInput(new InputTextContent(prompt));
        }
        if (systemPrompt != null) {
            replaceInput(request, SystemPromptContent.class, new SystemPromptContent(systemPrompt));
        }
        if (temperature != null) {
            replaceInput(request, TemperatureContent.class, new TemperatureContent(temperature));
        }
        if (maxOutputTokens != null) {
            replaceInput(request, MaxOutputTokensContent.class, new MaxOutputTokensContent(maxOutputTokens));
        }
        for (String toolName : tools) {
            request.attachTool(findTool(toolName));
        }
        for (String skillName : skills) {
            SkillData skill = SkillData.getSkillData(skillName);
            if (skill == null) {
                throw new AbortException("Unknown skill '" + skillName + "'. Available skills: "
                        + SkillData.getAllSkills().stream()
                                .map(SkillData::getSkillName)
                                .sorted()
                                .collect(Collectors.joining(", ")));
            }
            request.attachSkill(skill);
        }
        request.requestOutputType(OutputTextContent.class);
        return request;
    }

    private ModelData selectModel(ModelRequest request) throws AbortException {
        if (model != null) {
            ModelData modelData = ModelData.get(model);
            if (modelData == null) {
                throw new AbortException("Unknown model '" + model + "'. Configured models: "
                        + String.join(", ", new TreeSet<>(ModelData.getModelIds())));
            }
            if (!modelData.supportsRequest(request)) {
                throw new AbortException("Model '" + model + "' does not support this request. Check that it supports"
                        + " the tools, skills, system prompt, temperature and thinking level being used.");
            }
            return modelData;
        }
        ModelData modelData = ModelData.findModelForRequest(request);
        if (modelData == null) {
            throw new AbortException("No configured model supports this request");
        }
        return modelData;
    }

    private static void replaceInput(ModelRequest request, Class<? extends ModelInput> type, ModelInput input) {
        request.getModelInputs().removeIf(type::isInstance);
        request.addInput(input);
    }

    private static Tool findTool(String name) throws AbortException {
        // Looking the tools up as extensions also makes sure they are instantiated, which registers them.
        for (Tool tool : ExtensionList.lookup(Tool.class)) {
            if (name.equals(tool.getName())) {
                return tool;
            }
        }
        Tool tool = ToolRegistry.getTool(name);
        if (tool == null) {
            throw new AbortException("Unknown tool '" + name + "'. Available tools: "
                    + String.join(", ", new TreeSet<>(ToolRegistry.getTools().keySet())));
        }
        return tool;
    }

    public static ListBoxModel fillModelItems(@Nullable Item item) {
        ListBoxModel items = new ListBoxModel();
        items.add("(Any model that supports the request)", "");
        if (canConfigure(item)) {
            items.addAll(ModelData.getAllModelsListBox());
        }
        return items;
    }

    public static ListBoxModel fillPromptIdItems(@Nullable Item item) {
        ListBoxModel items = new ListBoxModel();
        items.add("(None)", "");
        if (canConfigure(item)) {
            for (PromptConfiguration prompt : PromptsConfiguration.get().getPrompts()) {
                items.add(prompt.getPromptId());
            }
        }
        return items;
    }

    private static boolean canConfigure(@Nullable Item item) {
        return item == null ? Jenkins.get().hasPermission(Jenkins.ADMINISTER) : item.hasPermission(Item.CONFIGURE);
    }

    /**
     * The outcome of a request.
     */
    public static final class Result {
        private final ModelResponse response;
        private final String modelId;
        private final UUID conversationId;

        Result(ModelResponse response, String modelId, UUID conversationId) {
            this.response = response;
            this.modelId = modelId;
            this.conversationId = conversationId;
        }

        public ModelResponse getResponse() {
            return response;
        }

        public String getText() {
            return response.getText();
        }

        /**
         * @return The response as plain, serializable values, for returning from a Pipeline step.
         */
        public Map<String, Object> toMap() {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("text", getText());
            result.put(
                    "thinking",
                    response.getOutputs(ThinkingContent.class).stream()
                            .map(ThinkingContent::getThinking)
                            .collect(Collectors.joining("\n\n")));
            List<Map<String, Object>> toolCalls = new ArrayList<>();
            for (ToolCallContent toolCall : response.getOutputs(ToolCallContent.class)) {
                Map<String, Object> call = new LinkedHashMap<>();
                call.put("id", toolCall.getToolUseId());
                call.put("name", toolCall.getName());
                call.put(
                        "arguments",
                        toolCall.getToolArguments() == null
                                ? "{}"
                                : toolCall.getToolArguments().toString());
                toolCalls.add(call);
            }
            result.put("toolCalls", toolCalls);
            List<FinishReasonContent> finishReasons = response.getOutputs(FinishReasonContent.class);
            result.put(
                    "finishReason",
                    finishReasons.isEmpty()
                            ? null
                            : String.valueOf(finishReasons.getLast().getReason()));
            List<TokenUtilizationContent> usages = response.getOutputs(TokenUtilizationContent.class);
            if (usages.isEmpty()) {
                result.put("usage", null);
            } else {
                Map<String, Object> usage = new LinkedHashMap<>();
                usage.put(
                        "inputTokens",
                        usages.stream()
                                .mapToLong(TokenUtilizationContent::getInputTokensUsed)
                                .sum());
                usage.put(
                        "outputTokens",
                        usages.stream()
                                .mapToLong(TokenUtilizationContent::getOutputTokensUsed)
                                .sum());
                usage.put(
                        "cachedTokens",
                        usages.stream()
                                .mapToLong(TokenUtilizationContent::getCachedTokensUsed)
                                .sum());
                result.put("usage", usage);
            }
            result.put("model", modelId);
            result.put("conversationId", conversationId.toString());
            return result;
        }
    }
}
