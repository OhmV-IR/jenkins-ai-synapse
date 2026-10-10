package io.ohmvir.plugins.jenkinsaisynapse.configuration.prompts;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelThinkingLevel;
import io.ohmvir.plugins.jenkinsaisynapse.api.request.GenerationParams;
import io.ohmvir.plugins.jenkinsaisynapse.api.request.ModelRequest;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

public class FullPrompt extends SimplePrompt {
    private @Getter String systemPrompt;
    private @Getter double temperature;
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
        if (temperature < 0 || temperature > 1) {
            throw new Descriptor.FormException("Temperature cannot be less than 0 or greater than 1", "temperature");
        }
        this.temperature = temperature;
        if (maxOutputTokensCount < 0) {
            throw new Descriptor.FormException("Max output tokens count cannot be negative", "maxOutputTokensCount");
        }
        this.maxOutputTokensCount = maxOutputTokensCount;
        if (thinkingLevel == null) {
            throw new Descriptor.FormException("Thinking level cannot be null", "thinkingLevel");
        }
        this.thinkingLevel = thinkingLevel;
    }

    @Override
    protected ModelRequest.Builder createRequestBuilder() {
        return super.createRequestBuilder()
                .systemPrompt(systemPrompt)
                .params(GenerationParams.builder()
                        .temperature(temperature)
                        // 0 means no limit
                        .maxOutputTokens(maxOutputTokensCount > 0 ? Math.toIntExact(maxOutputTokensCount) : null)
                        .thinkingLevel(thinkingLevel)
                        .build());
    }

    @Extension
    public static class DescriptorImpl extends SimplePrompt.DescriptorImpl {
        @Override
        public @NonNull String getDisplayName() {
            return "Full Prompt";
        }

        public FormValidation doCheckSystemPrompt(@QueryParameter String value) {
            if (value.trim().isEmpty()) {
                return FormValidation.error("Please enter a system prompt.");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckTemperature(@QueryParameter double value) {
            if (value < 0.0f || value > 1.0f) {
                return FormValidation.error("Temperature must be within 0 and 1.");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckMaxOutputTokensCount(@QueryParameter long value) {
            if (value < 0) {
                return FormValidation.error("Please enter a number greater than 0");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckThinkingLevel(@QueryParameter ModelThinkingLevel value) {
            if (value == null) {
                return FormValidation.error("Must select a thinking level");
            }
            return FormValidation.ok();
        }
    }
}
