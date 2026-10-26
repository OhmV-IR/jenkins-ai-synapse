package dev.ohmvir.plugins.jenkinsaisynapse.api.input;

import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelInputType;
import dev.ohmvir.plugins.jenkinsaisynapse.utils.DecisionUtils;
import hudson.Extension;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * A question that picks one of several named options, answered by a
 * {@link dev.ohmvir.plugins.jenkinsaisynapse.api.output.DecisionChoiceAnswerContent}.
 * The System One {@code choice} question type.
 */
public class DecisionChoiceQuestionContent extends ModelInput implements DecisionQuestion {
    private @Getter final String name;
    private @Getter final @Nullable String instructions;
    private final LinkedHashMap<String, String> criteria;

    /**
     * @param criteria Option labels mapped to their descriptions, at least two of them. A model answers with one of
     *     the labels.
     */
    @DataBoundConstructor
    public DecisionChoiceQuestionContent(String name, @Nullable String instructions, Map<String, String> criteria) {
        if (criteria == null || criteria.size() < 2) {
            throw new IllegalArgumentException("A choice question needs at least two options");
        }
        if (criteria.keySet().stream().anyMatch(label -> label == null || label.isBlank())) {
            throw new IllegalArgumentException("Choice question option labels must not be blank");
        }
        this.name = DecisionUtils.requireQuestionName(name);
        this.instructions = instructions;
        this.criteria = new LinkedHashMap<>(criteria);
    }

    /** @return Option labels mapped to their descriptions, in the order they were given */
    public Map<String, String> getCriteria() {
        return Collections.unmodifiableMap(criteria);
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
            return "Decision Choice Question Content";
        }
    }
}
