package dev.ohmvir.plugins.jenkinsaisynapse.api.input;

import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelInputType;
import dev.ohmvir.plugins.jenkinsaisynapse.utils.DecisionUtils;
import hudson.Extension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * A question that scores the state against an ordered rubric, answered by a
 * {@link dev.ohmvir.plugins.jenkinsaisynapse.api.output.DecisionScoreAnswerContent}.
 * The System One {@code score} question type.
 */
public class DecisionScoreQuestionContent extends ModelInput implements DecisionQuestion {
    private @Getter final String name;
    private @Getter final @Nullable String instructions;
    private final ArrayList<String> levels;

    /**
     * @param levels One description per level of the rubric, starting at level 0, at least two of them
     */
    @DataBoundConstructor
    public DecisionScoreQuestionContent(String name, @Nullable String instructions, List<String> levels) {
        if (levels == null || levels.size() < 2) {
            throw new IllegalArgumentException("A score question needs at least two levels");
        }
        this.name = DecisionUtils.requireQuestionName(name);
        this.instructions = instructions;
        this.levels = new ArrayList<>(levels);
    }

    /** @return One description per level, the description of level n at index n */
    public List<String> getLevels() {
        return Collections.unmodifiableList(levels);
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
            return "Decision Score Question Content";
        }
    }
}
