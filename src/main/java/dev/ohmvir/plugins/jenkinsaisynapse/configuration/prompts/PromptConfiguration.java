package dev.ohmvir.plugins.jenkinsaisynapse.configuration.prompts;

import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.ModelRequest;
import dev.ohmvir.plugins.jenkinsaisynapse.configuration.PromptsConfiguration;
import lombok.Getter;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

public abstract class PromptConfiguration implements Describable<PromptConfiguration>, ExtensionPoint {
    private @Getter String promptId = "";

    public abstract ModelRequest createRequest();

    public PromptConfiguration(String promptId) throws Descriptor.FormException {
        this.promptId = promptId;
        if (promptId.trim().isEmpty()) {
            throw new Descriptor.FormException("Prompt id cannot be empty", "promptId");
        }
    }

    @DataBoundSetter
    public void setPromptId(String promptId) {
        this.promptId = promptId;
    }

    public static class DescriptorImpl extends Descriptor<PromptConfiguration> {
        /**
         * Validates a prompt id field. The check only receives the value of the field being validated, so it cannot
         * tell whether the entry is already saved or being added. A saved prompt with the same id is therefore not an
         * error here, as that would flag every saved prompt as soon as the form loads. A duplicate id is instead
         * rejected when the form is submitted, see {@link PromptsConfiguration#setPrompts}, which reports the id in
         * the form error. This check reports ids that are already shared by more than one saved prompt.
         */
        @POST
        public FormValidation doCheckPromptId(@QueryParameter String value) {
            Jenkins.get().checkPermission(Jenkins.ADMINISTER);
            if (value == null || value.trim().isEmpty()) {
                return FormValidation.error("Prompt id cannot be empty");
            }
            if (PromptsConfiguration.get().getPrompts().stream()
                            .filter(config -> value.equals(config.getPromptId()))
                            .count()
                    > 1) {
                return FormValidation.error("Prompt id is the same as another saved value");
            }
            return FormValidation.ok();
        }
    }
}
