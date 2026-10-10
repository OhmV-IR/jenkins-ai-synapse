package io.ohmvir.plugins.jenkinsaisynapse.configuration.skills;

import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import io.ohmvir.plugins.jenkinsaisynapse.api.skills.SkillData;
import java.util.List;

public abstract class SkillConfiguration implements Describable<SkillConfiguration>, ExtensionPoint {

    /**
     * @return The skills described by this configuration. Never null: return an empty list if the skills
     * cannot be generated.
     */
    public abstract List<SkillData> generateSkills();

    public abstract static class DescriptorImpl extends Descriptor<SkillConfiguration> {}
}
