package dev.ohmvir.plugins.jenkinsaisynapse.configuration;

import dev.ohmvir.plugins.jenkinsaisynapse.api.skills.SkillData;
import dev.ohmvir.plugins.jenkinsaisynapse.configuration.skills.SkillConfiguration;
import hudson.Extension;
import hudson.ExtensionList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import jenkins.model.GlobalConfiguration;
import org.jenkinsci.Symbol;
import org.jspecify.annotations.NonNull;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * Stores the agent skills. Edited from {@link SkillsManagementLink}, and can be set with configuration as code under
 * {@code unclassified.aiSkills}.
 */
@Extension
@Symbol("aiSkills")
public class SkillsConfiguration extends GlobalConfiguration {
    private static final String LEGACY_FILE = "skills.xml";

    private volatile List<SkillConfiguration> skills = new ArrayList<>();

    public SkillsConfiguration() {
        load();
        if (!getConfigFile().exists()) {
            LegacySkills legacy = LegacyConfigFile.read(LEGACY_FILE, SkillsManagementLink.class, LegacySkills.class);
            if (legacy != null) {
                if (legacy.skillConfigurations != null) {
                    skills = new ArrayList<>(legacy.skillConfigurations);
                }
                save();
                LegacyConfigFile.markMigrated(LEGACY_FILE);
            }
        }
    }

    public static SkillsConfiguration get() {
        return ExtensionList.lookupSingleton(SkillsConfiguration.class);
    }

    public List<SkillConfiguration> getSkills() {
        return Collections.unmodifiableList(skills);
    }

    @DataBoundSetter
    public void setSkills(List<SkillConfiguration> skills) {
        this.skills = skills == null ? new ArrayList<>() : new ArrayList<>(skills);
        save();
        SkillData.reload();
    }

    @Override
    public @NonNull String getDisplayName() {
        return "AI Agent Skills";
    }

    /** Shape of the file written by {@link SkillsManagementLink} before this class existed. */
    private static final class LegacySkills {
        private List<SkillConfiguration> skillConfigurations;
    }
}
