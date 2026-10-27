package dev.ohmvir.plugins.jenkinsaisynapse.api.models;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

public enum ModelThinkingLevel {
    OFF(0),
    /** Thinking enabled at the provider's default effort, for models that don't offer graded levels */
    ON(null),
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    EXTRA_HIGH(4),
    MAX(5);

    /** Position on the effort scale, or null for {@link #ON} which has no fixed position */
    private final @Nullable Integer effort;

    ModelThinkingLevel(@Nullable Integer effort) {
        this.effort = effort;
    }

    /**
     * Finds the supported level closest to this one.
     * <ul>
     *     <li>A supported level is always used as is.</li>
     *     <li>A graded level ({@link #LOW} to {@link #MAX}) maps to the nearest supported graded level, preferring the
     *     higher one on a tie, then to {@link #ON}.</li>
     *     <li>{@link #ON} maps to the graded level nearest {@link #MEDIUM}.</li>
     *     <li>{@link #OFF} is satisfied by a model that doesn't think at all; on a model that always thinks it maps to
     *     the lowest effort available.</li>
     * </ul>
     *
     * @param supported The levels a model supports
     * @return The level to send to the model, or empty if the model can't think and thinking was requested
     */
    public Optional<ModelThinkingLevel> resolveAgainst(Collection<ModelThinkingLevel> supported) {
        if (supported.contains(this)) {
            return Optional.of(this);
        }
        boolean canThink = supported.stream().anyMatch(level -> level != OFF);
        if (!canThink) {
            return this == OFF ? Optional.of(OFF) : Optional.empty();
        }
        int target = this == OFF ? 0 : effort == null ? MEDIUM.effort : effort;
        Optional<ModelThinkingLevel> nearestGraded = supported.stream()
                .filter(level -> level.effort != null && level != OFF)
                .min(Comparator.<ModelThinkingLevel>comparingInt(level -> Math.abs(level.effort - target))
                        .thenComparing(level -> -level.effort));
        return nearestGraded.isPresent() ? nearestGraded : Optional.of(ON);
    }
}
