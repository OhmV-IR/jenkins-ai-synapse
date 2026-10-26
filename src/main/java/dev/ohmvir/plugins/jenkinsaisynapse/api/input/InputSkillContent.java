package dev.ohmvir.plugins.jenkinsaisynapse.api.input;

import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelInputType;
import dev.ohmvir.plugins.jenkinsaisynapse.api.skills.ReadSkillFileTool;
import dev.ohmvir.plugins.jenkinsaisynapse.api.skills.SkillData;
import hudson.Extension;
import java.util.Set;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Attaches a skill to a request. Skills don't need any provider support: when the request executes, every attached
 * skill is listed in the system prompt and the built-in {@link ReadSkillFileTool} is attached so the model can load
 * the skill's files on demand. Clients therefore never receive this input directly.
 */
public class InputSkillContent extends ModelInput {
    private @Getter final SkillData skill;

    @DataBoundConstructor
    public InputSkillContent(SkillData skill) {
        this.skill = skill;
    }

    @Extension
    public static class DescriptorImpl extends ModelInputDescriptor {
        @Override
        public Set<ModelCapability> getRequiredCapabilities() {
            return Set.of(ModelCapability.TOOLS, ModelCapability.ADJUSTABLE_SYSTEM_PROMPT);
        }

        @Override
        public Set<ModelInputType> getRequiredInputTypes() {
            return Set.of(ModelInputType.TEXT);
        }

        @Override
        public @NonNull String getDisplayName() {
            return "Input Skill Content";
        }
    }
}
