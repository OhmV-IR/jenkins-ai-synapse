package io.ohmvir.plugins.jenkinsaisynapse.configuration.skills;

import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import io.ohmvir.plugins.jenkinsaisynapse.api.skills.SkillData;
import io.ohmvir.plugins.jenkinsaisynapse.configuration.SkillsManagementLink;

import java.util.List;
import java.util.Objects;
import lombok.Getter;
import org.kohsuke.stapler.QueryParameter;

public abstract class SkillConfiguration implements Describable<SkillConfiguration>, ExtensionPoint {
    public SkillConfiguration(String configurationId) throws Descriptor.FormException {
        if (configurationId == null) {
            throw new Descriptor.FormException("Skill ID cannot be null", "configurationId");
        }
        if (configurationId.trim().isEmpty()) {
            throw new Descriptor.FormException("Skill ID cannot be empty", "configurationId");
        }
        if (SkillsManagementLink.get().getSkills().stream().anyMatch(skill -> skill.configurationId.equals(configurationId))) {
            throw new Descriptor.FormException("Skill IDs cannot be the same", "configurationId");
        }
        this.configurationId = configurationId;
    }

    protected abstract List<SkillData> generateSkills();

    private final @Getter String configurationId;

    public List<SkillData> getSkills() {
        var cacheData = SkillData.getSkillData(this);
        if (cacheData == null) {
            return generateSkills();
        }
        return cacheData;
    }

    public abstract static class DescriptorImpl extends Descriptor<SkillConfiguration> {
        public FormValidation doCheckSkillId(@QueryParameter String value) {
            if (value.trim().isEmpty()) {
                return FormValidation.error("Skill ID cannot be empty");
            }
            if (SkillsManagementLink.get().getSkills().stream()
                    .anyMatch(skill -> Objects.equals(skill.configurationId, value))) {
                return FormValidation.error("Skill ID cannot be the same as any other skill");
            }
            return FormValidation.ok();
        }
    }
}
