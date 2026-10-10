package io.ohmvir.plugins.jenkinsaisynapse.configuration;

import hudson.Extension;
import hudson.model.AdministrativeMonitor;
import io.ohmvir.plugins.jenkinsaisynapse.api.skills.SkillData;
import java.util.List;

/**
 * Warns administrators when skills failed to register because another skill already uses their name.
 */
@Extension
public class DuplicateSkillNameMonitor extends AdministrativeMonitor {

    @Override
    public String getDisplayName() {
        return "Duplicate Agent Skill names";
    }

    @Override
    public boolean isActivated() {
        return !SkillData.getDuplicateSkillRegistrations().isEmpty();
    }

    public List<SkillData.DuplicateSkillRegistration> getDuplicates() {
        return SkillData.getDuplicateSkillRegistrations();
    }
}
