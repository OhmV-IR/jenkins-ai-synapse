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
import io.ohmvir.plugins.jenkinsaisynapse.api.input.InputAudioContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.input.InputConversationContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.input.InputFileContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.input.InputImageContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.input.InputTextContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.input.InputVideoContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.input.MaxOutputTokensContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.input.ModelInput;
import io.ohmvir.plugins.jenkinsaisynapse.api.input.ModelRequest;
import io.ohmvir.plugins.jenkinsaisynapse.api.input.ModelRequestException;
import io.ohmvir.plugins.jenkinsaisynapse.api.input.StopSequencesContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.input.SystemPromptContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.input.TemperatureContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.input.ThinkingLevelContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.input.ToolCallResponseContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.input.TopKContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.input.TopPContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelData;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelFinishReason;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.CitationContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.ModelOutput;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.ModelResponse;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.OutputAudioContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.OutputFileContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.OutputImageContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.OutputTextContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.OutputVideoContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.ThinkingContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.TokenUtilizationContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.ToolCallContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.skills.SkillData;
import io.ohmvir.plugins.jenkinsaisynapse.api.tools.Tool;
import io.ohmvir.plugins.jenkinsaisynapse.api.tools.ToolRegistry;
import io.ohmvir.plugins.jenkinsaisynapse.configuration.BasePromptsManagementLink;
import io.ohmvir.plugins.jenkinsaisynapse.configuration.ModelsManagementLink;
import io.ohmvir.plugins.jenkinsaisynapse.configuration.models.ModelConfiguration;
import io.ohmvir.plugins.jenkinsaisynapse.configuration.prompts.PromptConfiguration;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
import javax.imageio.ImageIO;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import org.jspecify.annotations.Nullable;

/**
 * Builds a fresh {@link ModelRequest} for every execution of {@link AiRequestStep}, so nothing is shared between
 * builds, and executes it on a background thread so the build can be aborted while the model is working.
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
    static final Map<String, Class<? extends ModelOutput>> OUTPUT_TYPES = createOutputTypes();

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

    private static Map<String, Class<? extends ModelOutput>> createOutputTypes() {
        Map<String, Class<? extends ModelOutput>> outputTypes = new LinkedHashMap<>();
        outputTypes.put("text", OutputTextContent.class);
        outputTypes.put("thinking", ThinkingContent.class);
        outputTypes.put("image", OutputImageContent.class);
        outputTypes.put("audio", OutputAudioContent.class);
        outputTypes.put("video", OutputVideoContent.class);
        outputTypes.put("file", OutputFileContent.class);
        outputTypes.put("citations", CitationContent.class);
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

        ModelRequest request = new ModelRequest();
        UUID conversationUuid = attachConversation(request, run, env);
        addPromptInputs(request, env);
        addSettings(request);
        attachToolsAndSkills(request, env);
        for (String path : expand(env, step.getFiles())) {
            request.addInput(readAttachment(requireWorkspace(context, "files"), path));
        }
        String prompt = step.getPrompt();
        if (prompt != null) {
            request.addInput(new InputTextContent(step.isExpandEnvVars() ? env.expand(prompt) : prompt));
        }
        if (step.getOutputTypes() != null) {
            step.getOutputTypes()
                    .forEach(type -> request.requestOutputType(OUTPUT_TYPES.get(type.toLowerCase(Locale.ROOT))));
        }
        ModelData model = resolveModel(request, env.expand(step.getModel()));

        BuildLogRequestListener requestListener = new BuildLogRequestListener(logger, step.isQuiet(), step.isVerbose());
        request.setListener(requestListener);
        if (!step.isQuiet()) {
            logger.println("[aiRequest] Sending request to " + model.getModelId());
        }
        ModelResponse response;
        try {
            response = request.executeOrThrow(
                    model,
                    step.getTimeout() == null ? null : Duration.ofSeconds(step.getTimeout()),
                    step.getEffectiveMaxTurns());
        } catch (ModelRequestException e) {
            if (e.getCause() == null) {
                throw new AbortException(e.getMessage());
            }
            throw e;
        } finally {
            requestListener.finish();
        }

        String outputDir = env.expand(step.getOutputDir());
        List<Map<String, Object>> outputFiles =
                saveOutputFiles(response, outputDir == null ? null : requireWorkspace(context, "outputDir"), outputDir);
        if (!step.isQuiet()) {
            logger.println("[aiRequest] " + describeCompletion(model, response));
            if (outputDir == null && !outputFiles.isEmpty()) {
                logger.println("[aiRequest] The model produced " + outputFiles.size()
                        + " files, set outputDir to save them to the workspace");
            }
        }
        if (!step.isReturnResponse()) {
            return response.getText();
        }
        return toMap(request, response, model, outputFiles, conversationUuid);
    }

    private @Nullable UUID attachConversation(ModelRequest request, Run<?, ?> run, EnvVars env) throws IOException {
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
        ModelConversation conversation = new ModelConversation(uuid);
        request.addInput(new InputConversationContent(conversation));
        request.setAssociatedConversation(conversation);
        return uuid;
    }

    private void addPromptInputs(ModelRequest request, EnvVars env) throws AbortException {
        String promptId = env.expand(step.getPromptId());
        if (promptId == null) {
            return;
        }
        PromptConfiguration promptConfiguration =
                BasePromptsManagementLink.get().getPromptConfiguration(promptId);
        if (promptConfiguration == null) {
            throw new AbortException("Unknown prompt id '" + promptId + "'. Configured prompts: "
                    + describeNames(BasePromptsManagementLink.get().getPrompts().stream()
                            .map(PromptConfiguration::getPromptId)
                            .toList()));
        }
        for (ModelInput input : promptConfiguration.createRequest().getModelInputs()) {
            if (step.isExpandEnvVars() && input instanceof InputTextContent text) {
                request.addInput(new InputTextContent(env.expand(text.getText())));
            } else if (step.isExpandEnvVars() && input instanceof SystemPromptContent systemPrompt) {
                request.addInput(new SystemPromptContent(env.expand(systemPrompt.getSystemPrompt())));
            } else {
                request.addInput(input);
            }
        }
    }

    private void addSettings(ModelRequest request) {
        if (step.getSystemPrompt() != null) {
            request.replaceInput(new SystemPromptContent(step.getSystemPrompt()));
        }
        if (step.getTemperature() != null) {
            request.replaceInput(new TemperatureContent(step.getTemperature()));
        }
        if (step.getMaxOutputTokens() != null) {
            request.replaceInput(new MaxOutputTokensContent(step.getMaxOutputTokens()));
        }
        if (step.getThinkingLevel() != null) {
            request.replaceInput(new ThinkingLevelContent(AiRequestStep.parseThinkingLevel(step.getThinkingLevel())));
        }
        if (step.getTopP() != null) {
            request.replaceInput(new TopPContent(step.getTopP()));
        }
        if (step.getTopK() != null) {
            request.replaceInput(new TopKContent(step.getTopK()));
        }
        if (step.getStopSequences() != null) {
            request.replaceInput(new StopSequencesContent(List.copyOf(step.getStopSequences())));
        }
    }

    private void attachToolsAndSkills(ModelRequest request, EnvVars env) throws AbortException {
        for (String toolName : expand(env, step.getTools())) {
            Tool tool = ToolRegistry.getTool(toolName);
            if (tool == null) {
                throw new AbortException("Unknown tool '" + toolName + "'. Available tools: "
                        + describeNames(ToolRegistry.getAllTools().stream()
                                .map(Tool::getName)
                                .toList()));
            }
            request.attachTool(tool);
        }
        for (String skillName : expand(env, step.getSkills())) {
            SkillData skill = SkillData.getSkillData(skillName);
            if (skill == null) {
                throw new AbortException("Unknown skill '" + skillName + "'. Available skills: "
                        + describeNames(SkillData.getAllSkills().stream()
                                .map(SkillData::getSkillName)
                                .toList()));
            }
            request.attachSkill(skill);
        }
    }

    private static ModelData resolveModel(ModelRequest request, @Nullable String modelName) throws AbortException {
        if (modelName == null) {
            ModelData model = ModelData.findModelForRequest(request);
            if (model == null) {
                throw new AbortException(
                        "No configured model supports this request. Configured models: " + describeModels(request));
            }
            return model;
        }
        ModelData model = ModelData.get(modelName);
        if (model == null) {
            model = ModelsManagementLink.get().getModelConfigurations().stream()
                    .filter(configuration -> modelName.equals(configuration.getModelIdDisplayName())
                            || modelName.equals(configuration.getModelName())
                            || modelName.equals(configuration.getModelDisplayName()))
                    .map(ModelConfiguration::getModelId)
                    .map(ModelData::get)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
        }
        if (model == null) {
            throw new AbortException(
                    "Unknown model '" + modelName + "'. Configured models: " + describeModels(request));
        }
        List<String> unsupported = model.getUnsupportedRequirements(request);
        if (!unsupported.isEmpty()) {
            throw new AbortException("Model " + model.getModelId() + " does not support "
                    + String.join(", ", unsupported) + ", which this request needs");
        }
        return model;
    }

    private static String describeModels(ModelRequest request) {
        return describeNames(ModelData.getAll().stream()
                .map(model -> {
                    List<String> unsupported = model.getUnsupportedRequirements(request);
                    return unsupported.isEmpty()
                            ? model.getModelId()
                            : model.getModelId() + " (no " + String.join(", ", unsupported) + ")";
                })
                .sorted()
                .toList());
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

    private static ModelInput readAttachment(FilePath workspace, String path) throws IOException, InterruptedException {
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
        if (contentType.startsWith("image/")) {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));
            if (image != null) {
                return new InputImageContent(image);
            }
        } else if (contentType.startsWith("audio/")) {
            return new InputAudioContent(data);
        } else if (contentType.startsWith("video/")) {
            return new InputVideoContent(data);
        }
        return new InputFileContent(file.getName(), data, contentType);
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

    /**
     * Describes the images, audio, video and files in the response, saving them to the output directory if there is
     * one.
     */
    private static List<Map<String, Object>> saveOutputFiles(
            ModelResponse response, @Nullable FilePath workspace, @Nullable String outputDir)
            throws IOException, InterruptedException {
        List<Map<String, Object>> files = new ArrayList<>();
        Set<String> usedNames = new HashSet<>();
        FilePath directory = workspace == null || outputDir == null ? null : workspace.child(outputDir);
        int index = 0;
        for (ModelOutput output : response.getOutputs()) {
            String name;
            String contentType;
            byte[] data;
            switch (output) {
                case OutputFileContent file -> {
                    name = file.getFileId();
                    contentType = file.getContentType();
                    data = file.getFileData();
                }
                case OutputImageContent image -> {
                    name = "image-" + (index + 1) + ".png";
                    contentType = "image/png";
                    data = toPng(image.getImage());
                }
                case OutputAudioContent audio -> {
                    name = "audio-" + (index + 1) + ".mp3";
                    contentType = "audio/mpeg";
                    data = audio.getAudioData();
                }
                case OutputVideoContent video -> {
                    name = "video-" + (index + 1) + ".mp4";
                    contentType = "video/mp4";
                    data = video.getVideoData();
                }
                default -> {
                    continue;
                }
            }
            index++;
            if (data == null) {
                data = new byte[0];
            }
            name = uniqueFileName(name, index, usedNames);
            Map<String, Object> description = new LinkedHashMap<>();
            description.put("name", name);
            description.put("contentType", contentType);
            description.put("size", data.length);
            if (directory != null) {
                directory.mkdirs();
                directory.child(name).copyFrom(new ByteArrayInputStream(data));
                description.put("path", outputDir + "/" + name);
            }
            files.add(description);
        }
        return files;
    }

    private static byte[] toPng(@Nullable BufferedImage image) throws IOException {
        if (image == null) {
            return new byte[0];
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        }
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

    private static String describeCompletion(ModelData model, ModelResponse response) {
        StringBuilder description = new StringBuilder(model.getModelId()).append(" finished");
        List<String> details = new ArrayList<>();
        ModelFinishReason finishReason = response.getFinishReason();
        if (finishReason != null) {
            details.add(finishReason.name().toLowerCase(Locale.ROOT));
        }
        TokenUtilizationContent usage = response.getTokenUtilization();
        if (usage != null) {
            details.add(usage.getInputTokensUsed() + " input tokens");
            details.add(usage.getOutputTokensUsed() + " output tokens");
        }
        if (!details.isEmpty()) {
            description.append(" (").append(String.join(", ", details)).append(")");
        }
        return description.toString();
    }

    /**
     * Converts the response to plain maps, lists and values that Pipeline scripts can use and store.
     */
    private static Map<String, Object> toMap(
            ModelRequest request,
            ModelResponse response,
            ModelData model,
            List<Map<String, Object>> outputFiles,
            @Nullable UUID conversationUuid) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("text", response.getText());
        result.put("allText", response.getAllText());
        result.put("thinking", response.getThinking());
        result.put("model", model.getModelId());
        ModelFinishReason finishReason = response.getFinishReason();
        result.put("finishReason", finishReason == null ? null : finishReason.name());

        TokenUtilizationContent usage = response.getTokenUtilization();
        if (usage == null) {
            result.put("usage", null);
        } else {
            Map<String, Object> usageMap = new LinkedHashMap<>();
            usageMap.put("inputTokens", usage.getInputTokensUsed());
            usageMap.put("outputTokens", usage.getOutputTokensUsed());
            usageMap.put("cachedTokens", usage.getCachedTokensUsed());
            result.put("usage", usageMap);
        }

        Map<String, ToolCallResponseContent> toolResults = new HashMap<>();
        request.getModelInputs().stream()
                .filter(ToolCallResponseContent.class::isInstance)
                .map(ToolCallResponseContent.class::cast)
                .filter(toolResult -> toolResult.getToolUseId() != null)
                .forEach(toolResult -> toolResults.put(toolResult.getToolUseId(), toolResult));
        List<Map<String, Object>> toolCalls = new ArrayList<>();
        for (ToolCallContent toolCall : response.getOutputs(ToolCallContent.class)) {
            Map<String, Object> toolCallMap = new LinkedHashMap<>();
            toolCallMap.put("id", toolCall.getToolUseId());
            toolCallMap.put("name", toolCall.getName());
            toolCallMap.put("arguments", toJava(toolCall.getToolArguments()));
            ToolCallResponseContent toolResult = toolResults.get(toolCall.getToolUseId());
            toolCallMap.put("successful", toolResult != null && toolResult.isSuccessful());
            toolCallMap.put("result", toolResult == null ? null : toolResult.getResponseContent());
            toolCalls.add(toolCallMap);
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
        result.put("files", outputFiles);
        result.put("conversationId", conversationUuid == null ? null : conversationUuid.toString());
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
