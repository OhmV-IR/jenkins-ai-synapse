package dev.ohmvir.plugins.jenkinsaisynapse.api.client;

import dev.ohmvir.plugins.jenkinsaisynapse.api.input.ToolCallResponseContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.ModelOutput;
import java.util.List;

/**
 * Receives progress while a {@link dev.ohmvir.plugins.jenkinsaisynapse.api.input.ModelRequest} executes, for example to
 * write it to a build log as it happens. Callbacks may come from a thread other than the one executing the request.
 */
public interface ModelRequestListener {
    ModelRequestListener NOOP = new ModelRequestListener() {};

    /**
     * Called by clients that stream their responses, with each piece of text as the model generates it. The complete
     * text is still returned as an {@link dev.ohmvir.plugins.jenkinsaisynapse.api.output.OutputTextContent} when the
     * step completes.
     */
    default void onTextDelta(String delta) {}

    /**
     * Called after each exchange (API round trip) with the model, with the outputs it produced.
     */
    default void onStepCompleted(List<ModelOutput> outputs) {}

    /**
     * Called after a tool the model asked for has run, before its result is sent back to the model.
     */
    default void onToolResult(ToolCallResponseContent result) {}
}
