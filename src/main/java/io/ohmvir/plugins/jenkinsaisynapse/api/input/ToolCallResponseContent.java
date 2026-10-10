package io.ohmvir.plugins.jenkinsaisynapse.api.input;

import hudson.Extension;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelInputType;
import io.ohmvir.plugins.jenkinsaisynapse.api.tools.Tool;
import io.ohmvir.plugins.jenkinsaisynapse.api.tools.ToolResult;
import java.util.Set;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.kohsuke.stapler.DataBoundConstructor;

public class ToolCallResponseContent extends ModelInput {
    private @Getter final String toolUseId;
    private @Getter final boolean successful;
    private @Getter final @Nullable String responseContent;
    private @Getter final @Nullable Tool calledTool;

    @DataBoundConstructor
    public ToolCallResponseContent(
            String toolUseId, boolean successful, @Nullable String responseContent, @Nullable Tool calledTool) {
        this.toolUseId = toolUseId;
        this.successful = successful;
        this.responseContent = responseContent;
        this.calledTool = calledTool;
    }

    /**
     * @param toolUseId The id of the tool call this responds to
     * @param result The outcome of the tool call. On error, the response content is the error message for the model.
     * @param calledTool The tool that was called, or null if the model called a tool that is not available
     */
    public ToolCallResponseContent(String toolUseId, ToolResult result, @Nullable Tool calledTool) {
        this(toolUseId, !result.isError(), result.content(), calledTool);
    }

    public boolean isError() {
        return !successful;
    }

    @Extension
    public static class DescriptorImpl extends ModelInputDescriptor {
        @Override
        public Set<ModelCapability> getRequiredCapabilities() {
            return Set.of(ModelCapability.TOOLS);
        }

        @Override
        public Set<ModelInputType> getRequiredInputTypes() {
            return Set.of(ModelInputType.TEXT);
        }

        @Override
        public @NonNull String getDisplayName() {
            return "Tool Call Response Content";
        }
    }
}
