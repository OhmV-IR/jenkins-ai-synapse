package dev.ohmvir.plugins.jenkinsaisynapse.api.input;

import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelInputType;
import dev.ohmvir.plugins.jenkinsaisynapse.utils.DecisionUtils;
import hudson.Extension;
import java.util.Set;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * A yes/no question, answered with the probability of yes by a
 * {@link dev.ohmvir.plugins.jenkinsaisynapse.api.output.DecisionYesNoAnswerContent}.
 * The System One {@code noul} question type.
 */
public class DecisionYesNoQuestionContent extends ModelInput implements DecisionQuestion {
    private @Getter final String name;
    private @Getter final @Nullable String instructions;
    /** Description of the yes outcome, or null to leave it undescribed */
    private @Getter final @Nullable String trueCriteria;
    /** Description of the no outcome, or null to leave it undescribed */
    private @Getter final @Nullable String falseCriteria;

    @DataBoundConstructor
    public DecisionYesNoQuestionContent(
            String name, @Nullable String instructions, @Nullable String trueCriteria, @Nullable String falseCriteria) {
        this.name = DecisionUtils.requireQuestionName(name);
        if (instructions == null && trueCriteria == null && falseCriteria == null) {
            throw new IllegalArgumentException("A yes/no question needs instructions or a description of its outcomes");
        }
        this.instructions = instructions;
        this.trueCriteria = trueCriteria;
        this.falseCriteria = falseCriteria;
    }

    public DecisionYesNoQuestionContent(String name, String instructions) {
        this(name, instructions, null, null);
    }

    @Extension
    public static class DescriptorImpl extends ModelInputDescriptor {
        @Override
        public Set<ModelCapability> getRequiredCapabilities() {
            return Set.of(ModelCapability.DECISIONS);
        }

        @Override
        public Set<ModelInputType> getRequiredInputTypes() {
            return Set.of();
        }

        @Override
        public @NonNull String getDisplayName() {
            return "Decision Yes/No Question Content";
        }
    }
}
