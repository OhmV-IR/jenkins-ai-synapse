package io.ohmvir.plugins.jenkinsaisynapse.configuration.skills;

import hudson.Extension;
import hudson.model.Descriptor;
import io.ohmvir.plugins.jenkinsaisynapse.api.skills.SkillData;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.jspecify.annotations.NonNull;
import org.kohsuke.stapler.DataBoundConstructor;

public class BasicSkillFileConfiguration extends SkillConfiguration {
    public String fileContent;
    private final transient Logger logger;

    @DataBoundConstructor
    public BasicSkillFileConfiguration(String fileContent) throws Descriptor.FormException {
        this.fileContent = fileContent;
        this.logger = Logger.getLogger(BasicSkillFileConfiguration.class.getName());
    }

    @Override
    public List<SkillData> generateSkills() {
        try {
            return List.of(new SkillData(Map.of("SKILL.md", fileContent)));
        } catch (Exception e) {
            logger.severe("Failed to generate SkillData for BasicSkillFileConfiguration due to " + e.getMessage());
            return null;
        }
    }

    @Extension
    public static class DescriptorImpl extends SkillConfiguration.DescriptorImpl {
        @Override
        public @NonNull String getDisplayName() {
            return "Basic Skill File Configuration";
        }
    }
}
