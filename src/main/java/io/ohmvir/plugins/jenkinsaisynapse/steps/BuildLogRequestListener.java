package io.ohmvir.plugins.jenkinsaisynapse.steps;

import io.ohmvir.plugins.jenkinsaisynapse.api.agent.AgentListener;
import io.ohmvir.plugins.jenkinsaisynapse.api.message.CitationPart;
import io.ohmvir.plugins.jenkinsaisynapse.api.message.ContentPart;
import io.ohmvir.plugins.jenkinsaisynapse.api.message.TextPart;
import io.ohmvir.plugins.jenkinsaisynapse.api.message.ThinkingPart;
import io.ohmvir.plugins.jenkinsaisynapse.api.message.ToolCallPart;
import io.ohmvir.plugins.jenkinsaisynapse.api.message.ToolResultPart;
import io.ohmvir.plugins.jenkinsaisynapse.api.response.StepResult;
import java.io.PrintStream;
import org.jspecify.annotations.Nullable;

/**
 * Prints a request's progress to the build log as it happens: streamed text when the client supports streaming,
 * otherwise each step's message as soon as the model produces it.
 */
class BuildLogRequestListener implements AgentListener {
    private static final int MAX_TOOL_RESULT_LENGTH = 2000;

    private final PrintStream logger;
    private final boolean quiet;
    private final boolean verbose;

    /** Whether text or thinking of the current step was streamed, so the complete message is not printed again. */
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
    public synchronized void onStep(int step, StepResult result) {
        for (ContentPart part : result.message().getParts()) {
            switch (part) {
                case TextPart text -> {
                    if (!quiet && !textStreamed) {
                        println(text.getText());
                    }
                }
                case ThinkingPart thinking -> {
                    if (verbose && !thinkingStreamed) {
                        println("[thinking] " + thinking.getThinking());
                    }
                }
                case ToolCallPart toolCall -> {
                    if (!quiet) {
                        println("[aiRequest] Calling tool " + toolCall.getName()
                                + (verbose ? " with " + toolCall.getArgumentsJson() : ""));
                    }
                }
                case CitationPart citation -> {
                    if (verbose) {
                        println("[citation] " + citation.getCitedText());
                    }
                }
                default -> {}
            }
        }
        if (verbose) {
            println("[aiRequest] Step " + step + " finished: " + result.finishReason());
        }
        // The next step streams afresh.
        textStreamed = false;
        thinkingStreamed = false;
    }

    @Override
    public synchronized void onToolResult(ToolCallPart call, ToolResultPart result) {
        if (quiet) {
            return;
        }
        if (result.isError()) {
            println("[aiRequest] Tool " + call.getName() + " failed: " + truncate(result.getContent()));
        } else if (verbose) {
            println("[aiRequest] Tool " + call.getName() + " returned: " + truncate(result.getContent()));
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
