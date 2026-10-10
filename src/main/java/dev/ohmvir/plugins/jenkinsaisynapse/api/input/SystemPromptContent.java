package io.ohmvir.plugins.jenkinsaisynapse.api.input;

import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelInputType;
import java.util.Set;
import lombok.Getter;

public class SystemPromptContent extends ModelInput {
    private @Getter final String systemPrompt;

    public SystemPromptContent(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    @Override
    public Set<ModelCapability> getRequiredCapabilities() {
        return Set.of(ModelCapability.ADJUSTABLE_SYSTEM_PROMPT);
    }

    @Override
    public Set<ModelInputType> getRequiredInputTypes() {
        return Set.of();
    }
}
