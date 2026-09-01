package io.ohmvir.plugins.jenkinscr.configuration.prompts;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import io.ohmvir.plugins.jenkinscr.api.content.TextContent;
import io.ohmvir.plugins.jenkinscr.api.input.ModelRequest;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

public class SimplePrompt extends PromptConfiguration {
    private @Getter String promptText;
    @DataBoundConstructor
    public SimplePrompt(String promptId, String promptText) throws Descriptor.FormException {
        super(promptId);
        this.promptText = promptText;
        if(promptText.trim().isEmpty()){
            throw new Descriptor.FormException("Prompt text cannot be empty", "promptText");
        }
    }

    @Override
    public ModelRequest CreateRequest() {
        ModelRequest request = new ModelRequest();
        request.AddInput(new TextContent(promptText));
        return request;
    }

    @Extension
    public static class DescriptorImpl extends PromptConfiguration.DescriptorImpl {
        @Override
        public @NonNull String getDisplayName() {
            return "Simple Prompt";
        }

        public FormValidation doCheckPromptText(@QueryParameter String value) {
            if(value.trim().isEmpty()){
                return FormValidation.error("Please enter a prompt text");
            }
            return FormValidation.ok();
        }
    }
}
