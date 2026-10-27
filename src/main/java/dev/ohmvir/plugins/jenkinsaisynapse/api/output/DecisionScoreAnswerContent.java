package dev.ohmvir.plugins.jenkinsaisynapse.api.output;

import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelOutputType;
import dev.ohmvir.plugins.jenkinsaisynapse.utils.DecisionUtils;
import hudson.Extension;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * The answer to a {@link dev.ohmvir.plugins.jenkinsaisynapse.api.input.DecisionScoreQuestionContent}.
 * The System One {@code score} answer type.
 */
public class DecisionScoreAnswerContent extends ModelOutput implements DecisionAnswer {
    public static final ModelOutputKind<DecisionScoreAnswerContent> KIND =
            new ModelOutputKind<>(DecisionScoreAnswerContent.class, Set.of(ModelCapability.DECISIONS), Set.of());

    private @Getter final String questionName;
    /** The expected level along the rubric, from 0 to the highest level. May fall between two levels. */
    private @Getter final double score;
    /**
     * How concentrated the model's distribution over the levels is, from 0 to 1. This is not the chance the score is
     * right, so calibrate any threshold on it against your own data.
     */
    private @Getter final double confidence;

    private final TreeMap<Integer, Double> probabilities;

    /**
     * @param probabilities The probability of each level, with one entry for every level from 0 up
     */
    @DataBoundConstructor
    public DecisionScoreAnswerContent(
            String questionName, double score, double confidence, Map<Integer, Double> probabilities) {
        this.questionName = DecisionUtils.requireQuestionName(questionName);
        this.confidence = DecisionUtils.requireProbability(confidence, "confidence");
        this.probabilities = new TreeMap<>(DecisionUtils.requireProbabilities(probabilities, "probabilities"));
        if (this.probabilities.firstKey() != 0 || this.probabilities.lastKey() != this.probabilities.size() - 1) {
            throw new IllegalArgumentException("probabilities must have one entry for every level from 0, got levels: "
                    + this.probabilities.keySet());
        }
        if (!Double.isFinite(score) || score < 0 || score > probabilities.size() - 1) {
            throw new IllegalArgumentException(
                    "score must be between 0 and the highest level " + (probabilities.size() - 1) + ", got: " + score);
        }
        this.score = score;
    }

    /** @return The probability of each level, in level order */
    public Map<Integer, Double> getProbabilities() {
        return Collections.unmodifiableMap(probabilities);
    }

    @Override
    public ModelOutputKind<DecisionScoreAnswerContent> getKind() {
        return KIND;
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
            return "Decision Score Answer Content";
        }
    }
}
