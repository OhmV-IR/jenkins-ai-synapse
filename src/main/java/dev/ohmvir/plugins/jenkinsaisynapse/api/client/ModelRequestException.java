package io.ohmvir.plugins.jenkinsaisynapse.api.client;

/**
 * Thrown when a model request cannot be completed: no client is available, the client failed, the request timed out
 * or the model did not produce the requested outputs.
 */
public class ModelRequestException extends Exception {
    public ModelRequestException(String message) {
        super(message);
    }

    public ModelRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
