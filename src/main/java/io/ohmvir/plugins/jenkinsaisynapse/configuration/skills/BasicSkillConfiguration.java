package io.ohmvir.plugins.jenkinsaisynapse.configuration.skills;

import hudson.Extension;
import hudson.model.Descriptor;
import io.ohmvir.plugins.jenkinsaisynapse.api.skills.SkillData;

import java.util.List;
import java.util.Map;
import org.jspecify.annotations.NonNull;
import org.kohsuke.stapler.DataBoundConstructor;

public class BasicSkillConfiguration extends SkillConfiguration {
    @Override
    public List<SkillData> generateSkills() {
        return List.of(new SkillData(skillName, skillDescription, null, null, null, null, skillText, Map.of()));
    }

    @DataBoundConstructor
    public BasicSkillConfiguration(String skillText, String skillName, String skillDescription)
            throws Descriptor.FormException {
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
