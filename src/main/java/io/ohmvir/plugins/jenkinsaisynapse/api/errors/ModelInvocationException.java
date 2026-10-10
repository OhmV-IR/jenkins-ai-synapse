package io.ohmvir.plugins.jenkinsaisynapse.api.errors;

/**
 * Thrown when calling the model fails: no client is installed for it, or the client or provider reported an error.
 * Clients throw it with a message describing the provider's error.
 */
public class ModelInvocationException extends ModelException {
    public ModelInvocationException(String message) {
        super(message);
    }

    public ModelInvocationException(String message, Throwable cause) {
        super(message, cause);
    }
}
