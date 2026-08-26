package io.ohmvir.plugins.jenkinscr.configuration.skills;

import hudson.Extension;
import hudson.model.Descriptor;
import io.ohmvir.plugins.jenkinscr.api.skills.SkillData;
import org.jspecify.annotations.NonNull;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.Map;

public class BasicSkillConfiguration extends SkillConfiguration {
    @Override
    protected SkillData generateSkill() {
        return new SkillData(skillName, skillDescription, null, null, null, null, skillText, Map.of(), Map.of(), Map.of());
    }

    @DataBoundConstructor
    public BasicSkillConfiguration(String skillId, String skillText, String skillName, String skillDescription) throws Descriptor.FormException {
        super(skillId);
        this.skillName = skillName;
        this.skillDescription = skillDescription;
        this.skillText = skillText;
    }

    public String skillText;
    public String skillName;
    public String skillDescription;

    @Extension
    public static class DescriptorImpl extends SkillConfiguration.DescriptorImpl {
        @Override
        public @NonNull String getDisplayName() {
            return "Basic Skill";
        }
    }
}
