package io.ohmvir.plugins.jenkinsaisynapse.api.input;

import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelInputType;
import java.util.Set;
import lombok.Getter;

public class MaxOutputTokensContent extends ModelInput {
    private @Getter final long maxOutputTokens;

    public MaxOutputTokensContent(long maxOutputTokens) {
        this.maxOutputTokens = maxOutputTokens;
    }

    @Override
    public Set<ModelCapability> getRequiredCapabilities() {
        return Set.of(ModelCapability.OUTPUT_TOKEN_LIMITING);
    }

    @Override
    public Set<ModelInputType> getRequiredInputTypes() {
        return Set.of();
    }
}
