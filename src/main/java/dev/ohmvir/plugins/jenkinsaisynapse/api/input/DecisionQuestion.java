package dev.ohmvir.plugins.jenkinsaisynapse.api.input;

import org.jspecify.annotations.Nullable;

/**
 * A typed question for a model with {@link dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability#DECISIONS}
 * to answer about the rest of the request's inputs, which make up the state being decided about.
 * Modelled on the System One decisions API ({@code POST /v1/systemone}), which clients translate to and from.
 * Each question is answered by the {@link dev.ohmvir.plugins.jenkinsaisynapse.api.output.DecisionAnswer} with the same
 * name, so names must be unique within a request.
 */
public interface DecisionQuestion {
    /** @return The name the question is answered under, unique within the request */
    String getName();

    /** @return What to decide about the state, or null if the question's criteria say enough on their own */
    @Nullable
    String getInstructions();
}
