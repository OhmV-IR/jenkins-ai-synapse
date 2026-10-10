package io.ohmvir.plugins.jenkinsaisynapse.api.input;

import java.io.IOException;

/**
 * Thrown when a {@link ModelRequest} cannot be completed. The message is meant to be shown to the user.
 */
public class ModelRequestException extends IOException {
    public ModelRequestException(String message) {
        super(message);
    }

    public ModelRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
