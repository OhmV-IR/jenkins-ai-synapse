package dev.ohmvir.plugins.jenkinsaisynapse.api.models;

public enum ModelFinishReason {
    /** Model just felt like stopping there as the natural end of its response, or it hit a stop token. Cannot differentiate between regular stop and stop sequence because most major APIs don't */
    STOP,
    /** Model hit a token cap which stopped its response */
    TOKEN_CAP,
    /** Model hit some sort of safety safeguard, either restricted topics or training data / copyrighted material extraction */
    SAFEGUARD,
    /** Model stopped responding to wait for tool calls */
    TOOL_CALLS,
    /** Model declined to fulfil the request */
    REFUSAL,
    /** The request and response no longer fit in the model's context window */
    CONTEXT_WINDOW_EXCEEDED,
    /** Model stopped for a reason this plugin doesn't know about. The provider's own reason is kept on the output */
    OTHER
}
