package dev.ohmvir.plugins.jenkinsaisynapse.api.output;

/**
 * A model's answer to a {@link dev.ohmvir.plugins.jenkinsaisynapse.api.input.DecisionQuestion}.
 */
public interface DecisionAnswer {
    /** @return The name of the question this answers */
    String getQuestionName();
}
