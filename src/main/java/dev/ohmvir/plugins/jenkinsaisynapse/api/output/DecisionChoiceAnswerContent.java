package dev.ohmvir.plugins.jenkinsaisynapse.api.output;

import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelOutputType;
import dev.ohmvir.plugins.jenkinsaisynapse.utils.DecisionUtils;
import hudson.Extension;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * The answer to a {@link dev.ohmvir.plugins.jenkinsaisynapse.api.input.DecisionChoiceQuestionContent}.
 * The System One {@code choice} answer type.
 */
public class DecisionChoiceAnswerContent extends ModelOutput implements DecisionAnswer {
    private @Getter final String questionName;
    /** The label of the selected option */
    private @Getter final String choice;
    /**
     * How concentrated the model's distribution over the options is, from 0 to 1. This is not the chance the choice is
     * right, so calibrate any threshold on it against your own data.
     */
    private @Getter final double confidence;

    private final LinkedHashMap<String, Double> probabilities;

    /**
     * @param probabilities The probability of each option label, which must include the selected choice
     */
    @DataBoundConstructor
    public DecisionChoiceAnswerContent(
            String questionName, String choice, double confidence, Map<String, Double> probabilities) {
        this.questionName = DecisionUtils.requireQuestionName(questionName);
        this.confidence = DecisionUtils.requireProbability(confidence, "confidence");
        this.probabilities = new LinkedHashMap<>(DecisionUtils.requireProbabilities(probabilities, "probabilities"));
        if (choice == null || !this.probabilities.containsKey(choice)) {
            throw new IllegalArgumentException("choice must be one of the options in probabilities, got: " + choice);
        }
        this.choice = choice;
    }

    /** @return The probability of each option label */
    public Map<String, Double> getProbabilities() {
        return Collections.unmodifiableMap(probabilities);
    }

    @Extension
    public static class DescriptorImpl extends ModelOutputDescriptor {
        @Override
        public Set<ModelCapability> getRequiredCapabilities() {
            return Set.of(ModelCapability.DECISIONS);
        }

        @Override
        public Set<ModelOutputType> getRequiredOutputTypes() {
            return Set.of();
        }

        @Override
        public @NonNull String getDisplayName() {
            return "Decision Choice Answer Content";
        }
    }
}
