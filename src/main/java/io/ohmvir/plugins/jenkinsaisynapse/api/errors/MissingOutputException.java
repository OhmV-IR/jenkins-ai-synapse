package io.ohmvir.plugins.jenkinsaisynapse.api.errors;

import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelOutputType;
import io.ohmvir.plugins.jenkinsaisynapse.api.response.ModelResponse;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Thrown when the model finished without producing an output the request required.
 */
public class MissingOutputException extends ModelException {
    private final Set<ModelOutputType> missingOutputs;
    private final transient ModelResponse response;

    public MissingOutputException(Set<ModelOutputType> missingOutputs, ModelResponse response) {
        super("The model's response did not contain the required outputs: "
                + missingOutputs.stream().map(Enum::name).sorted().collect(Collectors.joining(", ")));
        this.missingOutputs = Set.copyOf(missingOutputs);
        this.response = response;
    }

    public Set<ModelOutputType> getMissingOutputs() {
        return missingOutputs;
    }

    /**
     * @return The response the model did give.
     */
    public ModelResponse getResponse() {
        return response;
    }
}
