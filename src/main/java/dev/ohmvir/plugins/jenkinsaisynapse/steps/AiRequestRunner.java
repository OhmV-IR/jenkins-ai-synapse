package dev.ohmvir.plugins.jenkinsaisynapse.steps;

import dev.ohmvir.plugins.jenkinsaisynapse.api.ModelConversation;
import dev.ohmvir.plugins.jenkinsaisynapse.api.ModelConversationsAction;
import dev.ohmvir.plugins.jenkinsaisynapse.api.client.ModelRequestException;
import dev.ohmvir.plugins.jenkinsaisynapse.api.client.ModelRequestListener;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.InputTextContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.MaxOutputTokensContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.ModelInput;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.ModelRequest;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.StopSequencesContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.SystemPromptContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.TemperatureContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.ThinkingLevelContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.ToolCallResponseContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.TopKContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.TopPContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelRegistry;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelThinkingLevel;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.RegisteredModel;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.CitationContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.FinishReasonContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.ModelOutputKind;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.ModelResponse;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.OutputTextContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.ThinkingContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.TokenUtilizationContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.ToolCallContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.skills.SkillData;
import dev.ohmvir.plugins.jenkinsaisynapse.api.tools.Tool;
import dev.ohmvir.plugins.jenkinsaisynapse.api.tools.ToolRegistry;
import dev.ohmvir.plugins.jenkinsaisynapse.configuration.PromptsConfiguration;
import dev.ohmvir.plugins.jenkinsaisynapse.configuration.prompts.PromptConfiguration;
import hudson.AbortException;
import hudson.ExtensionList;
import hudson.Functions;
import hudson.model.Item;
import hudson.model.Queue;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.queue.Tasks;
import hudson.security.ACL;
import hudson.security.ACLContext;
import hudson.util.ListBoxModel;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import jenkins.model.Jenkins;
import lombok.Setter;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;

/**
 * Builds and executes the request for {@link AiRequestStep} and the freestyle
 * {@link dev.ohmvir.plugins.jenkinsaisynapse.ModelRequestExecutionStep}. A new request is built for every execution,
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
    private @Nullable ModelThinkingLevel thinkingLevel;
    private @Nullable Double topP;
    private @Nullable Integer topK;
    private List<String> stopSequences = List.of();
    private List<String> tools = List.of();
    private List<String> skills = List.of();
    /** Inputs such as workspace files to attach after the prompt text. */
    private List<ModelInput> attachments = List.of();
    /** The outputs the response must contain. Text by default. */
    private List<ModelOutputKind<?>> outputs = List.of(OutputTextContent.KIND);
    /** Bounds the whole request in seconds, on top of the timeout of the model's provider. 0 means no extra bound. */
    private long timeoutSeconds;
    /**
     * The maximum number of model calls, including the ones that return tool results. Null uses the default from the
     * global configuration.
     */
    private @Nullable Integer maxSteps;

    private boolean quiet;

    /**
     * Executes the request, logging progress to the listener, and records the conversation on the build.
     *
     * @throws AbortException if the request is invalid or fails
     * @throws InterruptedException if the build is aborted
     */
    public Result execute(@Nullable Run<?, ?> run, TaskListener listener) throws IOException, InterruptedException {
        ModelRequest request = buildRequest();
        RegisteredModel modelData = selectModel(request);
        PrintStream logger = listener.getLogger();
        if (!quiet) {
            logger.println("[AI] Sending request to " + modelData.getModelId());
        }
        ModelConversation conversation = ModelConversation.inMemory();
        conversation.setModelId(modelData.getModelId());
        request.setAssociatedConversation(conversation);
        Authentication toolAuthentication = getToolAuthentication(run);
        ModelRequestListener requestListener = quiet ? ModelRequestListener.NOOP : new BuildLogListener(logger);
        long timeoutSeconds = effectiveTimeout(modelData.getRequestTimeoutSeconds(), this.timeoutSeconds);
        ExecutorService executor = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "jenkins-ai-model-request");
            thread.setDaemon(true);
            return thread;
        });
        Future<ModelResponse> future = executor.submit(() -> request.executeOrThrow(
                modelData,
                requestListener,
                (tool, toolCall) -> executeTool(tool, toolCall, toolAuthentication, listener)));
        ModelResponse response;
        try {
            response = timeoutSeconds == 0 ? future.get() : future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new AbortException("AI request timed out after " + timeoutSeconds + " seconds");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw interrupted;
            }
            if (cause instanceof ModelRequestException requestException && requestException.getCause() != null) {
                Functions.printStackTrace(requestException.getCause(), logger);
            }
            String message = cause instanceof ModelRequestException requestException
                    ? requestException.getMessage()
                    : cause == null ? "unknown failure" : cause.getMessage();
            throw new AbortException("AI request failed: " + message);
        } catch (InterruptedException e) {
            future.cancel(true);
            throw e;
        } finally {
            executor.shutdownNow();
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
        attachments.forEach(request::addInput);
        if (systemPrompt != null) {
            replaceInput(request, SystemPromptContent.class, new SystemPromptContent(systemPrompt));
        }
        if (temperature != null) {
            replaceInput(request, TemperatureContent.class, new TemperatureContent(temperature));
        }
        if (maxOutputTokens != null) {
            replaceInput(request, MaxOutputTokensContent.class, new MaxOutputTokensContent(maxOutputTokens));
        }
        if (thinkingLevel != null) {
            replaceInput(request, ThinkingLevelContent.class, new ThinkingLevelContent(thinkingLevel));
        }
        if (topP != null) {
            replaceInput(request, TopPContent.class, new TopPContent(topP));
        }
        if (topK != null) {
            replaceInput(request, TopKContent.class, new TopKContent(topK));
        }
        if (!stopSequences.isEmpty()) {
            replaceInput(request, StopSequencesContent.class, new StopSequencesContent(List.copyOf(stopSequences)));
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
        request.expandSkills();
        request.setModelId(model);
        if (maxSteps != null) {
            request.setMaxSteps(maxSteps);
        }
        outputs.forEach(request::requestOutput);
        return request;
    }

    /**
     * @return The shorter of the two timeouts, where 0 means no timeout.
     */
    static long effectiveTimeout(long providerTimeoutSeconds, long stepTimeoutSeconds) {
        if (providerTimeoutSeconds <= 0) {
            return Math.max(stepTimeoutSeconds, 0);
        }
        if (stepTimeoutSeconds <= 0) {
            return providerTimeoutSeconds;
        }
        return Math.min(providerTimeoutSeconds, stepTimeoutSeconds);
    }

    private RegisteredModel selectModel(ModelRequest request) throws AbortException {
        ModelRegistry registry = ModelRegistry.get();
        RegisteredModel selected = registry.findModelForRequest(request);
        if (selected == null && model != null) {
            if (registry.getModel(model).isEmpty()) {
                throw new AbortException("Unknown model '" + model + "'");
            }
            throw new AbortException("Model '" + model + "' does not support this request.");
        }
        if (selected == null) {
            throw new AbortException("No configured model supports this request");
        }
        return selected;
    }

    private static Authentication getToolAuthentication(@Nullable Run<?, ?> run) {
        if (run != null && run.getParent() instanceof Queue.Task task) {
            return Tasks.getAuthenticationOf2(task);
        }
        return Jenkins.getAuthentication2();
    }

    private ToolCallResponseContent executeTool(
            Tool tool, ToolCallContent toolCall, Authentication authentication, TaskListener listener) {
        if (!quiet) {
            listener.getLogger()
                    .println("[AI Synapse] Model called tool '" + tool.getName() + "' as " + authentication.getName()
                            + " with arguments " + toolCall.getArgumentsPrettyJson());
        }
        ToolCallResponseContent response;
        try (ACLContext ignored = ACL.as2(authentication)) {
            response = tool.callTool(toolCall);
        }
        if (!quiet && !response.isSuccessful()) {
            listener.getLogger()
                    .println("[AI Synapse] Tool '" + tool.getName() + "' failed: " + response.getResponseContent());
        }
        return response;
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
                    + ToolRegistry.getAllTools().stream()
                            .map(Tool::getName)
                            .sorted()
                            .collect(Collectors.joining(", ")));
        }
        return tool;
    }

    public static ListBoxModel fillModelItems(@Nullable Item item) {
        ListBoxModel items = new ListBoxModel();
        items.add("(Any model that supports the request)", "");
        if (canConfigure(item)) {
            for (RegisteredModel model : ModelRegistry.get().getModels()) {
                items.add(model.configuration().getModelIdDisplayName(), model.getModelId());
            }
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
            List<Map<String, Object>> citations = new ArrayList<>();
            for (CitationContent citation : response.getOutputs(CitationContent.class)) {
                Map<String, Object> citationMap = new LinkedHashMap<>();
                citationMap.put("citedText", citation.getCitedText());
                citationMap.put("documentIndex", citation.getDocumentIndex());
                citationMap.put("documentTitle", citation.getDocumentTitle());
                citationMap.put("fileId", citation.getFileId());
                citationMap.put("startCharIndex", citation.getStartCharIndex());
                citationMap.put("endCharIndex", citation.getEndCharIndex());
                citations.add(citationMap);
            }
            result.put("citations", citations);
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
