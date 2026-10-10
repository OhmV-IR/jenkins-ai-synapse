package io.ohmvir.plugins.jenkinsaisynapse.api.agent;

import io.ohmvir.plugins.jenkinsaisynapse.api.message.ToolCallPart;
import io.ohmvir.plugins.jenkinsaisynapse.api.message.ToolResultPart;
import io.ohmvir.plugins.jenkinsaisynapse.api.request.ModelRequest;
import io.ohmvir.plugins.jenkinsaisynapse.api.tools.Tool;
import io.ohmvir.plugins.jenkinsaisynapse.api.tools.ToolRegistry;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Runs tool calls with the {@link Tool}s in the {@link ToolRegistry}, allowing only the tools the request offers.
 */
public class RegistryToolExecutor implements ToolExecutor {
    private static final Logger LOGGER = Logger.getLogger(RegistryToolExecutor.class.getName());

    @Override
    public ToolResultPart execute(ModelRequest request, ToolCallPart call) throws InterruptedException {
        if (request.getTool(call.getName()) == null) {
            return ToolResultPart.error(call, "Tool " + call.getName() + " is not available");
        }
        Tool tool = ToolRegistry.getTool(call.getName());
        if (tool == null) {
            return ToolResultPart.error(call, "Tool " + call.getName() + " is not installed");
        }
        try {
            return ToolResultPart.success(call, tool.invoke(call.getArguments()));
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Tool " + call.getName() + " failed", e);
            return ToolResultPart.error(call, "Tool " + call.getName() + " failed: " + e.getMessage());
        }
    }
}
