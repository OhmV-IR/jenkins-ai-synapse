package dev.ohmvir.plugins.jenkinsaisynapse.steps;

import hudson.Extension;
import hudson.Util;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.ListBoxModel;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
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
 *                         tools: ['readFile'], skills: ['code-review'], returnDetails: true)
 * echo details.text
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
    private boolean returnDetails;
    private boolean quiet;

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

    /** Return a map with the text, thinking, tool calls, finish reason and token usage instead of only the text. */
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

    AiRequestRunner createRunner() {
        AiRequestRunner runner = new AiRequestRunner();
        runner.setPrompt(prompt);
        runner.setPromptId(promptId);
        runner.setModel(model);
        runner.setSystemPrompt(systemPrompt);
        runner.setTemperature(temperature);
        runner.setMaxOutputTokens(maxOutputTokens);
        runner.setTools(List.copyOf(tools));
        runner.setSkills(List.copyOf(skills));
        runner.setQuiet(quiet);
        return runner;
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
            AiRequestRunner.Result result = step.createRunner().execute(run, listener);
            return step.isReturnDetails() ? result.toMap() : result.getText();
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
            for (String field : List.of("temperature", "maxOutputTokens")) {
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
    }
}
