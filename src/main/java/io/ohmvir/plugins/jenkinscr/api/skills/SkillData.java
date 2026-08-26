package io.ohmvir.plugins.jenkinscr.api.skills;

import hudson.init.InitMilestone;
import hudson.init.Initializer;
import io.ohmvir.plugins.jenkinscr.configuration.AgenticCodeReviewSettings;
import io.ohmvir.plugins.jenkinscr.configuration.skills.SkillConfiguration;
import lombok.Getter;

import javax.annotation.Nullable;
import java.util.*;

public class SkillData {
    private @Getter String skillName;
    private @Getter String skillDescription;
    private @Getter @Nullable String skillLicense;
    private @Getter @Nullable String skillCompatibility;
    private @Getter @Nullable String skillMetadata;
    private @Getter @Nullable List<String> allowedTools;
    private @Getter String skillText;
    private HashMap<String, String> skillReferences;

    private static final HashMap<String, SkillData> skillDataCache = new HashMap<>();

    @Initializer(after = InitMilestone.PLUGINS_STARTED)
    public static void initializeSkillDataCache(){
        for(SkillConfiguration skillConfig : AgenticCodeReviewSettings.get().getSkills()){
            initializeSkillDataForConfig(skillConfig);
        }
    }

    public static @Nullable SkillData getSkillData(SkillConfiguration config){
        return skillDataCache.get(config.getSkillId());
    }

    public static void initializeSkillDataForConfig(SkillConfiguration config){
        skillDataCache.put(config.getSkillId(), config.getSkillData());
    }

    public Map<String, String> getSkillReferences(){
        return Collections.unmodifiableMap(skillReferences);
    }

    public SkillData(String skillName, String skillDescription, @Nullable String skillLicense,
                     @Nullable String skillCompatibility, @Nullable String skillMetadata, @Nullable List<String> allowedTools,
                     String skillText, Map<String, String> skillReferences){
        this.skillName = skillName;
        this.skillDescription = skillDescription;
        this.skillLicense = skillLicense;
        this.skillCompatibility = skillCompatibility;
        this.skillMetadata = skillMetadata;
        this.allowedTools = allowedTools;
        this.skillText = skillText;
        this.skillReferences = new HashMap<>();
        this.skillReferences.putAll(skillReferences);
    }

    /**
     *
     * @param skillDirectory A hashmap representing the files in the directory and their contents. Eg entry SKILL.md -> My skill text
     *                       and references/my-other-thing.md -> My other thing ...
     */
    public SkillData(Map<String, String> skillDirectory){
        // TODO
    }
}
