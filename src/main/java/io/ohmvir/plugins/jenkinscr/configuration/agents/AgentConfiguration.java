package io.ohmvir.plugins.jenkinscr.configuration.agents;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import io.ohmvir.plugins.jenkinscr.api.models.ModelData;
import io.ohmvir.plugins.jenkinscr.api.models.ModelThinkingLevel;
import io.ohmvir.plugins.jenkinscr.configuration.AgenticCodeReviewSettings;
import io.ohmvir.plugins.jenkinscr.configuration.models.ModelConfiguration;
import org.jspecify.annotations.NonNull;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

public class AgentConfiguration extends AbstractAgentConfiguration {

    @DataBoundConstructor
    public AgentConfiguration(String systemPrompt, double temperature, long maxOutputTokensPerPrompt, String modelId, ModelThinkingLevel thinkingLevel) throws Descriptor.FormException {
        super(systemPrompt, temperature, maxOutputTokensPerPrompt, modelId, thinkingLevel);
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<AbstractAgentConfiguration> {

        @Override
        public @NonNull String getDisplayName() {
            return "Agent Configuration";
        }

        public ListBoxModel doFillModelIdItems() {
            ListBoxModel items = new ListBoxModel();
            for (ModelConfiguration modelConfiguration : AgenticCodeReviewSettings.get().getModels()) {
                items.add(modelConfiguration.getModelId());
            }
            return items;
        }

        public ListBoxModel doFillThinkingLevelItems(@QueryParameter String modelId) {
            ListBoxModel items = new ListBoxModel();
            ModelData modelData = ModelData.get(modelId);
            if (modelData == null) {
                return items;
            }
            modelData.getSupportedThinkingLevels().forEach(
                    thinkingLevel -> items.add(thinkingLevel.toString(), thinkingLevel.toString())
            );
            return items;
        }

        @POST
        public FormValidation doCheckTemperature(@QueryParameter Double value) {
            if (value == null) {
                return FormValidation.error("Temperature is required");
            }
            if (value < 0 || value > 1) {
                return FormValidation.error("Temperature must be between 0 and 1");
            }
            return FormValidation.ok();
        }

        @POST
        public FormValidation doCheckMaxOutputTokensPerPrompt(@QueryParameter Long value) {
            if (value == null) {
                return FormValidation.error("Max output tokens per prompt is required");
            }
            if (value < 0) {
                return FormValidation.error("Max output tokens per prompt must be positive");
            }
            return FormValidation.ok();
        }

        @POST
        public FormValidation doCheckProvider(@QueryParameter String modelName) {
            if (modelName == null || modelName.trim().isEmpty()) {
                return FormValidation.error("Provider is required");
            }
            return FormValidation.ok();
        }
    }
}