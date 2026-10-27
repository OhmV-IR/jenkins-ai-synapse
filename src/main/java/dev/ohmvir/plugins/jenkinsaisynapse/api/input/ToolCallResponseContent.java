package dev.ohmvir.plugins.jenkinsaisynapse.api.input;

import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelInputType;
import dev.ohmvir.plugins.jenkinsaisynapse.api.tools.Tool;
import java.util.Set;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

public class ToolCallResponseContent extends ModelInput {
    private @Getter final String toolUseId;
    private @Getter final boolean successful;
    private @Getter final @Nullable String responseContent;
    private @Getter final @Nullable Tool calledTool;

    public ToolCallResponseContent(
            String toolUseId, boolean successful, @Nullable String responseContent, @Nullable Tool calledTool) {
        this.toolUseId = toolUseId;
        this.successful = successful;
        this.responseContent = responseContent;
        this.calledTool = calledTool;
    }

    public static ToolCallResponseContent success(String toolUseId, Tool tool, String response) {
        return new ToolCallResponseContent(toolUseId, true, response, tool);
    }

    public static ToolCallResponseContent error(String toolUseId, @Nullable Tool tool, String message) {
        return new ToolCallResponseContent(toolUseId, false, message, tool);
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
