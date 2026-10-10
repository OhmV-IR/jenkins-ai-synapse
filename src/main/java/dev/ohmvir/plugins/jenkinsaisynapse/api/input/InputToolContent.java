package io.ohmvir.plugins.jenkinsaisynapse.api.input;

import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelInputType;
import io.ohmvir.plugins.jenkinsaisynapse.api.tools.Tool;
import java.util.Set;
import lombok.Getter;

public class InputToolContent extends ModelInput {
    private @Getter final Tool tool;

    public InputToolContent(Tool tool) {
        this.tool = tool;
    }

    @Override
    public Set<ModelCapability> getRequiredCapabilities() {
        return Set.of(ModelCapability.TOOLS);
    }

    @Override
    public Set<ModelInputType> getRequiredInputTypes() {
        return Set.of();
    }
}
