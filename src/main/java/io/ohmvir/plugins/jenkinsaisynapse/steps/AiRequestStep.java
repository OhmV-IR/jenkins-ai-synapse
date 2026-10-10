package io.ohmvir.plugins.jenkinsaisynapse.steps;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Util;
import hudson.model.Item;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import io.ohmvir.plugins.jenkinsaisynapse.api.input.ModelRequest;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelData;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelThinkingLevel;
import io.ohmvir.plugins.jenkinsaisynapse.configuration.BasePromptsManagementLink;
import io.ohmvir.plugins.jenkinsaisynapse.configuration.models.ModelConfiguration;
import io.ohmvir.plugins.jenkinsaisynapse.configuration.prompts.PromptConfiguration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import jenkins.model.Jenkins;
import lombok.Getter;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

/**
 * Sends a request to an AI model and returns its answer.
 * <pre>
 * def answer = aiRequest 'Summarize the test failures in this build'
 * def review = aiRequest prompt: 'Review this diff', files: ['changes.diff'], tools: ['readFile'], returnResponse: true
 * </pre>
 */
@Getter
public class AiRequestStep extends Step {
    private final @Nullable String prompt;
    private @Nullable String promptId;
    private @Nullable String model;
    private @Nullable String systemPrompt;
    private @Nullable Double temperature;
    private @Nullable Integer maxOutputTokens;
    private @Nullable String thinkingLevel;
    private @Nullable Double topP;
    private @Nullable Integer topK;
    private @Nullable List<String> stopSequences;
    private @Nullable List<String> tools;
    private @Nullable List<String> skills;
    private @Nullable List<String> files;
    private @Nullable List<String> outputTypes;
    private @Nullable String outputDir;
    private @Nullable String conversationId;
    private @Nullable Integer timeout;
    private @Nullable Integer maxTurns;
    private boolean returnResponse;
    private boolean quiet;
    private boolean verbose;
    private boolean expandEnvVars;

    /**
     * @param prompt The text to send to the model. May be omitted when {@link #setPromptId(String)} is set, otherwise
     *               it is appended to the stored prompt.
     */
    @DataBoundConstructor
    public AiRequestStep(@Nullable String prompt) {
        this.prompt = Util.fixEmpty(prompt);
    }

    /**
     * @param promptId The id of a prompt configured under Manage Jenkins » Base Prompts to start the request from.
     */
    @DataBoundSetter
    public void setPromptId(@Nullable String promptId) {
        this.promptId = Util.fixEmptyAndTrim(promptId);
    }

    /**
     * @param model The id of the model to use, such as {@code provider:model-name}. When omitted, the first configured
     *              model that supports the request is used.
     */
    @DataBoundSetter
    public void setModel(@Nullable String model) {
        this.model = Util.fixEmptyAndTrim(model);
    }

    @DataBoundSetter
    public void setSystemPrompt(@Nullable String systemPrompt) {
        this.systemPrompt = Util.fixEmpty(systemPrompt);
    }

    @DataBoundSetter
    public void setTemperature(@Nullable Double temperature) {
        if (temperature != null && temperature < 0) {
            throw new IllegalArgumentException("temperature must not be negative");
        }
        this.temperature = temperature;
    }

    @DataBoundSetter
    public void setMaxOutputTokens(@Nullable Integer maxOutputTokens) {
        if (maxOutputTokens != null && maxOutputTokens <= 0) {
            throw new IllegalArgumentException("maxOutputTokens must be positive");
        }
        this.maxOutputTokens = maxOutputTokens;
    }

    /**
     * @param thinkingLevel One of {@link ModelThinkingLevel}, case insensitive.
     */
    @DataBoundSetter
    public void setThinkingLevel(@Nullable String thinkingLevel) {
        thinkingLevel = Util.fixEmptyAndTrim(thinkingLevel);
        if (thinkingLevel != null) {
            parseThinkingLevel(thinkingLevel);
        }
        this.thinkingLevel = thinkingLevel;
    }

    @DataBoundSetter
    public void setTopP(@Nullable Double topP) {
        if (topP != null && (topP < 0 || topP > 1)) {
            throw new IllegalArgumentException("topP must be between 0 and 1");
        }
        this.topP = topP;
    }

    @DataBoundSetter
    public void setTopK(@Nullable Integer topK) {
        if (topK != null && topK <= 0) {
            throw new IllegalArgumentException("topK must be positive");
        }
        this.topK = topK;
    }

    @DataBoundSetter
    public void setStopSequences(@Nullable List<String> stopSequences) {
        this.stopSequences = copy(stopSequences);
    }

    /**
     * @param tools The names of the tools the model may call.
     */
    @DataBoundSetter
    public void setTools(@Nullable List<String> tools) {
        this.tools = copy(tools);
    }

    /**
     * @param skills The names of the skills to give the model.
     */
    @DataBoundSetter
    public void setSkills(@Nullable List<String> skills) {
        this.skills = copy(skills);
    }

    /**
     * @param files Paths, relative to the workspace, of files to attach to the request.
     */
    @DataBoundSetter
    public void setFiles(@Nullable List<String> files) {
        this.files = copy(files);
    }

    /**
     * @param outputTypes The kinds of output the response must contain, see {@link AiRequestStepExecution#OUTPUT_TYPES}.
     */
    @DataBoundSetter
    public void setOutputTypes(@Nullable List<String> outputTypes) {
        outputTypes = copy(outputTypes);
        if (outputTypes != null) {
            for (String outputType : outputTypes) {
                if (!AiRequestStepExecution.OUTPUT_TYPES.containsKey(outputType.toLowerCase(Locale.ROOT))) {
                    throw new IllegalArgumentException("Unknown output type '" + outputType + "', expected one of "
                            + String.join(", ", AiRequestStepExecution.OUTPUT_TYPES.keySet()));
                }
            }
        }
        this.outputTypes = outputTypes;
    }

    /**
     * @param outputDir A directory, relative to the workspace, where images, audio, video and files produced by the
     *                  model are saved.
     */
    @DataBoundSetter
    public void setOutputDir(@Nullable String outputDir) {
        this.outputDir = Util.fixEmptyAndTrim(outputDir);
    }

    /**
     * @param conversationId A key identifying a conversation to continue. The request sees the conversation's history,
     *                       and the request and its response are added to it. Any key other than a UUID is scoped to
     *                       the job.
     */
    @DataBoundSetter
    public void setConversationId(@Nullable String conversationId) {
        this.conversationId = Util.fixEmptyAndTrim(conversationId);
    }

    /**
     * @param timeout The maximum number of seconds the whole request may take.
     */
    @DataBoundSetter
    public void setTimeout(@Nullable Integer timeout) {
        if (timeout != null && timeout <= 0) {
            throw new IllegalArgumentException("timeout must be positive");
        }
        this.timeout = timeout;
    }

    /**
     * @param maxTurns The maximum number of times the model is called while it keeps calling tools.
     */
    @DataBoundSetter
    public void setMaxTurns(@Nullable Integer maxTurns) {
        if (maxTurns != null && maxTurns <= 0) {
            throw new IllegalArgumentException("maxTurns must be positive");
        }
        this.maxTurns = maxTurns;
    }

    /**
     * @param returnResponse Return a map describing the whole response instead of only the model's answer.
     */
    @DataBoundSetter
    public void setReturnResponse(boolean returnResponse) {
        this.returnResponse = returnResponse;
    }

    /**
     * @param quiet Do not print the model's answer and tool calls to the build log.
     */
    @DataBoundSetter
    public void setQuiet(boolean quiet) {
        this.quiet = quiet;
    }

    /**
     * @param verbose Also print the model's thinking, tool arguments and tool results to the build log.
     */
    @DataBoundSetter
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    /**
     * @param expandEnvVars Expand {@code ${VAR}} references to environment variables in the prompt texts, including
     *                      those of stored prompts.
     */
    @DataBoundSetter
    public void setExpandEnvVars(boolean expandEnvVars) {
        this.expandEnvVars = expandEnvVars;
    }

    static ModelThinkingLevel parseThinkingLevel(String thinkingLevel) {
        String name =
                thinkingLevel.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        try {
            return ModelThinkingLevel.valueOf(name);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown thinking level '" + thinkingLevel + "', expected one of "
                    + Arrays.stream(ModelThinkingLevel.values())
                            .map(level -> level.name().toLowerCase(Locale.ROOT))
                            .collect(Collectors.joining(", ")));
        }
    }

    private static @Nullable List<String> copy(@Nullable List<String> values) {
        if (values == null) {
            return null;
        }
        List<String> copy = values.stream()
                .map(Util::fixEmptyAndTrim)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(ArrayList::new));
        return copy.isEmpty() ? null : copy;
    }

    int getEffectiveMaxTurns() {
        return maxTurns == null ? ModelRequest.DEFAULT_MAX_TURNS : maxTurns;
    }

    @Override
    public StepExecution start(StepContext context) {
        return new AiRequestStepExecution(this, context);
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {
        @Override
        public String getFunctionName() {
            return "aiRequest";
        }

        @Override
        public @NonNull String getDisplayName() {
            return "Send a request to an AI model";
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return Set.of(Run.class, TaskListener.class, EnvVars.class);
        }

        private static boolean canConfigure(@Nullable Item item) {
            return item == null ? Jenkins.get().hasPermission(Jenkins.ADMINISTER) : item.hasPermission(Item.CONFIGURE);
        }

        public ListBoxModel doFillModelItems(@AncestorInPath @Nullable Item item) {
            ListBoxModel items = new ListBoxModel();
            items.add("(first model supporting the request)", "");
            if (!canConfigure(item)) {
                return items;
            }
            ModelData.getAll().stream()
                    .sorted(Comparator.comparing(ModelData::getModelId))
                    .forEach(modelData -> {
                        ModelConfiguration configuration = modelData.getModelConfiguration();
                        items.add(
                                configuration == null ? modelData.getModelId() : configuration.getModelIdDisplayName(),
                                modelData.getModelId());
                    });
            return items;
        }

        public ListBoxModel doFillPromptIdItems(@AncestorInPath @Nullable Item item) {
            ListBoxModel items = new ListBoxModel();
            items.add("(none)", "");
            if (!canConfigure(item)) {
                return items;
            }
            BasePromptsManagementLink.get().getPrompts().stream()
                    .map(PromptConfiguration::getPromptId)
                    .sorted()
                    .forEach(items::add);
            return items;
        }

        public ListBoxModel doFillThinkingLevelItems() {
            ListBoxModel items = new ListBoxModel();
            items.add("(model default)", "");
            for (ModelThinkingLevel level : ModelThinkingLevel.values()) {
                items.add(level.name().toLowerCase(Locale.ROOT));
            }
            return items;
        }

        public FormValidation doCheckTemperature(@QueryParameter String value) {
            return checkNumber(value, 0, Double.MAX_VALUE);
        }

        public FormValidation doCheckTopP(@QueryParameter String value) {
            return checkNumber(value, 0, 1);
        }

        private static FormValidation checkNumber(String value, double min, double max) {
            if (Util.fixEmptyAndTrim(value) == null) {
                return FormValidation.ok();
            }
            try {
                double number = Double.parseDouble(value.trim());
                if (number < min || number > max) {
                    return max == Double.MAX_VALUE
                            ? FormValidation.error("Must not be less than " + min)
                            : FormValidation.error("Must be between " + min + " and " + max);
                }
                return FormValidation.ok();
            } catch (NumberFormatException e) {
                return FormValidation.error("Not a number");
            }
        }
    }
}
