package io.ohmvir.plugins.jenkinsaisynapse.api.agent;

import io.ohmvir.plugins.jenkinsaisynapse.api.message.ToolCallPart;
import io.ohmvir.plugins.jenkinsaisynapse.api.message.ToolResultPart;
import io.ohmvir.plugins.jenkinsaisynapse.api.response.StepResult;

/**
 * Follows a request while {@link AgentLoop} runs it, for example to stream it to a build log.
 * Calls may come from a thread other than the one running the loop, and implementations must not throw.
 */
public interface AgentListener {
    AgentListener NOOP = new AgentListener() {};

    /**
     * Called by clients that support streaming with each new piece of response text, as it is generated. The complete
     * message is still passed to {@link #onStep(int, StepResult)}.
     */
    default void onTextDelta(String delta) {}

    /**
     * Called by clients that support streaming with each new piece of thinking, as it is generated.
     */
    default void onThinkingDelta(String delta) {}

    /**
     * Called after each call to the model, with what it produced.
     *
     * @param step The number of the call, starting at 1.
     */
    default void onStep(int step, StepResult result) {}

    /**
     * Called after a tool the model asked for has run.
     */
    default void onToolResult(ToolCallPart call, ToolResultPart result) {}
}
