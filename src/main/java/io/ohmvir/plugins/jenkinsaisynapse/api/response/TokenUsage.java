package io.ohmvir.plugins.jenkinsaisynapse.api.response;

/**
 * Tokens used by one call to a model, or summed over several.
 */
public record TokenUsage(long inputTokens, long outputTokens, long cachedTokens) {
    public static final TokenUsage NONE = new TokenUsage(0, 0, 0);

    public TokenUsage plus(TokenUsage other) {
        return new TokenUsage(
                inputTokens + other.inputTokens, outputTokens + other.outputTokens, cachedTokens + other.cachedTokens);
    }
}
