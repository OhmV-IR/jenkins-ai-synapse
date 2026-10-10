package io.ohmvir.plugins.jenkinsaisynapse.api.message;

import java.util.Objects;

/**
 * The result of a tool call, answering the {@link ToolCallPart} with the same id.
 */
public final class ToolResultPart implements ContentPart {
    private final String toolCallId;
    private final String toolName;
    private final String content;
    private final boolean error;

    /**
     * @param toolCallId The id of the {@link ToolCallPart} this answers.
     * @param toolName The name of the tool that was called.
     * @param content The tool's output, or a description of the error.
     * @param error Whether the call failed.
     */
    public ToolResultPart(String toolCallId, String toolName, String content, boolean error) {
        this.toolCallId = Objects.requireNonNull(toolCallId, "toolCallId");
        this.toolName = Objects.requireNonNull(toolName, "toolName");
        this.content = Objects.requireNonNull(content, "content");
        this.error = error;
    }

    public static ToolResultPart success(ToolCallPart call, String content) {
        return new ToolResultPart(call.getId(), call.getName(), content, false);
    }

    public static ToolResultPart error(ToolCallPart call, String message) {
        return new ToolResultPart(call.getId(), call.getName(), message, true);
    }

    public String getToolCallId() {
        return toolCallId;
    }

    public String getToolName() {
        return toolName;
    }

    public String getContent() {
        return content;
    }

    public boolean isError() {
        return error;
    }

    @Override
    public String toString() {
        return "ToolResultPart[" + toolCallId + ", " + toolName + ", " + (error ? "error: " : "") + content + "]";
    }
}
