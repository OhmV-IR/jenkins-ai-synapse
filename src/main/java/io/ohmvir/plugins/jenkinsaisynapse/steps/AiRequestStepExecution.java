package io.ohmvir.plugins.jenkinsaisynapse.steps;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import io.ohmvir.plugins.jenkinsaisynapse.api.ModelConversation;
import io.ohmvir.plugins.jenkinsaisynapse.api.agent.AgentLoop;
import io.ohmvir.plugins.jenkinsaisynapse.api.errors.ModelException;
import io.ohmvir.plugins.jenkinsaisynapse.api.message.CitationPart;
import io.ohmvir.plugins.jenkinsaisynapse.api.message.ContentPart;
import io.ohmvir.plugins.jenkinsaisynapse.api.message.MediaPart;
import io.ohmvir.plugins.jenkinsaisynapse.api.message.Message;
import io.ohmvir.plugins.jenkinsaisynapse.api.message.TextPart;
import io.ohmvir.plugins.jenkinsaisynapse.api.message.ToolCallPart;
import io.ohmvir.plugins.jenkinsaisynapse.api.message.ToolResultPart;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelData;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelOutputType;
import io.ohmvir.plugins.jenkinsaisynapse.api.request.GenerationParams;
import io.ohmvir.plugins.jenkinsaisynapse.api.request.ModelRequest;
import io.ohmvir.plugins.jenkinsaisynapse.api.request.ToolSpec;
import io.ohmvir.plugins.jenkinsaisynapse.api.response.ModelResponse;
import io.ohmvir.plugins.jenkinsaisynapse.api.response.TokenUsage;
import io.ohmvir.plugins.jenkinsaisynapse.api.skills.SkillData;
import io.ohmvir.plugins.jenkinsaisynapse.api.tools.Tool;
import io.ohmvir.plugins.jenkinsaisynapse.api.tools.ToolRegistry;
import io.ohmvir.plugins.jenkinsaisynapse.configuration.BasePromptsManagementLink;
import io.ohmvir.plugins.jenkinsaisynapse.configuration.prompts.PromptConfiguration;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import org.jspecify.annotations.Nullable;

/**
 * Builds a {@link ModelRequest} from the step's parameters and runs it with an {@link AgentLoop} on a background
 * thread, so the build can be aborted while the model is working.
 */
public class AiRequestStepExecution extends SynchronousNonBlockingStepExecution<Object> {
    private static final long serialVersionUID = 1L;

    /**
     * Files larger than this cannot be attached to a request.
     */
    static final long MAX_ATTACHMENT_BYTES = 32L * 1024 * 1024;

    /**
     * The values accepted by {@link AiRequestStep#setOutputTypes(List)}.
     */
    static final Map<String, ModelOutputType> OUTPUT_TYPES = createOutputTypes();

    private static final Map<String, String> CONTENT_TYPES_BY_EXTENSION = Map.ofEntries(
            Map.entry("md", "text/markdown"),
            Map.entry("json", "application/json"),
            Map.entry("yaml", "application/yaml"),
            Map.entry("yml", "application/yaml"),
            Map.entry("xml", "application/xml"),
            Map.entry("csv", "text/csv"),
            Map.entry("html", "text/html"),
            Map.entry("pdf", "application/pdf"),
            Map.entry("png", "image/png"),
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("gif", "image/gif"),
            Map.entry("webp", "image/webp"),
            Map.entry("mp3", "audio/mpeg"),
            Map.entry("wav", "audio/wav"),
            Map.entry("ogg", "audio/ogg"),
            Map.entry("mp4", "video/mp4"),
            Map.entry("webm", "video/webm"),
            Map.entry("mov", "video/quicktime"));

    private final transient AiRequestStep step;

    private static Map<String, ModelOutputType> createOutputTypes() {
        Map<String, ModelOutputType> outputTypes = new LinkedHashMap<>();
        outputTypes.put("text", ModelOutputType.UNSTRUCTURED_TEXT);
        outputTypes.put("image", ModelOutputType.IMAGE);
        outputTypes.put("audio", ModelOutputType.AUDIO);
        outputTypes.put("video", ModelOutputType.VIDEO);
        outputTypes.put("file", ModelOutputType.FILE);
        return Collections.unmodifiableMap(outputTypes);
    }

    AiRequestStepExecution(AiRequestStep step, StepContext context) {
        super(context);
        this.step = step;
    }

    @Override
    protected Object run() throws Exception {
        StepContext context = getContext();
        Run<?, ?> run = Objects.requireNonNull(context.get(Run.class));
        TaskListener listener = Objects.requireNonNull(context.get(TaskListener.class));
        EnvVars env = Objects.requireNonNull(context.get(EnvVars.class));
        PrintStream logger = listener.getLogger();
        if (step.getPrompt() == null && step.getPromptId() == null) {
            throw new AbortException("aiRequest needs a prompt or a promptId");
        }

        ModelRequest.Builder builder = startRequest(env);
        builder.tools(resolveTools(env)).skills(resolveSkills(env));
        List<ContentPart> userParts = new ArrayList<>();
        for (String path : expand(env, step.getFiles())) {
            userParts.add(readAttachment(requireWorkspace(context, "files"), path));
        }
        String prompt = step.getPrompt();
        if (prompt != null) {
            userParts.add(new TextPart(step.isExpandEnvVars() ? env.expand(prompt) : prompt));
        }
        builder.addUserParts(userParts);
        if (step.getOutputTypes() != null) {
            step.getOutputTypes()
                    .forEach(type -> builder.requireOutput(OUTPUT_TYPES.get(type.toLowerCase(Locale.ROOT))));
        }
        ModelConversation conversation = loadConversation(run, env);
        List<Message> history = conversation == null ? List.of() : conversation.getMessages();
        builder.prependMessages(history);
        ModelRequest request = builder.build();

        BuildLogRequestListener requestListener = new BuildLogRequestListener(logger, step.isQuiet(), step.isVerbose());
        AgentLoop loop = AgentLoop.builder()
                .maxSteps(step.getEffectiveMaxTurns())
                .timeout(step.getTimeout() == null ? null : Duration.ofSeconds(step.getTimeout()))
                .listener(requestListener)
                .build();
        ModelResponse response;
        try {
            ModelData model = ModelData.resolve(env.expand(step.getModel()), request);
            if (!step.isQuiet()) {
                logger.println("[aiRequest] Sending request to " + model.getModelId());
            }
            response = loop.run(model, request);
        } catch (ModelException e) {
            if (e.getCause() == null) {
                throw new AbortException(e.getMessage());
            }
            throw e;
        } finally {
            requestListener.finish();
        }

        if (conversation != null) {
            List<Message> added = new ArrayList<>(request.getMessages()
                    .subList(history.size(), request.getMessages().size()));
            added.addAll(response.getNewMessages());
            conversation.addMessages(added);
        }
        String outputDir = env.expand(step.getOutputDir());
        List<Map<String, Object>> outputFiles =
                saveOutputFiles(response, outputDir == null ? null : requireWorkspace(context, "outputDir"), outputDir);
        if (!step.isQuiet()) {
            logger.println("[aiRequest] " + describeCompletion(response));
            if (outputDir == null && !outputFiles.isEmpty()) {
                logger.println("[aiRequest] The model produced " + outputFiles.size()
                        + " files, set outputDir to save them to the workspace");
            }
        }
        if (!step.isReturnResponse()) {
            return response.getText();
        }
        return toMap(response, outputFiles, conversation);
    }

    /**
     * Starts the request from the stored prompt, if there is one, and applies the step's settings over it.
     */
    private ModelRequest.Builder startRequest(EnvVars env) throws AbortException {
        String promptId = env.expand(step.getPromptId());
        ModelRequest.Builder builder = ModelRequest.builder();
        GenerationParams baseParams = GenerationParams.DEFAULTS;
        if (promptId != null) {
            PromptConfiguration promptConfiguration =
                    BasePromptsManagementLink.get().getPromptConfiguration(promptId);
            if (promptConfiguration == null) {
                throw new AbortException("Unknown prompt id '" + promptId + "'. Configured prompts: "
                        + describeNames(BasePromptsManagementLink.get().getPrompts().stream()
                                .map(PromptConfiguration::getPromptId)
                                .toList()));
            }
            ModelRequest stored = promptConfiguration.createRequest();
            baseParams = stored.getParams();
            stored.getRequiredOutputs().forEach(builder::requireOutput);
            builder.tools(stored.getTools()).skills(stored.getSkills());
            if (step.isExpandEnvVars()) {
                builder.systemPrompt(env.expand(stored.getSystemPrompt()));
                stored.getMessages().forEach(message -> builder.message(expandText(message, env)));
            } else {
                builder.systemPrompt(stored.getSystemPrompt()).messages(stored.getMessages());
            }
        }
        if (step.getSystemPrompt() != null) {
            builder.systemPrompt(step.getSystemPrompt());
        }
        GenerationParams.Builder params = baseParams.toBuilder();
        if (step.getTemperature() != null) {
            params.temperature(step.getTemperature());
        }
        if (step.getMaxOutputTokens() != null) {
            params.maxOutputTokens(step.getMaxOutputTokens());
        }
        if (step.getThinkingLevel() != null) {
            params.thinkingLevel(AiRequestStep.parseThinkingLevel(step.getThinkingLevel()));
        }
        if (step.getTopP() != null) {
            params.topP(step.getTopP());
        }
        if (step.getTopK() != null) {
            params.topK(step.getTopK());
        }
        if (step.getStopSequences() != null) {
            params.stopSequences(step.getStopSequences());
        }
        return builder.params(params.build());
    }

    private static Message expandText(Message message, EnvVars env) {
        return new Message(
                message.getRole(),
                message.getParts().stream()
                        .map(part -> part instanceof TextPart text ? new TextPart(env.expand(text.getText())) : part)
                        .toList());
    }

    private List<ToolSpec> resolveTools(EnvVars env) throws AbortException {
        List<ToolSpec> tools = new ArrayList<>();
        for (String toolName : expand(env, step.getTools())) {
            Tool tool = ToolRegistry.getTool(toolName);
            if (tool == null) {
                throw new AbortException("Unknown tool '" + toolName + "'. Available tools: "
                        + describeNames(ToolRegistry.getAllTools().stream()
                                .map(Tool::getName)
                                .sorted()
                                .toList()));
            }
            tools.add(ToolSpec.of(tool));
        }
        return tools;
    }

    private List<SkillData> resolveSkills(EnvVars env) throws AbortException {
        List<SkillData> skills = new ArrayList<>();
        for (String skillName : expand(env, step.getSkills())) {
            SkillData skill = SkillData.getSkillData(skillName);
            if (skill == null) {
                throw new AbortException("Unknown skill '" + skillName + "'. Available skills: "
                        + describeNames(SkillData.getAllSkills().stream()
                                .map(SkillData::getSkillName)
                                .sorted()
                                .toList()));
            }
            skills.add(skill);
        }
        return skills;
    }

    /**
     * Loads the conversation named by the step, if any. Keys other than UUIDs are scoped to the job.
     */
    private @Nullable ModelConversation loadConversation(Run<?, ?> run, EnvVars env) throws IOException {
        String key = env.expand(step.getConversationId());
        if (key == null) {
            return null;
        }
        UUID uuid;
        try {
            uuid = UUID.fromString(key);
        } catch (IllegalArgumentException e) {
            uuid = UUID.nameUUIDFromBytes(
                    (run.getParent().getFullName() + "\n" + key).getBytes(StandardCharsets.UTF_8));
        }
        return new ModelConversation(uuid);
    }

    private static String describeNames(List<String> names) {
        return names.isEmpty() ? "(none)" : String.join(", ", names);
    }

    private static List<String> expand(EnvVars env, @Nullable List<String> values) {
        return values == null ? List.of() : values.stream().map(env::expand).toList();
    }

    private static FilePath requireWorkspace(StepContext context, String parameter)
            throws IOException, InterruptedException {
        FilePath workspace = context.get(FilePath.class);
        if (workspace == null) {
            throw new AbortException("aiRequest needs a workspace for " + parameter + ", use it inside a node block");
        }
        return workspace;
    }

    private static MediaPart readAttachment(FilePath workspace, String path) throws IOException, InterruptedException {
        FilePath file = workspace.child(path);
        if (!file.exists() || file.isDirectory()) {
            throw new AbortException("File to attach does not exist: " + file.getRemote());
        }
        if (file.length() > MAX_ATTACHMENT_BYTES) {
            throw new AbortException("File to attach is larger than " + MAX_ATTACHMENT_BYTES + " bytes: " + path);
        }
        byte[] data;
        try (InputStream in = file.read()) {
            data = in.readAllBytes();
        }
        String contentType = guessContentType(file.getName(), data);
        MediaPart.Kind kind;
        if (contentType.startsWith("image/")) {
            kind = MediaPart.Kind.IMAGE;
        } else if (contentType.startsWith("audio/")) {
            kind = MediaPart.Kind.AUDIO;
        } else if (contentType.startsWith("video/")) {
            kind = MediaPart.Kind.VIDEO;
        } else {
            kind = MediaPart.Kind.FILE;
        }
        return new MediaPart(kind, file.getName(), contentType, data);
    }

    static String guessContentType(String fileName, byte[] data) {
        int dot = fileName.lastIndexOf('.');
        if (dot >= 0) {
            String contentType =
                    CONTENT_TYPES_BY_EXTENSION.get(fileName.substring(dot + 1).toLowerCase(Locale.ROOT));
            if (contentType != null) {
                return contentType;
            }
        }
        String contentType = URLConnection.guessContentTypeFromName(fileName);
        if (contentType != null) {
            return contentType;
        }
        for (int i = 0; i < Math.min(data.length, 8000); i++) {
            if (data[i] == 0) {
                return "application/octet-stream";
            }
        }
        return "text/plain";
    }

    private static String extensionFor(String mediaType) {
        return CONTENT_TYPES_BY_EXTENSION.entrySet().stream()
                .filter(entry -> entry.getValue().equals(mediaType))
                .map(Map.Entry::getKey)
                .sorted()
                .findFirst()
                .orElse(mediaType.startsWith("text/") ? "txt" : "bin");
    }

    /**
     * Describes the images, audio, video and files the model produced, saving them to the output directory if there
     * is one.
     */
    private static List<Map<String, Object>> saveOutputFiles(
            ModelResponse response, @Nullable FilePath workspace, @Nullable String outputDir)
            throws IOException, InterruptedException {
        List<Map<String, Object>> files = new ArrayList<>();
        Set<String> usedNames = new HashSet<>();
        FilePath directory = workspace == null || outputDir == null ? null : workspace.child(outputDir);
        int index = 0;
        for (MediaPart media : response.getMedia()) {
            index++;
            String name = media.getName() == null
                    ? media.getKind().name().toLowerCase(Locale.ROOT) + "-" + index + "."
                            + extensionFor(media.getMediaType())
                    : media.getName();
            name = uniqueFileName(name, index, usedNames);
            Map<String, Object> description = new LinkedHashMap<>();
            description.put("name", name);
            description.put("kind", media.getKind().name());
            description.put("contentType", media.getMediaType());
            description.put("size", media.getSize());
            if (directory != null) {
                directory.mkdirs();
                directory.child(name).copyFrom(new ByteArrayInputStream(media.getData()));
                description.put("path", outputDir + "/" + name);
            }
            files.add(description);
        }
        return files;
    }

    /**
     * Keeps model chosen file names from escaping the output directory or overwriting each other.
     */
    static String uniqueFileName(@Nullable String name, int index, Set<String> usedNames) {
        String fileName = name == null ? "" : name;
        fileName = fileName.substring(Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\')) + 1);
        fileName = fileName.replaceAll("[^A-Za-z0-9._-]", "_").replaceFirst("^\\.+", "");
        if (fileName.isEmpty()) {
            fileName = "output-" + index;
        }
        if (!usedNames.add(fileName)) {
            fileName = index + "-" + fileName;
            usedNames.add(fileName);
        }
        return fileName;
    }

    private static String describeCompletion(ModelResponse response) {
        StringBuilder description = new StringBuilder(response.getModelId()).append(" finished");
        List<String> details = new ArrayList<>();
        details.add(response.getFinishReason().name().toLowerCase(Locale.ROOT));
        TokenUsage usage = response.getUsage();
        if (usage != null) {
            details.add(usage.inputTokens() + " input tokens");
            details.add(usage.outputTokens() + " output tokens");
        }
        return description
                .append(" (")
                .append(String.join(", ", details))
                .append(")")
                .toString();
    }

    /**
     * Converts the response to plain maps, lists and values that Pipeline scripts can use and store.
     */
    private static Map<String, Object> toMap(
            ModelResponse response, List<Map<String, Object>> outputFiles, @Nullable ModelConversation conversation) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("text", response.getText());
        result.put("allText", response.getAllText());
        result.put("thinking", response.getThinking());
        result.put("model", response.getModelId());
        result.put("finishReason", response.getFinishReason().name());
        result.put("steps", response.getSteps());

        TokenUsage usage = response.getUsage();
        if (usage == null) {
            result.put("usage", null);
        } else {
            Map<String, Object> usageMap = new LinkedHashMap<>();
            usageMap.put("inputTokens", usage.inputTokens());
            usageMap.put("outputTokens", usage.outputTokens());
            usageMap.put("cachedTokens", usage.cachedTokens());
            result.put("usage", usageMap);
        }

        Map<String, ToolResultPart> toolResults = new HashMap<>();
        response.getToolResults().forEach(toolResult -> toolResults.put(toolResult.getToolCallId(), toolResult));
        List<Map<String, Object>> toolCalls = new ArrayList<>();
        for (ToolCallPart toolCall : response.getToolCalls()) {
            Map<String, Object> toolCallMap = new LinkedHashMap<>();
            toolCallMap.put("id", toolCall.getId());
            toolCallMap.put("name", toolCall.getName());
            toolCallMap.put("arguments", toJava(toolCall.getArguments()));
            ToolResultPart toolResult = toolResults.get(toolCall.getId());
            toolCallMap.put("successful", toolResult != null && !toolResult.isError());
            toolCallMap.put("result", toolResult == null ? null : toolResult.getContent());
            toolCalls.add(toolCallMap);
        }
        result.put("toolCalls", toolCalls);

        List<Map<String, Object>> citations = new ArrayList<>();
        for (CitationPart citation : response.getParts(CitationPart.class)) {
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
        result.put("files", outputFiles);
        result.put(
                "conversationId",
                conversation == null ? null : conversation.getConversationId().toString());
        return result;
    }

    /**
     * Converts JSON to the plain maps, lists and values Pipeline scripts can use.
     */
    static @Nullable Object toJava(@Nullable JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        if (element instanceof JsonObject object) {
            Map<String, Object> map = new LinkedHashMap<>();
            object.entrySet().forEach(entry -> map.put(entry.getKey(), toJava(entry.getValue())));
            return map;
        }
        if (element instanceof JsonArray array) {
            List<Object> list = new ArrayList<>();
            array.forEach(item -> list.add(toJava(item)));
            return list;
        }
        JsonPrimitive primitive = element.getAsJsonPrimitive();
        if (primitive.isBoolean()) {
            return primitive.getAsBoolean();
        }
        if (primitive.isNumber()) {
            double number = primitive.getAsDouble();
            if (number == Math.rint(number) && Math.abs(number) < 1e15) {
                return primitive.getAsLong();
            }
            return number;
        }
        return primitive.getAsString();
    }
}
