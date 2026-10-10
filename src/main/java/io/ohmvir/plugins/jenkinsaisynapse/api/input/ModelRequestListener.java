package io.ohmvir.plugins.jenkinsaisynapse.api.input;

import io.ohmvir.plugins.jenkinsaisynapse.api.output.ModelOutput;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.ToolCallContent;

/**
 * Receives progress while a {@link ModelRequest} executes, for example to stream it to a build log.
 * Attach one with {@link ModelRequest#setListener(ModelRequestListener)}.
 * <p>
 * Calls may come from a thread other than the one executing the request, and implementations must not throw.
 */
public interface ModelRequestListener {
    ModelRequestListener NOOP = new ModelRequestListener() {};

    /**
     * Called by clients that support streaming with each new piece of response text, as it is generated.
     * The complete text is still passed to {@link #onOutput(ModelOutput)} once the turn finishes.
     */
    default void onTextDelta(String delta) {}

    /**
     * Called by clients that support streaming with each new piece of thinking, as it is generated.
     * The complete thinking is still passed to {@link #onOutput(ModelOutput)} once the turn finishes.
     */
    default void onThinkingDelta(String delta) {}

    /**
     * Called by the request once for every output the model produces, in order, as soon as the turn producing it ends.
     */
    default void onOutput(ModelOutput output) {}

    /**
     * Called by the request after it has executed a tool the model asked for.
     */
    default void onToolResult(ToolCallContent call, ToolCallResponseContent result) {}
}
