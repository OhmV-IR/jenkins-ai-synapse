package io.ohmvir.plugins.jenkinsaisynapse.api.errors;

import io.ohmvir.plugins.jenkinsaisynapse.api.message.Message;
import java.util.List;

/**
 * Thrown when the model is still calling tools after the maximum number of steps.
 */
public class MaxStepsExceededException extends ModelException {
    private final transient List<Message> transcript;

    public MaxStepsExceededException(int maxSteps, List<Message> transcript) {
        super("The model was still calling tools after " + maxSteps + " steps");
        this.transcript = List.copyOf(transcript);
    }

    /**
     * @return The conversation up to the point the request was stopped.
     */
    public List<Message> getTranscript() {
        return transcript;
    }
}
