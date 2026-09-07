package io.ohmvir.plugins.jenkinsaisynapse;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import io.ohmvir.plugins.jenkinsaisynapse.api.client.ModelClient;
import io.ohmvir.plugins.jenkinsaisynapse.api.input.*;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelData;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.ModelResponse;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.OutputTextContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.ThinkingContent;
import java.io.IOException;
import java.io.PrintStream;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.kohsuke.stapler.DataBoundConstructor;

public class ModelRequestExecutionStep extends Builder implements SimpleBuildStep {
    private final ModelRequest request;

    @DataBoundConstructor
    public ModelRequestExecutionStep(
            String requestText, String systemPrompt, double temperature, @Nullable Long maxOutputTokens) {
        this.request = new ModelRequest();
        this.request.addInput(new InputTextContent(requestText));
        this.request.addInput(new SystemPromptContent(systemPrompt));
        if (maxOutputTokens != null) {
            this.request.addInput(new MaxOutputTokensContent(maxOutputTokens));
        }
        this.request.addInput(new TemperatureContent(temperature));
    }

    @Override
    public void perform(@NonNull Run<?, ?> run, @NonNull EnvVars env, @NonNull TaskListener listener)
            throws InterruptedException, IOException {
        ModelClient<?, ?> client = ModelData.createClientForRequest(request);
        if (client == null) {
            throw new IOException("Failed to get a client that could respond to the request");
        }
        ModelResponse response = client.generateResponse(request);
        if (response == null) {
            throw new IOException("Failed to get a response that could respond to the request");
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
    }
}
