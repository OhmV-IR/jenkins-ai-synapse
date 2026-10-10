package io.ohmvir.plugins.jenkinsaisynapse.api.input;

import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelInputType;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelThinkingLevel;
import java.util.Set;
import lombok.Getter;

public class ThinkingLevelContent extends ModelInput {
    private @Getter final ModelThinkingLevel thinkingLevel;

    public ThinkingLevelContent(ModelThinkingLevel thinkingLevel) {
        this.thinkingLevel = thinkingLevel;
    }

    @Override
    public Set<ModelCapability> getRequiredCapabilities() {
        // Asking for no thinking doesn't need a model that can think
        return thinkingLevel == ModelThinkingLevel.OFF ? Set.of() : Set.of(ModelCapability.THINKING);
    }

    @Override
    public Set<ModelInputType> getRequiredInputTypes() {
        return Set.of();
    }
}
