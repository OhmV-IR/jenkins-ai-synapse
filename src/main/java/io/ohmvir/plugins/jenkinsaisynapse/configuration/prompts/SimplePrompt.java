package io.ohmvir.plugins.jenkinsaisynapse.configuration.prompts;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import io.ohmvir.plugins.jenkinsaisynapse.api.request.ModelRequest;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

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

    /**
     * @return A builder holding this prompt, for subclasses to add their settings to.
     */
    protected ModelRequest.Builder createRequestBuilder() {
        return ModelRequest.builder().user(promptText);
    }

    @Override
    public ModelRequest createRequest() {
        return createRequestBuilder().build();
    }

    @Extension
    public static class DescriptorImpl extends PromptConfiguration.DescriptorImpl {
        @Override
        public @NonNull String getDisplayName() {
            return "Simple Prompt";
        }

        public FormValidation doCheckPromptText(@QueryParameter String value) {
            if (value.trim().isEmpty()) {
                return FormValidation.error("Please enter a prompt text");
            }
            return FormValidation.ok();
        }
    }
}
