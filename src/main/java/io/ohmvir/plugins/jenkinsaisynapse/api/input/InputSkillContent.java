package io.ohmvir.plugins.jenkinsaisynapse.api.input;

import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelInputType;
import io.ohmvir.plugins.jenkinsaisynapse.api.skills.SkillData;
import java.util.Set;
import lombok.Getter;

public class InputSkillContent extends ModelInput {
    private @Getter final SkillData skill;

    public InputSkillContent(SkillData skill) {
        this.skill = skill;
    }

    @Override
    public Set<ModelCapability> getRequiredCapabilities() {
        return Set.of(ModelCapability.SKILLS);
    }

    @Override
    public Set<ModelInputType> getRequiredInputTypes() {
        return Set.of();
    }
}
