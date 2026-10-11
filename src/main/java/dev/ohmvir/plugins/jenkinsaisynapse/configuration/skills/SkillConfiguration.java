package dev.ohmvir.plugins.jenkinsaisynapse.configuration.skills;

import dev.ohmvir.plugins.jenkinsaisynapse.api.skills.SkillData;
import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import java.util.List;

public abstract class SkillConfiguration implements Describable<SkillConfiguration>, ExtensionPoint {

    /**
     * @return The skills described by this configuration. Never null: return an empty list if the skills
     * cannot be generated.
     */
    public abstract List<SkillData> generateSkills();

    public abstract static class DescriptorImpl extends Descriptor<SkillConfiguration> {}
}
