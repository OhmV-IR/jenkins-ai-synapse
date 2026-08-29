package io.ohmvir.plugins.jenkinscr.configuration.skills;

import com.fasterxml.jackson.core.JsonProcessingException;
import hudson.Extension;
import hudson.model.Descriptor;
import io.ohmvir.plugins.jenkinscr.api.skills.SkillData;
import org.jspecify.annotations.NonNull;
import org.kohsuke.stapler.DataBoundConstructor;
import java.util.Map;
import java.util.logging.Logger;

public class BasicSkillFileConfiguration extends SkillConfiguration {
    public String fileContent;
    private transient final Logger logger;

    @DataBoundConstructor
    public BasicSkillFileConfiguration(String skillId, String fileContent) throws Descriptor.FormException {
        super(skillId);
        this.fileContent = fileContent;
        this.logger = Logger.getLogger(BasicSkillFileConfiguration.class.getName());
    }

    @Override
    protected SkillData generateSkill() {
        try {
            return new SkillData(Map.of("SKILL.md", fileContent));
        } catch(Exception e){
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
