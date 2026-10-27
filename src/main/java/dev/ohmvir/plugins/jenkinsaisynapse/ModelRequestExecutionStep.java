package dev.ohmvir.plugins.jenkinsaisynapse;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ListBoxModel;
import dev.ohmvir.plugins.jenkinsaisynapse.steps.AiRequestRunner;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import jenkins.tasks.SimpleBuildStep;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * Freestyle build step that sends a request to a model and writes the answer to the build log. Every field supports
 * build variables such as {@code ${GIT_COMMIT}}. In Pipeline, use the {@code aiRequest} step instead, which returns
 * the answer.
 */
@Getter
public class ModelRequestExecutionStep extends Builder implements SimpleBuildStep {
    private @Nullable String prompt;
    private @Nullable String promptId;
    private @Nullable String model;
    private @Nullable String systemPrompt;
    /** Kept as text so that it can contain build variables. */
    private @Nullable String temperature;
    /** Kept as text so that it can contain build variables. */
    private @Nullable String maxOutputTokens;
    /** Tool names, separated by commas or whitespace. */
    private @Nullable String tools;
    /** Skill names, separated by commas or whitespace. */
    private @Nullable String skills;

    private boolean quiet;

    @DataBoundConstructor
    public ModelRequestExecutionStep() {}

    @DataBoundSetter
    public void setPrompt(@Nullable String prompt) {
        this.prompt = Util.fixEmpty(prompt);
    }

    @DataBoundSetter
    public void setPromptId(@Nullable String promptId) {
        this.promptId = Util.fixEmptyAndTrim(promptId);
    }

    @DataBoundSetter
    public void setModel(@Nullable String model) {
        this.model = Util.fixEmptyAndTrim(model);
    }

    @DataBoundSetter
    public void setSystemPrompt(@Nullable String systemPrompt) {
        this.systemPrompt = Util.fixEmpty(systemPrompt);
    }

    @DataBoundSetter
    public void setTemperature(@Nullable String temperature) {
        this.temperature = Util.fixEmptyAndTrim(temperature);
    }

    @DataBoundSetter
    public void setMaxOutputTokens(@Nullable String maxOutputTokens) {
        this.maxOutputTokens = Util.fixEmptyAndTrim(maxOutputTokens);
    }

    @DataBoundSetter
    public void setTools(@Nullable String tools) {
        this.tools = Util.fixEmptyAndTrim(tools);
    }

    @DataBoundSetter
    public void setSkills(@Nullable String skills) {
        this.skills = Util.fixEmptyAndTrim(skills);
    }

    @DataBoundSetter
    public void setQuiet(boolean quiet) {
        this.quiet = quiet;
    }

    @Override
    public boolean requiresWorkspace() {
        return false;
    }

    @Override
    public void perform(@NonNull Run<?, ?> run, @NonNull EnvVars env, @NonNull TaskListener listener)
            throws InterruptedException, IOException {
        AiRequestRunner runner = new AiRequestRunner();
        runner.setPrompt(expand(env, prompt));
        runner.setPromptId(Util.fixEmptyAndTrim(expand(env, promptId)));
        runner.setModel(Util.fixEmptyAndTrim(expand(env, model)));
        runner.setSystemPrompt(expand(env, systemPrompt));
        runner.setTemperature(parseDouble("temperature", Util.fixEmptyAndTrim(expand(env, temperature))));
        runner.setMaxOutputTokens(parseLong("maxOutputTokens", Util.fixEmptyAndTrim(expand(env, maxOutputTokens))));
        runner.setTools(splitNames(expand(env, tools)));
        runner.setSkills(splitNames(expand(env, skills)));
        runner.setQuiet(quiet);
        AiRequestRunner.Result result = runner.execute(run, listener);
        if (quiet) {
            listener.getLogger().println(result.getText());
        }
    }

    private static @Nullable String expand(EnvVars env, @Nullable String value) {
        return value == null ? null : Util.fixEmpty(env.expand(value));
    }

    private static @Nullable Double parseDouble(String field, @Nullable String value) throws AbortException {
        try {
            return value == null ? null : Double.valueOf(value);
        } catch (NumberFormatException e) {
            throw new AbortException(field + " must be a number, got '" + value + "'");
        }
    }

    private static @Nullable Long parseLong(String field, @Nullable String value) throws AbortException {
        try {
            return value == null ? null : Long.valueOf(value);
        } catch (NumberFormatException e) {
            throw new AbortException(field + " must be a whole number, got '" + value + "'");
        }
    }

    static List<String> splitNames(@Nullable String names) {
        if (names == null) {
            return List.of();
        }
        return Arrays.stream(names.split("[,\\s]+"))
                .filter(name -> !name.isEmpty())
                .toList();
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Builder> {
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public @NonNull String getDisplayName() {
            return "Send a request to an AI model";
        }

        public ListBoxModel doFillModelItems(@AncestorInPath @Nullable Item item) {
            return AiRequestRunner.fillModelItems(item);
        }

        public ListBoxModel doFillPromptIdItems(@AncestorInPath @Nullable Item item) {
            return AiRequestRunner.fillPromptIdItems(item);
        }
    }
}
