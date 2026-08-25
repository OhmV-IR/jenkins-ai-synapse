package io.ohmvir.plugins.jenkinscr.api.skills;

import hudson.Extension;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import io.ohmvir.plugins.jenkinscr.configuration.AgenticCodeReviewSettings;
import io.ohmvir.plugins.jenkinscr.configuration.skills.SkillConfiguration;
import lombok.Getter;

import javax.annotation.Nullable;
import java.util.*;

@Extension
public class SkillData {
    private @Getter String skillName;
    private @Getter String skillDescription;
    private @Getter @Nullable String skillLicense;
    private @Getter @Nullable String skillCompatibility;
    private @Getter @Nullable String skillMetadata;
    private @Getter @Nullable List<String> allowedTools;
    private @Getter String skillText;
    private HashMap<String, String> skillScripts;
    private HashMap<String, String> skillReferences;
    private HashMap<String, String> skillAssets;

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

    public Map<String, String> getSkillScripts(){
        return Collections.unmodifiableMap(skillScripts);
    }

    public Map<String, String> getSkillReferences(){
        return Collections.unmodifiableMap(skillReferences);
    }

    public Map<String, String> getSkillAssets(){
        return Collections.unmodifiableMap(skillAssets);
    }

    public SkillData(String skillMdText, HashMap<String, String> skillScripts, HashMap<String, String> skillReferences, HashMap<String, String> skillAssets){
        // TODO
    }

    public SkillData(String skillName, String skillDescription, @Nullable String skillLicense,
                     @Nullable String skillCompatibility, @Nullable String skillMetadata, @Nullable List<String> allowedTools,
                     String skillText, Map<String, String> skillScripts, Map<String, String> skillReferences, Map<String, String> skillAssets){
        this.skillName = skillName;
        this.skillDescription = skillDescription;
        this.skillLicense = skillLicense;
        this.skillCompatibility = skillCompatibility;
        this.skillMetadata = skillMetadata;
        this.allowedTools = allowedTools;
        this.skillText = skillText;
        this.skillScripts = new HashMap<>();
        this.skillScripts.putAll(skillScripts);
        this.skillReferences = new HashMap<>();
        this.skillReferences.putAll(skillReferences);
        this.skillAssets = new HashMap<>();
        this.skillAssets.putAll(skillAssets);
    }

    public SkillData(String skillZipPath){
        // TODO
    }
}
