package dev.ohmvir.plugins.jenkinsaisynapse.api.output;

import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelOutputType;
import java.util.Objects;
import java.util.Set;

/**
 * Describes a type of {@link ModelOutput} and what a model needs to produce it. Each output class exposes one as a
 * {@code KIND} constant, which is what {@link dev.ohmvir.plugins.jenkinsaisynapse.api.input.ModelRequest#requestOutput}
 * takes.
 */
public record ModelOutputKind<T extends ModelOutput>(
        Class<T> type, Set<ModelCapability> requiredCapabilities, Set<ModelOutputType> requiredOutputTypes) {
    public ModelOutputKind {
        Objects.requireNonNull(type, "type");
        requiredCapabilities = Set.copyOf(requiredCapabilities);
        requiredOutputTypes = Set.copyOf(requiredOutputTypes);
    }

    public boolean isKindOf(ModelOutput output) {
        return type.isInstance(output);
    }
}
