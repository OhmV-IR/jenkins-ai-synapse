package io.ohmvir.plugins.jenkinscr;

import hudson.EnvVars;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import io.ohmvir.plugins.jenkinscr.api.client.ModelClient;
import io.ohmvir.plugins.jenkinscr.api.client.ModelRequest;
import io.ohmvir.plugins.jenkinscr.api.models.ModelData;
import jenkins.tasks.SimpleBuildStep;
import org.jspecify.annotations.NonNull;
import org.kohsuke.stapler.DataBoundConstructor;
import hudson.tasks.Builder;

import java.io.IOException;

public class ModelRequestExecutionStep extends Builder implements SimpleBuildStep {
    private final ModelRequest request;
    @DataBoundConstructor
    public ModelRequestExecutionStep(String requestText, String systemPrompt, double temperature)
    {
        this.request = new ModelRequest();
    }

    @Override
    public void perform(@NonNull Run<?, ?> run, @NonNull EnvVars env, @NonNull TaskListener listener) throws InterruptedException, IOException {
        ModelClient<?,?> client = ModelData.CreateClientForRequest(request);
        if(client == null){
            throw new IOException("Failed to get a client that could respond to the request");
        }
        client.generateResponse(request);
    }

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
