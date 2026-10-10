package io.ohmvir.plugins.jenkinsaisynapse.api.errors;

/**
 * Base class of the failures of a model request. Messages are written to be shown to the user.
 */
public class ModelException extends Exception {
    public ModelException(String message) {
        super(message);
    }

    public ModelException(String message, Throwable cause) {
        super(message, cause);
    }
}
