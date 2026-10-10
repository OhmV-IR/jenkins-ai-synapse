package io.ohmvir.plugins.jenkinsaisynapse.api.output;

import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import java.util.Set;
import lombok.Getter;

public class TokenUtilizationContent extends ModelOutput {
    public static final ModelOutputKind<TokenUtilizationContent> KIND =
            new ModelOutputKind<>(TokenUtilizationContent.class, Set.of(ModelCapability.TOKEN_USAGE_METRICS), Set.of());

    private @Getter final long inputTokensUsed;
    private @Getter final long outputTokensUsed;
    private @Getter final long cachedTokensUsed;

    public TokenUtilizationContent(long inputTokensUsed, long outputTokensUsed, long cachedTokensUsed) {
        this.inputTokensUsed = inputTokensUsed;
        this.outputTokensUsed = outputTokensUsed;
        this.cachedTokensUsed = cachedTokensUsed;
    }

    @Override
    public ModelOutputKind<TokenUtilizationContent> getKind() {
        return KIND;
    }
}
