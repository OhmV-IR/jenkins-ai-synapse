package io.ohmvir.plugins.jenkinsaisynapse.api.models;

public enum ModelFinishReason {
    /** Model just felt like stopping there as the natural end of its response, or it hit a stop token. Cannot differentiate between regular stop and stop sequence because most major APIs don't */
    STOP,
    /** Model hit a token cap which stopped its response */
    TOKEN_CAP,
    /** Model hit some sort of safety safeguard, either restricted topics or training data / copyrighted material extraction */
    SAFEGUARD,
    /** Model stopped responding to wait for tool calls */
    TOOL_CALLS,
}
