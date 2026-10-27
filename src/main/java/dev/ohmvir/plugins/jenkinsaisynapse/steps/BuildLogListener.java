package dev.ohmvir.plugins.jenkinsaisynapse.steps;

import dev.ohmvir.plugins.jenkinsaisynapse.api.client.ModelRequestListener;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.ToolCallResponseContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.FinishReasonContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.ModelOutput;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.OutputTextContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.ThinkingContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.TokenUtilizationContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.ToolCallContent;
import java.io.PrintStream;
import java.util.List;

/**
 * Writes the progress of a request to a build log as it happens.
 */
final class BuildLogListener implements ModelRequestListener {
    static final int MAX_TOOL_RESULT_LENGTH = 500;

    private final PrintStream logger;
    private boolean streamedText;

    BuildLogListener(PrintStream logger) {
        this.logger = logger;
    }

    @Override
    public synchronized void onTextDelta(String delta) {
        logger.print(delta);
        logger.flush();
        streamedText = true;
    }

    @Override
    public synchronized void onStepCompleted(List<ModelOutput> outputs) {
        // Text that was already streamed is not printed again.
        boolean textAlreadyLogged = streamedText;
        if (streamedText) {
            logger.println();
            streamedText = false;
        }
        for (ModelOutput output : outputs) {
            switch (output) {
                case OutputTextContent text -> {
                    if (!textAlreadyLogged) {
                        logger.println("[AI] " + text.getText());
                    }
                }
                case ThinkingContent thinking -> logger.println("[AI] Thinking: " + thinking.getThinking());
                case ToolCallContent toolCall ->
                    logger.println("[AI] Calling tool " + toolCall.getName() + " with " + toolCall.getToolArguments());
                case TokenUtilizationContent usage ->
                    logger.println("[AI] Tokens used: " + usage.getInputTokensUsed() + " input, "
                            + usage.getOutputTokensUsed() + " output, " + usage.getCachedTokensUsed() + " cached");
                case FinishReasonContent finishReason ->
                    logger.println("[AI] Finish reason: " + finishReason.getReason());
                default -> logger.println("[AI] Received " + output.getClass().getSimpleName());
            }
        }
    }

    @Override
    public synchronized void onToolResult(ToolCallResponseContent result) {
        String toolName = result.getCalledTool() == null
                ? result.getToolUseId()
                : result.getCalledTool().getName();
        if (!result.isSuccessful()) {
            logger.println("[AI] Tool " + toolName + " failed");
            return;
        }
        String content = result.getResponseContent() == null ? "" : result.getResponseContent();
        if (content.length() > MAX_TOOL_RESULT_LENGTH) {
            content = content.substring(0, MAX_TOOL_RESULT_LENGTH) + "...";
        }
        logger.println("[AI] Tool " + toolName + " returned: " + content);
    }
}
