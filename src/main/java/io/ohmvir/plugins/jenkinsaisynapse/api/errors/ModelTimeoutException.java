package io.ohmvir.plugins.jenkinsaisynapse.api.errors;

/**
 * Thrown when a call to the model, or the whole request, takes longer than allowed.
 */
public class ModelTimeoutException extends ModelInvocationException {
    public ModelTimeoutException(String message) {
        super(message);
    }
}
