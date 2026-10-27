package dev.ohmvir.plugins.jenkinsaisynapse.configuration.prompts;

import dev.ohmvir.plugins.jenkinsaisynapse.api.input.InputTextContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.ModelRequest;
import hudson.Extension;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import lombok.Getter;
import org.jenkinsci.Symbol;
import org.jspecify.annotations.NonNull;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

public class SimplePrompt extends PromptConfiguration {
    private @Getter final String promptText;

    @DataBoundConstructor
    public SimplePrompt(String promptId, String promptText) throws Descriptor.FormException {
        super(promptId);
        this.promptText = promptText;
        if (promptText.trim().isEmpty()) {
            throw new Descriptor.FormException("Prompt text cannot be empty", "promptText");
        }
    }

    @Override
    public ModelRequest createRequest() {
        ModelRequest request = new ModelRequest();
        request.addInput(new InputTextContent(promptText));
        return request;
    }

    @Extension
    @Symbol("simplePrompt")
    public static class DescriptorImpl extends PromptConfiguration.DescriptorImpl {
        @Override
        public @NonNull String getDisplayName() {
            return "Simple Prompt";
        }

        @POST
        public FormValidation doCheckPromptText(@QueryParameter String value) {
            Jenkins.get().checkPermission(Jenkins.ADMINISTER);
            if (value == null || value.trim().isEmpty()) {
                return FormValidation.error("Please enter a prompt text");
            }
            return FormValidation.ok();
        }
    }
}
