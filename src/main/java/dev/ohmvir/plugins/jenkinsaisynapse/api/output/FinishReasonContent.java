package dev.ohmvir.plugins.jenkinsaisynapse.api.output;

import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelFinishReason;
import java.util.Objects;
import java.util.Set;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

public class FinishReasonContent extends ModelOutput {
    public static final ModelOutputKind<FinishReasonContent> KIND =
            new ModelOutputKind<>(FinishReasonContent.class, Set.of(ModelCapability.PREMATURE_STOP), Set.of());

    private @Getter final ModelFinishReason reason;
    /** The reason exactly as the provider reported it, eg {@code end_turn}, so nothing is lost for {@link ModelFinishReason#OTHER} */
    private @Getter final @Nullable String providerReason;

    public FinishReasonContent(ModelFinishReason reason) {
        this(reason, null);
    }

    public FinishReasonContent(ModelFinishReason reason, @Nullable String providerReason) {
        this.reason = Objects.requireNonNull(reason, "reason");
        this.providerReason = providerReason;
    }

    @Override
    public ModelOutputKind<FinishReasonContent> getKind() {
        return KIND;
    }
}
