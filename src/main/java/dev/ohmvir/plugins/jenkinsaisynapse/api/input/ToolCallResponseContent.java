package dev.ohmvir.plugins.jenkinsaisynapse.api.input;

import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelInputType;
import dev.ohmvir.plugins.jenkinsaisynapse.api.tools.Tool;
import hudson.Extension;
import java.util.Set;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * The response to a model's tool call. When the call failed, {@link #isSuccessful()} is false and the response content
 * is the error message for the model.
 */
public class ToolCallResponseContent extends ModelInput {
    private @Getter final String toolUseId;
    private @Getter final boolean successful;
    private @Getter final String responseContent;
    private @Getter final @Nullable Tool calledTool;

    @DataBoundConstructor
    public ToolCallResponseContent(
            String toolUseId, boolean successful, @Nullable String responseContent, @Nullable Tool calledTool) {
        this.toolUseId = toolUseId;
        this.successful = successful;
        this.responseContent = responseContent == null ? "" : responseContent;
        this.calledTool = calledTool;
    }

    /**
     * @param toolUseId The id of the tool call this responds to
     * @param calledTool The tool that was called
     * @param content The tool output
     */
    public static ToolCallResponseContent success(String toolUseId, @Nullable Tool calledTool, String content) {
        return new ToolCallResponseContent(toolUseId, true, content, calledTool);
    }

    /**
     * @param toolUseId The id of the tool call this responds to
     * @param calledTool The tool that was called, or null if the model called a tool that is not available
     * @param message Why the call failed, reported back to the model
     */
    public static ToolCallResponseContent error(String toolUseId, @Nullable Tool calledTool, String message) {
        return new ToolCallResponseContent(toolUseId, false, message, calledTool);
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
