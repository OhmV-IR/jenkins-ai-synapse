package io.ohmvir.plugins.jenkinsaisynapse.api.message;

/**
 * Who a {@link Message} comes from.
 */
public enum Role {
    /** The person or pipeline asking. */
    USER,
    /** The model. */
    ASSISTANT,
    /** Results of the tools the model called in the preceding assistant message. */
    TOOL
}
