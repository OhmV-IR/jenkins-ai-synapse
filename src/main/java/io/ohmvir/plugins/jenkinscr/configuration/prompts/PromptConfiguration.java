package io.ohmvir.plugins.jenkinscr.configuration.prompts;

import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import io.ohmvir.plugins.jenkinscr.api.client.ModelRequest;
import io.ohmvir.plugins.jenkinscr.configuration.BasePromptsManagementLink;
import lombok.Getter;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

public abstract class PromptConfiguration implements Describable<PromptConfiguration>, ExtensionPoint {
    private @Getter String promptId = "";
    public abstract ModelRequest CreateRequest();

    public PromptConfiguration(String promptId) throws Descriptor.FormException{
        this.promptId = promptId;
        if(promptId.trim().isEmpty()){
            throw new Descriptor.FormException("Prompt id cannot be empty", "promptId");
        }
    }

    @DataBoundSetter
    public void setPromptId(String promptId) {
        this.promptId = promptId;
    }

    public static class DescriptorImpl extends Descriptor<PromptConfiguration> {
        public FormValidation doCheckPromptId(@QueryParameter String value) {
            if(value.trim().isEmpty()){
                return FormValidation.error("Prompt id cannot be empty");
            }
            if(BasePromptsManagementLink.get().getPrompts().stream().anyMatch(config -> config.getPromptId().equals(value))){
                return FormValidation.error("Prompt id is the same as another saved value");
            }
            return FormValidation.ok();
        }
    }
}
