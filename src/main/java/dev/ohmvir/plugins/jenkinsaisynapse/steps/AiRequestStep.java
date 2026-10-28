package dev.ohmvir.plugins.jenkinsaisynapse.steps;

import dev.ohmvir.plugins.jenkinsaisynapse.api.input.ModelInput;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelThinkingLevel;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.ModelOutputKind;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.OutputAudioContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.OutputFileContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.OutputImageContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.OutputTextContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.OutputVideoContent;
import dev.ohmvir.plugins.jenkinsaisynapse.utils.SecretsUtils;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Util;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.ListBoxModel;
import java.io.IOException;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest2;

/**
 * Pipeline step that sends a request to a model and returns its answer.
 *
 * <pre>
 * def answer = aiRequest 'Summarize the changes in this build'
 * def details = aiRequest(promptId: 'review', prompt: diff, model: 'anthropic:claude-opus-5-5',
 *                         skills: ['code-review'], maxSteps: 10, returnDetails: true)
 * echo details.text
 *
 * node {
 *     def review = aiRequest(prompt: 'Describe this screenshot', files: ['screenshot.png'],
 *                            outputTypes: ['text', 'image'], outputDir: 'ai-output', returnDetails: true)
 * }
 * </pre>
 */
@Getter
public class AiRequestStep extends Step {
    private final @Nullable String prompt;
    private @Nullable String promptId;
    private @Nullable String model;
    private @Nullable String systemPrompt;
    private @Nullable Double temperature;
    private @Nullable Long maxOutputTokens;
    private List<String> tools = new ArrayList<>();
    private List<String> skills = new ArrayList<>();
    private @Nullable String thinkingLevel;
    private @Nullable Double topP;
    private @Nullable Integer topK;
    private List<String> stopSequences = new ArrayList<>();
    private List<String> files = new ArrayList<>();
    private List<String> outputTypes = new ArrayList<>();
    private @Nullable String outputDir;
    private @Nullable Integer timeout;
    private @Nullable Integer maxSteps;
    private boolean returnDetails;
    private boolean quiet;
    private boolean expandEnvVars;

    /** The values accepted by {@link #setOutputTypes(List)}, mapped to the outputs they require. */
    static final Map<String, ModelOutputKind<?>> OUTPUT_TYPES = createOutputTypes();

    private static Map<String, ModelOutputKind<?>> createOutputTypes() {
        Map<String, ModelOutputKind<?>> outputTypes = new LinkedHashMap<>();
        outputTypes.put("text", OutputTextContent.KIND);
        outputTypes.put("image", OutputImageContent.KIND);
        outputTypes.put("audio", OutputAudioContent.KIND);
        outputTypes.put("video", OutputVideoContent.KIND);
        outputTypes.put("file", OutputFileContent.KIND);
        return Collections.unmodifiableMap(outputTypes);
    }

    /**
     * @param prompt The text to send to the model. May be left out when {@link #setPromptId(String)} is set.
     */
    @DataBoundConstructor
    public AiRequestStep(@Nullable String prompt) {
        this.prompt = Util.fixEmpty(prompt);
    }

    /** The id of a base prompt to start from. {@code prompt}, if also set, is added to it. */
    @DataBoundSetter
    public void setPromptId(@Nullable String promptId) {
        this.promptId = Util.fixEmptyAndTrim(promptId);
    }

    /** The id of the model to use, for example {@code anthropic:claude-opus-5-5}. By default any capable model is used. */
    @DataBoundSetter
    public void setModel(@Nullable String model) {
        this.model = Util.fixEmptyAndTrim(model);
    }

    /** Replaces the system prompt of the base prompt, if any. */
    @DataBoundSetter
    public void setSystemPrompt(@Nullable String systemPrompt) {
        this.systemPrompt = Util.fixEmpty(systemPrompt);
    }

    @DataBoundSetter
    public void setTemperature(@Nullable Double temperature) {
        // The upper bound differs between providers, so only values no provider accepts are rejected here.
        if (temperature != null && (temperature.isNaN() || temperature.isInfinite() || temperature < 0)) {
            throw new IllegalArgumentException("temperature must be a finite number that is not negative");
        }
        this.temperature = temperature;
    }

    @DataBoundSetter
    public void setMaxOutputTokens(@Nullable Long maxOutputTokens) {
        this.maxOutputTokens = maxOutputTokens;
    }

    /** Names of the tools the model may call. */
    @DataBoundSetter
    public void setTools(@Nullable List<String> tools) {
        this.tools = tools == null ? new ArrayList<>() : new ArrayList<>(tools);
    }

    /** Names of the skills to give the model. */
    @DataBoundSetter
    public void setSkills(@Nullable List<String> skills) {
        this.skills = skills == null ? new ArrayList<>() : new ArrayList<>(skills);
    }

    /**
     * How much the model should think, one of {@code off}, {@code on}, {@code low}, {@code medium}, {@code high},
     * {@code extra_high} or {@code max}. The nearest level the model supports is used.
     */
    @DataBoundSetter
    public void setThinkingLevel(@Nullable String thinkingLevel) {
        String level = Util.fixEmptyAndTrim(thinkingLevel);
        if (level != null) {
            parseThinkingLevel(level);
        }
        this.thinkingLevel = level;
    }

    @DataBoundSetter
    public void setTopP(@Nullable Double topP) {
        if (topP != null && !(topP > 0 && topP <= 1)) {
            throw new IllegalArgumentException("topP must be greater than 0 and at most 1");
        }
        this.topP = topP;
    }

    @DataBoundSetter
    public void setTopK(@Nullable Integer topK) {
        if (topK != null && topK < 1) {
            throw new IllegalArgumentException("topK must be at least 1");
        }
        this.topK = topK;
    }

    /** Text that makes the model stop generating when it produces it. */
    @DataBoundSetter
    public void setStopSequences(@Nullable List<String> stopSequences) {
        this.stopSequences = stopSequences == null ? new ArrayList<>() : new ArrayList<>(stopSequences);
    }

    /**
     * Workspace files to attach to the request. PNG, JPEG, GIF and WebP images, audio and video are sent as such, text
     * files (including JSON, YAML, XML and SVG) as text headed by the file name, anything else as a file.
     *
     * @throws IllegalArgumentException If an entry is null or blank, for example because it comes from an unset variable
     */
    @DataBoundSetter
    public void setFiles(@Nullable List<String> files) {
        List<String> paths = new ArrayList<>();
        if (files != null) {
            for (int i = 0; i < files.size(); i++) {
                String path = Util.fixEmptyAndTrim(files.get(i));
                if (path == null) {
                    throw new IllegalArgumentException("files[" + i + "] is empty, every file needs a path");
                }
                paths.add(path);
            }
        }
        this.files = paths;
    }

    /**
     * The outputs the response must contain, out of {@code text}, {@code image}, {@code audio}, {@code video} and
     * {@code file}. Only models that can produce all of them are used. Defaults to {@code text}.
     */
    @DataBoundSetter
    public void setOutputTypes(@Nullable List<String> outputTypes) {
        List<String> types = new ArrayList<>();
        if (outputTypes != null) {
            for (String type : outputTypes) {
                String normalized = type == null ? "" : type.trim().toLowerCase(Locale.ROOT);
                if (!OUTPUT_TYPES.containsKey(normalized)) {
                    throw new IllegalArgumentException("Unknown output type '" + type + "'. Use one of: "
                            + String.join(", ", OUTPUT_TYPES.keySet()));
                }
                types.add(normalized);
            }
        }
        this.outputTypes = types;
    }

    /** Workspace directory to save the images, audio, video and files the model produces to. */
    @DataBoundSetter
    public void setOutputDir(@Nullable String outputDir) {
        this.outputDir = Util.fixEmptyAndTrim(outputDir);
    }

    /**
     * The maximum number of seconds the whole request may take. When set, it replaces the timeout configured for the
     * model's provider, even if it is longer. Without it the provider's timeout applies.
     */
    @DataBoundSetter
    public void setTimeout(@Nullable Integer timeout) {
        if (timeout != null && timeout < 0) {
            throw new IllegalArgumentException("timeout must not be negative");
        }
        this.timeout = timeout == null || timeout == 0 ? null : timeout;
    }

    /**
<<<<<<< HEAD
     * The maximum number of times the model may be called, which limits how many rounds of tool calls it can make.
     * The request fails when the model still wants to call tools after that many steps. When not set, the default
     * maximum number of steps from the global configuration (Manage Jenkins &gt; System &gt; AI Requests) applies,
     * which is 25 until it is changed.
     */
    @DataBoundSetter
    public void setMaxSteps(@Nullable Integer maxSteps) {
        if (maxSteps != null && maxSteps <= 0) {
            throw new IllegalArgumentException("maxSteps must be greater than 0");
        }
        this.maxSteps = maxSteps;
    }

    /**
     * Expand references to environment variables such as {@code ${BUILD_NUMBER}} in the prompt, prompt id, model,
     * system prompt, stop sequences, files and output directory. Groovy string interpolation already does this for
     * double-quoted strings.
     */
    @DataBoundSetter
    public void setExpandEnvVars(boolean expandEnvVars) {
        this.expandEnvVars = expandEnvVars;
    }

    /**
     * Return a map with the text, thinking, tool calls, citations, files, finish reason and token usage instead of
     * only the text.
     */
    @DataBoundSetter
    public void setReturnDetails(boolean returnDetails) {
        this.returnDetails = returnDetails;
    }

    /** Do not write the request's progress to the build log. */
    @DataBoundSetter
    public void setQuiet(boolean quiet) {
        this.quiet = quiet;
    }

    public List<String> getTools() {
        return Collections.unmodifiableList(tools);
    }

    public List<String> getSkills() {
        return Collections.unmodifiableList(skills);
    }

    public List<String> getStopSequences() {
        return Collections.unmodifiableList(stopSequences);
    }

    public List<String> getFiles() {
        return Collections.unmodifiableList(files);
    }

    public List<String> getOutputTypes() {
        return Collections.unmodifiableList(outputTypes);
    }

    static ModelThinkingLevel parseThinkingLevel(String level) {
        try {
            return ModelThinkingLevel.valueOf(
                    level.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown thinking level '" + level + "'. Use one of: "
                    + Arrays.stream(ModelThinkingLevel.values())
                            .map(value -> value.name().toLowerCase(Locale.ROOT))
                            .collect(Collectors.joining(", ")));
        }
    }

    /**
     * @param env The build's environment, used when {@link #isExpandEnvVars()} is set
     * @param attachments The workspace files to attach, already read
     */
    AiRequestRunner createRunner(@Nullable EnvVars env, List<ModelInput> attachments) {
        AiRequestRunner runner = new AiRequestRunner();
        runner.setPrompt(expand(env, prompt));
        runner.setPromptId(expandAndTrim(env, promptId));
        runner.setModel(expandAndTrim(env, model));
        runner.setSystemPrompt(expand(env, systemPrompt));
        runner.setTemperature(temperature);
        runner.setMaxOutputTokens(maxOutputTokens);
        runner.setThinkingLevel(thinkingLevel == null ? null : parseThinkingLevel(thinkingLevel));
        runner.setTopP(topP);
        runner.setTopK(topK);
        runner.setStopSequences(expandAll(env, stopSequences));
        runner.setTools(List.copyOf(tools));
        runner.setSkills(List.copyOf(skills));
        runner.setAttachments(List.copyOf(attachments));
        if (!outputTypes.isEmpty()) {
            runner.setOutputs(outputTypes.stream()
                    .<ModelOutputKind<?>>map(OUTPUT_TYPES::get)
                    .toList());
        }
        runner.setTimeoutSeconds(timeout == null ? 0 : timeout);
        runner.setMaxSteps(maxSteps);
        runner.setQuiet(quiet);
        return runner;
    }

    /**
     * @return The value with its environment variables expanded when {@link #isExpandEnvVars()} is set, or
     *         {@code null} when that leaves it empty
     */
    @Nullable
    String expand(@Nullable EnvVars env, @Nullable String value) {
        return expandEnvVars ? EnvVarExpansion.expand(env, value) : value;
    }

    /**
     * @return The values with their environment variables expanded when {@link #isExpandEnvVars()} is set, leaving out
     *         those that are then empty
     */
    List<String> expandAll(@Nullable EnvVars env, List<String> values) {
        if (!expandEnvVars) {
            return List.copyOf(values);
        }
        return values.stream()
                .map(value -> EnvVarExpansion.expand(env, value))
                .filter(Objects::nonNull)
                .toList();
    }

    /** Like {@link #expand(EnvVars, String)}, for values such as paths that must not have surrounding whitespace. */
    @Nullable
    String expandAndTrim(@Nullable EnvVars env, @Nullable String value) {
        return expandEnvVars ? EnvVarExpansion.expandAndTrim(env, value) : value;
    }

    @Override
    public StepExecution start(StepContext context) {
        return new Execution(this, context);
    }

    private static final class Execution extends SynchronousNonBlockingStepExecution<Object> {
        @Serial
        private static final long serialVersionUID = 1L;

        private final transient AiRequestStep step;

        Execution(AiRequestStep step, StepContext context) {
            super(context);
            this.step = step;
        }

        @Override
        protected Object run() throws Exception {
            Run<?, ?> run = getContext().get(Run.class);
            TaskListener listener = getContext().get(TaskListener.class);
            EnvVars env = getContext().get(EnvVars.class);
            List<ModelInput> attachments = new ArrayList<>();
            WorkspaceFiles.AttachmentBudget budget = new WorkspaceFiles.AttachmentBudget();
            for (String path : step.getFiles()) {
                String expandedPath = step.expandAndTrim(env, path);
                if (expandedPath == null) {
                    throw new AbortException("The file path '" + path + "' is empty after expanding variables");
                }
                attachments.add(WorkspaceFiles.readAttachment(requireWorkspace("files"), expandedPath));
            }
            AiRequestRunner runner = step.createRunner(env, attachments);
            runner.setSecrets(SecretsUtils.getBuildSecrets(run, env));
            AiRequestRunner.Result result = runner.execute(run, listener);
            String outputDir = step.expand(env, step.getOutputDir());
            List<Map<String, Object>> files = WorkspaceFiles.saveOutputs(
                    result.getResponse(),
                    outputDir == null ? null : requireWorkspace("outputDir").child(outputDir),
                    outputDir);
            if (!step.isQuiet() && outputDir == null && !files.isEmpty()) {
                listener.getLogger()
                        .println("[AI] The model produced " + files.size()
                                + " files, set outputDir to save them to the workspace");
            }
            if (!step.isReturnDetails()) {
                return result.getText();
            }
            Map<String, Object> details = result.toMap();
            details.put("files", files);
            return details;
        }

        private FilePath requireWorkspace(String parameter) throws IOException, InterruptedException {
            FilePath workspace = getContext().get(FilePath.class);
            if (workspace == null) {
                throw new AbortException(
                        "aiRequest needs a workspace for " + parameter + ", use it inside a node block");
            }
            return workspace;
        }
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {
        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return Set.of(Run.class, TaskListener.class);
        }

        @Override
        public String getFunctionName() {
            return "aiRequest";
        }

        @Override
        public @NonNull String getDisplayName() {
            return "Send a request to an AI model";
        }

        @Override
        public Step newInstance(@Nullable StaplerRequest2 req, @NonNull JSONObject formData)
                throws Descriptor.FormException {
            // The snippet generator submits empty number fields as "", which cannot be bound to a Double or Long.
            for (String field : List.of("temperature", "maxOutputTokens", "topP", "topK", "timeout", "maxSteps")) {
                if (Util.fixEmptyAndTrim(formData.optString(field)) == null) {
                    formData.remove(field);
                }
            }
            return super.newInstance(req, formData);
        }

        public ListBoxModel doFillModelItems(@AncestorInPath @Nullable Item item) {
            return AiRequestRunner.fillModelItems(item);
        }

        public ListBoxModel doFillPromptIdItems(@AncestorInPath @Nullable Item item) {
            return AiRequestRunner.fillPromptIdItems(item);
        }

        public ListBoxModel doFillThinkingLevelItems() {
            ListBoxModel items = new ListBoxModel();
            items.add("(Not set)", "");
            for (ModelThinkingLevel level : ModelThinkingLevel.values()) {
                items.add(level.name().toLowerCase(Locale.ROOT));
            }
            return items;
        }
    }
}
