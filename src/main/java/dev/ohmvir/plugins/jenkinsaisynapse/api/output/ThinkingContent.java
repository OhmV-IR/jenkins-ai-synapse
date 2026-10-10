package io.ohmvir.plugins.jenkinsaisynapse.api.output;

import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import java.util.Set;
import lombok.Getter;

public class ThinkingContent extends ModelOutput {
    public static final ModelOutputKind<ThinkingContent> KIND =
            new ModelOutputKind<>(ThinkingContent.class, Set.of(ModelCapability.THINKING), Set.of());

    private @Getter final String thinking;

    public ThinkingContent(String thinking) {
        this.thinking = thinking;
    }

    @Override
    public ModelOutputKind<ThinkingContent> getKind() {
        return KIND;
    }
}
