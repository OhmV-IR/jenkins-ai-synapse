package io.ohmvir.plugins.jenkinsaisynapse.api.input;

import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelInputType;
import io.ohmvir.plugins.jenkinsaisynapse.api.tools.Tool;
import java.util.Set;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

public class ToolCallResponseContent extends ModelInput {
    private @Getter final String toolUseId;
    private @Getter final boolean successful;
    private @Getter final @Nullable String responseContent;
    private @Getter final Tool calledTool;

    public ToolCallResponseContent(
            String toolUseId, boolean successful, @Nullable String responseContent, Tool calledTool) {
        this.toolUseId = toolUseId;
        this.successful = successful;
        this.responseContent = responseContent;
        this.calledTool = calledTool;
    }

    @Override
    public Set<ModelCapability> getRequiredCapabilities() {
        return Set.of(ModelCapability.TOOLS);
    }

    @Override
    public Set<ModelInputType> getRequiredInputTypes() {
        return Set.of(ModelInputType.TEXT);
    }
}
