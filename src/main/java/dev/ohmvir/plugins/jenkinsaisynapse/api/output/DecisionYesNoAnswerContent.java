package dev.ohmvir.plugins.jenkinsaisynapse.api.output;

import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelOutputType;
import dev.ohmvir.plugins.jenkinsaisynapse.utils.DecisionUtils;
import hudson.Extension;
import java.util.Set;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * The answer to a {@link dev.ohmvir.plugins.jenkinsaisynapse.api.input.DecisionYesNoQuestionContent}.
 * The System One {@code noul} answer type.
 */
public class DecisionYesNoAnswerContent extends ModelOutput implements DecisionAnswer {
    private @Getter final String questionName;
    /** The probability that the answer is yes, from 0 to 1 */
    private @Getter final double probability;

    @DataBoundConstructor
    public DecisionYesNoAnswerContent(String questionName, double probability) {
        this.questionName = DecisionUtils.requireQuestionName(questionName);
        this.probability = DecisionUtils.requireProbability(probability, "probability");
    }

    /**
     * @param threshold How likely a yes has to be to count as one, from 0 to 1. Raise it where a false yes is the
     *     expensive mistake, lower it where a false no is.
     * @return Whether the probability of yes reaches the threshold
     */
    public boolean isYes(double threshold) {
        return probability >= DecisionUtils.requireProbability(threshold, "threshold");
    }

    /** @return Whether yes is at least as likely as no */
    public boolean isYes() {
        return isYes(0.5);
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
            return "Decision Yes/No Answer Content";
        }
    }
}
