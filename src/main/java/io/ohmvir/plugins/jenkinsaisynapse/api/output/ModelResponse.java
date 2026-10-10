package io.ohmvir.plugins.jenkinsaisynapse.api.output;

import io.ohmvir.plugins.jenkinsaisynapse.api.ModelContent;
import java.util.*;

public class ModelResponse {
    private final List<ModelOutput> outputs;
    private final List<ModelContent> conversation;

    /**
     * @param outputs All outputs the model produced while executing the request
     * @param conversation The full ordered transcript of the exchange: prior history, the request inputs, model outputs and tool responses
     */
    public ModelResponse(List<ModelOutput> outputs, List<ModelContent> conversation) {
        this.outputs = new ArrayList<>(outputs);
        this.conversation = new ArrayList<>(conversation);
    }

    public List<ModelOutput> getOutputs() {
        return Collections.unmodifiableList(outputs);
    }

    /**
     * The conversation is not persisted anywhere, to continue it pass this response to
     * {@link io.ohmvir.plugins.jenkinsaisynapse.api.input.ModelRequest#continueConversation(ModelResponse)} on a future request.
     *
     * @return The full ordered transcript of the exchange that produced this response
     */
    public List<ModelContent> getConversation() {
        return Collections.unmodifiableList(conversation);
    }
}
