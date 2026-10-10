package io.ohmvir.plugins.jenkinsaisynapse;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ListBoxModel;
import io.ohmvir.plugins.jenkinsaisynapse.api.input.*;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelRegistry;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.RegisteredModel;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.ModelResponse;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.OutputTextContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.ThinkingContent;
import java.io.IOException;
import java.io.PrintStream;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import lombok.Getter;
import org.jenkinsci.Symbol;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.verb.POST;

public class ModelRequestExecutionStep extends Builder implements SimpleBuildStep {
    private final @Getter String requestText;
    private final @Getter String systemPrompt;
    private final @Getter double temperature;
    private final @Getter @Nullable Long maxOutputTokens;
    /** The model to run on, or null to use the first suitable model in the order configured on the Models page */
    private @Getter @Nullable String modelId;

    @DataBoundConstructor
    public ModelRequestExecutionStep(
            String requestText, String systemPrompt, double temperature, @Nullable Long maxOutputTokens) {
        this.requestText = requestText;
        this.systemPrompt = systemPrompt;
        this.temperature = temperature;
        this.maxOutputTokens = maxOutputTokens;
    }

    @DataBoundSetter
    public void setModelId(@Nullable String modelId) {
        this.modelId = modelId == null || modelId.isBlank() ? null : modelId.trim();
    }

    public ModelRequest createRequest() {
        ModelRequest request = new ModelRequest();
        request.setModelId(modelId);
        request.addInput(new InputTextContent(requestText));
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            request.addInput(new SystemPromptContent(systemPrompt));
        }
        if (maxOutputTokens != null) {
            request.addInput(new MaxOutputTokensContent(maxOutputTokens));
        }
        request.addInput(new TemperatureContent(temperature));
        return request;
    }

    @Override
    public void perform(@NonNull Run<?, ?> run, @NonNull EnvVars env, @NonNull TaskListener listener)
            throws InterruptedException, IOException {
        ModelResponse response = createRequest().execute();
        if (response == null) {
            throw new IOException(
                    modelId != null
                            ? "Model " + modelId + " is not configured or failed to respond to the request"
                            : "No configured model could respond to the request");
        }
        PrintStream logger = listener.getLogger();
        response.getOutputs().forEach(output -> {
            switch (output) {
                case OutputTextContent text -> logger.println("Model response: " + text.getText());
                case ThinkingContent thinking -> logger.println("Model thinking: " + thinking.getThinking());
                default ->
                    logger.println("Unknown output type: " + output.getClass().getName());
            }
        });
    }

    @Symbol("customStep")
    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Builder> {
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public @NonNull String getDisplayName() {
            return "Model Request Execution Step";
        }

        @POST
        public ListBoxModel doFillModelIdItems(@AncestorInPath Item item) {
            ListBoxModel items = new ListBoxModel();
            items.add("Automatic (first suitable model)", "");
            boolean allowed =
                    item == null ? Jenkins.get().hasPermission(Jenkins.ADMINISTER) : item.hasPermission(Item.CONFIGURE);
            if (!allowed) {
                return items;
            }
            for (RegisteredModel model : ModelRegistry.get().getModels()) {
                items.add(model.configuration().getModelIdDisplayName(), model.getModelId());
            }
            return items;
        }
    }
}
