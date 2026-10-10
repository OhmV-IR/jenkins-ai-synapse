package io.ohmvir.plugins.jenkinsaisynapse.api.tools;

import org.jspecify.annotations.NonNull;

/**
 * The outcome of a tool call, as reported back to the model.
 * @param isError Whether the tool call failed. When true, content explains why.
 * @param content The tool output, or the error message when isError is true.
 */
public record ToolResult(boolean isError, @NonNull String content) {
    public ToolResult {
        if (content == null) {
            content = "";
        }
    }

    public static ToolResult success(String content) {
        return new ToolResult(false, content);
    }

    public static ToolResult error(String message) {
        return new ToolResult(true, message);
    }
}
