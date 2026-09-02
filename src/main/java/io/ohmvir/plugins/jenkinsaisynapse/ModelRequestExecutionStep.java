package io.ohmvir.plugins.jenkinsaisynapse;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import io.ohmvir.plugins.jenkinsaisynapse.api.client.ModelClient;
import io.ohmvir.plugins.jenkinsaisynapse.api.content.*;
import io.ohmvir.plugins.jenkinsaisynapse.api.input.ModelRequest;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelData;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.ModelResponse;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.io.PrintStream;

public class ModelRequestExecutionStep extends Builder implements SimpleBuildStep {
    private final ModelRequest request;

    @DataBoundConstructor
    public ModelRequestExecutionStep(String requestText, String systemPrompt, double temperature, @Nullable Long maxOutputTokens) {
        this.request = new ModelRequest();
        this.request.AddInput(new TextContent(requestText));
        this.request.AddInput(new SystemPromptContent(systemPrompt));
        if(maxOutputTokens != null) {
            this.request.AddInput(new MaxOutputTokensContent(maxOutputTokens));
        }
        this.request.AddInput(new TemperatureContent(temperature));
    }

    @Override
    public void perform(@NonNull Run<?, ?> run, @NonNull EnvVars env, @NonNull TaskListener listener) throws InterruptedException, IOException {
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
                        case TextContent text -> logger.println("Model response: " + text.getText());
                        case ThinkingContent thinking -> logger.println("Model thinking: " + thinking.getThinking());
                        default -> logger.println("Unknown output type: " + output.getClass().getName());
                    }
                }
        );
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
