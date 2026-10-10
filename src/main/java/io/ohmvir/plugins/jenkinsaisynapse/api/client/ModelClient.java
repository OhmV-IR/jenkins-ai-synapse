package io.ohmvir.plugins.jenkinsaisynapse.api.client;

import io.ohmvir.plugins.jenkinsaisynapse.api.input.ModelInput;
import io.ohmvir.plugins.jenkinsaisynapse.api.input.ModelRequest;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelData;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelProvider;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.ModelOutput;
import java.io.IOException;
import java.util.List;

/**
 * Sends requests to one configured model. Created by {@link ModelProvider#createModelClient}, which hands it the model
 * configuration it needs.
 */
@FunctionalInterface
public interface ModelClient {
    /**
     * Generate a response for the given request. This function can assume that the request is able to be handled by the model.
     * Thinking levels should be sent as resolved by {@link ModelData#resolveThinkingLevel}.
     * @param modelData What the model supports
     * @param request The associated model request
     * @param turnInputs The inputs to attach for the model to respond. This will include all context and relevant conversation history, as well as things like tool responses
     * @return A list of all the model outputs that were produced on this exchange (ie API roundtrip)
     */
    List<ModelOutput> takeStep(ModelData modelData, ModelRequest request, List<ModelInput> turnInputs)
            throws IOException, InterruptedException;
}
