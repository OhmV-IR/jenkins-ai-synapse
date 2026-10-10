package io.ohmvir.plugins.jenkinsaisynapse.api.agent;

import io.ohmvir.plugins.jenkinsaisynapse.api.message.ToolCallPart;
import io.ohmvir.plugins.jenkinsaisynapse.api.message.ToolResultPart;
import io.ohmvir.plugins.jenkinsaisynapse.api.request.ModelRequest;

/**
 * Runs the tools a model calls.
 */
public interface ToolExecutor {
    /**
     * Runs a tool call. Failures, including calls to tools the request does not offer, are reported as an
     * {@link ToolResultPart#isError() error result} so the model can react to them.
     *
     * @param request The request being run; only its tools may be called.
     */
    ToolResultPart execute(ModelRequest request, ToolCallPart call) throws InterruptedException;
}
