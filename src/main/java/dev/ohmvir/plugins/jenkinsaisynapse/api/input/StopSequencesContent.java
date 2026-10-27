package dev.ohmvir.plugins.jenkinsaisynapse.api.input;

import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelInputType;
import java.util.List;
import java.util.Set;
import lombok.Getter;

public class StopSequencesContent extends ModelInput {
    private @Getter final List<String> stopPhrases;

    public StopSequencesContent(List<String> stopPhrases) {
        this.stopPhrases = stopPhrases;
    }

    @Override
    public Set<ModelCapability> getRequiredCapabilities() {
        return Set.of(ModelCapability.CUSTOM_STOP_SEQUENCES);
    }

    @Override
    public Set<ModelInputType> getRequiredInputTypes() {
        return Set.of();
    }
}
