package io.ohmvir.plugins.jenkinscr.configuration.agents;

import com.google.gson.JsonObject;
import hudson.Extension;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import io.ohmvir.plugins.jenkinscr.configuration.AgenticCodeReviewSettings;
import io.ohmvir.plugins.jenkinscr.configuration.models.ModelConfiguration;
import org.jspecify.annotations.NonNull;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

public class AgentConfiguration extends AbstractAgentConfiguration {

    @DataBoundConstructor
    public AgentConfiguration(String systemPrompt, double temperature, long maxOutputTokensPerPrompt, String modelId, AgentReasoningLevel thinkingLevel) throws Descriptor.FormException {
        super(systemPrompt, temperature, maxOutputTokensPerPrompt, modelId, thinkingLevel);
    }

    public static AgentConfiguration fromDefaultJson(JsonObject json) throws Descriptor.FormException {
        return new AgentConfiguration(json.get("systemPrompt").getAsString(),
                json.get("temperature").getAsDouble(),
                json.get("maxOutputTokensPerPrompt").getAsLong(),
                AgenticCodeReviewSettings.get().getProviders().getFirst().getModelId(),
                AgentReasoningLevel.fromString(json.get("thinkingLevel").getAsString()));
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<AbstractAgentConfiguration> {
        @Override
        public @NonNull String getDisplayName() {
            return "Agent Configuration";
        }

        public ListBoxModel doFillModelNameItems() {
            ListBoxModel items = new ListBoxModel();
            for(ModelConfiguration modelConfiguration : AgenticCodeReviewSettings.get().getProviders()){
                items.add(modelConfiguration.modelName);
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
        public FormValidation doCheckProvider(@QueryParameter String modelName) {
            if(modelName == null || modelName.trim().isEmpty()){
                return FormValidation.error("Provider is required");
            }
            return FormValidation.ok();
        }
    }
}
