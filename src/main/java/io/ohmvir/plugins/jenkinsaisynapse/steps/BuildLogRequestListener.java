package io.ohmvir.plugins.jenkinsaisynapse.steps;

import io.ohmvir.plugins.jenkinsaisynapse.api.input.ModelRequestListener;
import io.ohmvir.plugins.jenkinsaisynapse.api.input.ToolCallResponseContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.CitationContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.FinishReasonContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.ModelOutput;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.OutputTextContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.ThinkingContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.ToolCallContent;
import java.io.PrintStream;
import org.jspecify.annotations.Nullable;

/**
 * Prints a request's progress to the build log as it happens: streamed text when the client supports streaming,
 * otherwise each complete output as soon as the model's turn ends.
 */
class BuildLogRequestListener implements ModelRequestListener {
    private static final int MAX_TOOL_RESULT_LENGTH = 2000;

    private final PrintStream logger;
    private final boolean quiet;
    private final boolean verbose;

    /** Whether text or thinking of the current turn was streamed, so the complete output is not printed again. */
    private boolean textStreamed;

    private boolean thinkingStreamed;
    /** Whether a streamed line has been started and not yet ended. */
    private boolean lineOpen;

    BuildLogRequestListener(PrintStream logger, boolean quiet, boolean verbose) {
        this.logger = logger;
        this.quiet = quiet;
        this.verbose = verbose;
    }

    @Override
    public synchronized void onTextDelta(String delta) {
        if (quiet || delta == null) {
            return;
        }
        if (thinkingStreamed && !textStreamed) {
            endLine();
        }
        textStreamed = true;
        print(delta);
    }

    @Override
    public synchronized void onThinkingDelta(String delta) {
        if (!verbose || delta == null) {
            return;
        }
        if (!thinkingStreamed) {
            endLine();
            print("[thinking] ");
        }
        thinkingStreamed = true;
        print(delta);
    }

    @Override
    public synchronized void onOutput(ModelOutput output) {
        switch (output) {
            case OutputTextContent text -> {
                if (!quiet && !textStreamed) {
                    println(text.getText());
                }
            }
            case ThinkingContent thinking -> {
                if (verbose && !thinkingStreamed) {
                    println("[thinking] " + thinking.getThinking());
                }
            }
            case ToolCallContent toolCall -> {
                if (!quiet) {
                    println("[aiRequest] Calling tool " + toolCall.getName()
                            + (verbose ? " with " + toolCall.getArgumentsPrettyJson() : ""));
                }
            }
            case CitationContent citation -> {
                if (verbose) {
                    println("[citation] " + citation.getCitedText());
                }
            }
            case FinishReasonContent finishReason -> {
                if (verbose) {
                    println("[aiRequest] Turn finished: " + finishReason.getReason());
                }
            }
            default -> {}
        }
    }

    @Override
    public synchronized void onToolResult(ToolCallContent call, ToolCallResponseContent result) {
        // A tool result means the next turn is about to start.
        textStreamed = false;
        thinkingStreamed = false;
        if (quiet) {
            return;
        }
        if (!result.isSuccessful()) {
            println("[aiRequest] Tool " + call.getName() + " failed");
        } else if (verbose) {
            println("[aiRequest] Tool " + call.getName() + " returned: " + truncate(result.getResponseContent()));
        }
    }

    /**
     * Ends any line left open by streaming. Called once the request has finished.
     */
    synchronized void finish() {
        endLine();
    }

    private void print(String text) {
        if (text.isEmpty()) {
            return;
        }
        logger.print(text);
        lineOpen = !text.endsWith("\n");
    }

    private void println(@Nullable String text) {
        endLine();
        logger.println(text);
    }

    private void endLine() {
        if (lineOpen) {
            logger.println();
            lineOpen = false;
        }
    }

    private static String truncate(@Nullable String text) {
        if (text == null) {
            return "";
        }
        if (text.length() <= MAX_TOOL_RESULT_LENGTH) {
            return text;
        }
        return text.substring(0, MAX_TOOL_RESULT_LENGTH) + "... (" + (text.length() - MAX_TOOL_RESULT_LENGTH)
                + " more characters)";
    }
}
