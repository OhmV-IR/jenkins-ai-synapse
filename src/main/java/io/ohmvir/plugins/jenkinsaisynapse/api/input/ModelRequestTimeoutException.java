package io.ohmvir.plugins.jenkinsaisynapse.api.input;

/**
 * Thrown when a {@link ModelRequest}, or a single call to its model client, takes longer than allowed.
 */
public class ModelRequestTimeoutException extends ModelRequestException {
    public ModelRequestTimeoutException(String message) {
        super(message);
    }
}
