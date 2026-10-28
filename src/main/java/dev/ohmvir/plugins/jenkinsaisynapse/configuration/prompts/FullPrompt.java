package dev.ohmvir.plugins.jenkinsaisynapse.configuration.prompts;

import dev.ohmvir.plugins.jenkinsaisynapse.api.input.MaxOutputTokensContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.ModelRequest;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.SystemPromptContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.TemperatureContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.ThinkingLevelContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelRegistry;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelThinkingLevel;
import hudson.Extension;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

public class FullPrompt extends SimplePrompt {
    private @Getter String systemPrompt;
    private @Getter double temperature;
    /** Maximum number of tokens to generate, 0 means not set and leaves the limit to the provider. */
    private @Getter long maxOutputTokensCount;

    private @Getter ModelThinkingLevel thinkingLevel;

    @DataBoundConstructor
    public FullPrompt(
            String promptId,
            String promptText,
            String systemPrompt,
            double temperature,
            long maxOutputTokensCount,
            ModelThinkingLevel thinkingLevel)
            throws Descriptor.FormException {
        super(promptId, promptText);
        if (systemPrompt.trim().isEmpty()) {
            throw new Descriptor.FormException("System prompt cannot be empty", "systemPrompt");
        }
        this.systemPrompt = systemPrompt;
        String temperatureError = validateTemperature(temperature);
        if (temperatureError != null) {
            throw new Descriptor.FormException(temperatureError, "temperature");
        }
        this.temperature = temperature;
        if (maxOutputTokensCount < 0) {
            throw new Descriptor.FormException(
                    "Max output tokens count cannot be negative, use 0 for not set", "maxOutputTokensCount");
        }
        this.maxOutputTokensCount = maxOutputTokensCount;
        if (thinkingLevel == null) {
            throw new Descriptor.FormException("Thinking level cannot be null", "thinkingLevel");
        }
        this.thinkingLevel = thinkingLevel;
    }

    /**
     * Validates the temperature against the highest max temperature reported by the configured models.
     *
     * @return null if the temperature is valid, otherwise an error message.
     */
    private static @Nullable String validateTemperature(double temperature) {
        if (temperature < 0) {
            return "Temperature cannot be less than 0";
        }
        Double maxTemperature = ModelRegistry.get().getHighestMaxTemperature();
        if (maxTemperature != null && temperature > maxTemperature) {
            return "Temperature cannot be greater than " + maxTemperature
                    + ", the highest max temperature supported by the configured models";
        }
        return null;
    }

    @Override
    public ModelRequest createRequest() {
        ModelRequest req = super.createRequest();
        req.addInput(new SystemPromptContent(systemPrompt));
        req.addInput(new TemperatureContent(temperature));
        if (maxOutputTokensCount > 0) {
            req.addInput(new MaxOutputTokensContent(maxOutputTokensCount));
        }
        req.addInput(new ThinkingLevelContent(thinkingLevel));
        return req;
    }

    @Extension
    public static class DescriptorImpl extends SimplePrompt.DescriptorImpl {
        @Override
        public @NonNull String getDisplayName() {
            return "Full Prompt";
        }

        @POST
        public FormValidation doCheckSystemPrompt(@QueryParameter String value) {
            Jenkins.get().checkPermission(Jenkins.ADMINISTER);
            if (value == null || value.trim().isEmpty()) {
                return FormValidation.error("Please enter a system prompt.");
            }
            return FormValidation.ok();
        }

        @POST
        public FormValidation doCheckTemperature(@QueryParameter double value) {
            Jenkins.get().checkPermission(Jenkins.ADMINISTER);
            String error = validateTemperature(value);
            if (error != null) {
                return FormValidation.error(error);
            }
            return FormValidation.ok();
        }

        @POST
        public FormValidation doCheckMaxOutputTokensCount(@QueryParameter long value) {
            Jenkins.get().checkPermission(Jenkins.ADMINISTER);
            if (value < 0) {
                return FormValidation.error(
                        "Max output tokens cannot be negative, enter a positive number or 0 for not set");
            }
            return FormValidation.ok();
        }

        @POST
        public FormValidation doCheckThinkingLevel(@QueryParameter ModelThinkingLevel value) {
            Jenkins.get().checkPermission(Jenkins.ADMINISTER);
            if (value == null) {
                return FormValidation.error("Must select a thinking level");
            }
            return FormValidation.ok();
        }
    }
}
