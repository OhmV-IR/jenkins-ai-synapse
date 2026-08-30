package io.ohmvir.plugins.jenkinscr.configuration.skills;

import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import io.ohmvir.plugins.jenkinscr.api.skills.SkillData;
import io.ohmvir.plugins.jenkinscr.configuration.AgenticCodeReviewSettings;
import io.ohmvir.plugins.jenkinscr.configuration.SkillsManagementLink;
import lombok.Getter;
import org.kohsuke.stapler.QueryParameter;

import java.util.Objects;

public abstract class SkillConfiguration implements Describable<SkillConfiguration>, ExtensionPoint {
    public SkillConfiguration(String skillId) throws Descriptor.FormException {
        if(skillId == null){
            throw new Descriptor.FormException("Skill ID cannot be null", "skillId");
        }
        if(skillId.trim().isEmpty()){
            throw new Descriptor.FormException("Skill ID cannot be empty", "skillId");
        }
        if(SkillsManagementLink.get().getSkills().stream().anyMatch(skill -> skill.skillId.equals(skillId))){
            throw new Descriptor.FormException("Skill IDs cannot be the same", "skillId");
        }
        this.skillId = skillId;
    }

    protected abstract SkillData generateSkill();
    private final @Getter String skillId;

    public SkillData getSkillData(){
        var cacheData = SkillData.getSkillData(this);
        if(cacheData == null){
            return generateSkill();
        }
        return cacheData;
    }

    public static abstract class DescriptorImpl extends Descriptor<SkillConfiguration> {
        public FormValidation doCheckSkillId(@QueryParameter String value) {
            if(value.trim().isEmpty()){
                return FormValidation.error("Skill ID cannot be empty");
            }
            if(SkillsManagementLink.get().getSkills().stream().anyMatch(skill -> Objects.equals(skill.skillId, value))){
                return FormValidation.error("Skill ID cannot be the same as any other skill");
            }
            return FormValidation.ok();
        }
    }
}