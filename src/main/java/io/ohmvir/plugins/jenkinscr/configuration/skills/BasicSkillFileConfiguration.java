package io.ohmvir.plugins.jenkinscr.configuration.skills;

import hudson.Extension;
import hudson.model.Descriptor;
import io.ohmvir.plugins.jenkinscr.api.skills.SkillData;
import org.jspecify.annotations.NonNull;
import org.kohsuke.stapler.DataBoundConstructor;
import java.util.Map;

public class BasicSkillFileConfiguration extends SkillConfiguration {
    public String fileContent;

    @DataBoundConstructor
    public BasicSkillFileConfiguration(String skillId, String fileContent) throws Descriptor.FormException {
        super(skillId);
        this.fileContent = fileContent;
    }

    @Override
    protected SkillData generateSkill() {
        return new SkillData(Map.of("SKILL.md", fileContent));
    }

    @Extension
    public static class DescriptorImpl extends SkillConfiguration.DescriptorImpl {
        @Override
        public @NonNull String getDisplayName() {
            return "Basic Skill File Configuration";
        }
    }
}
