package io.ohmvir.plugins.jenkinsaisynapse.api.message;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * The model's reasoning. Clients must send thinking back unchanged, signature included, when the provider requires it
 * (for example when continuing after a tool call).
 */
public final class ThinkingPart implements ContentPart {
    private final String thinking;
    private final @Nullable String signature;

    public ThinkingPart(String thinking, @Nullable String signature) {
        this.thinking = Objects.requireNonNull(thinking, "thinking");
        this.signature = signature;
    }

    public String getThinking() {
        return thinking;
    }

    /**
     * @return The provider's opaque signature or encrypted form of the thinking, if it gave one.
     */
    public @Nullable String getSignature() {
        return signature;
    }

    @Override
    public String toString() {
        return "ThinkingPart[" + thinking + "]";
    }
}
