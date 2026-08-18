package io.ohmvir.plugins.jenkinscr.configuration.agents;

import hudson.Extension;
import hudson.RelativePath;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import io.ohmvir.plugins.jenkinscr.configuration.AgenticCodeReviewSettings;
import io.ohmvir.plugins.jenkinscr.configuration.models.Model;
import org.jspecify.annotations.NonNull;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

import java.util.List;

public class AgentConfiguration extends AbstractAgentConfiguration {

    @DataBoundConstructor
    public AgentConfiguration(String systemPrompt, double temperature, long maxOutputTokensPerPrompt, String modelName) throws Descriptor.FormException {
        super(systemPrompt, temperature, maxOutputTokensPerPrompt, modelName);
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<AbstractAgentConfiguration> {
        @Override
        public @NonNull String getDisplayName() {
            return "Agent Configuration";
        }

        public ListBoxModel doFillProviderItems(@RelativePath("..") @QueryParameter List<Model> providers){
            ListBoxModel items = new ListBoxModel();
            for(Model model : providers){
                items.add(model.modelName);
            }
            return items;
        }

        @POST
        public FormValidation doCheckTemperature(@QueryParameter Double value) {
            if(value == null){
                return FormValidation.error("Temperature is required");
            }
            if(value < 0 || value > 1){
                return FormValidation.error("Temperature must be between 0 and 1");
            }
            return FormValidation.ok();
        }

        @POST
        public FormValidation doCheckMaxOutputTokensPerPrompt(@QueryParameter Long value) {
            if(value == null){
                return FormValidation.error("Max output tokens per prompt is required");
            }
            if(value < 0){
                return FormValidation.error("Max output tokens per prompt must be positive");
            }
            return FormValidation.ok();
        }

        @POST
        public FormValidation doCheckProvider(@QueryParameter Model provider) {
            if(provider == null){
                return FormValidation.error("Provider is required");
            }
            return FormValidation.ok();
        }
    }
}
