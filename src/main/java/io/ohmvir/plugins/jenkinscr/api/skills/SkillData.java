package io.ohmvir.plugins.jenkinscr.api.skills;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.Descriptor;
import io.ohmvir.plugins.jenkinscr.configuration.SkillsManagementLink;
import io.ohmvir.plugins.jenkinscr.configuration.skills.SkillConfiguration;
import lombok.Getter;
import javax.annotation.Nullable;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SkillData {
    private @Getter final String skillName;
    private @Getter final String skillDescription;
    private @Getter @Nullable final String skillLicense;
    private @Getter @Nullable final String skillCompatibility;
    private @Getter @Nullable final String skillMetadata;
    private @Getter @Nullable final List<String> allowedTools;
    private @Getter final String skillText;
    private final HashMap<String, String> skillReferences;

    private static final HashMap<String, SkillData> skillDataCache = new HashMap<>();

    @Initializer(after = InitMilestone.PLUGINS_STARTED)
    public static void initializeSkillDataCache(){
        for(SkillConfiguration skillConfig : SkillsManagementLink.get().getSkills()){
            initializeSkillDataForConfig(skillConfig);
        }
    }

    public static @Nullable SkillData getSkillData(SkillConfiguration config){
        return skillDataCache.get(config.getSkillId());
    }

    public static List<SkillData> getAllSkills(){
        return skillDataCache.values().stream().toList();
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

    private static final Pattern YAML_DATA_PATTERN = Pattern.compile("---(\\w+)---(\\w+)");
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    /**
     *
     * @param skillDirectory A hashmap representing the files in the directory and their contents. Eg entry SKILL.md -> My skill text
     *                       and references/my-other-thing.md -> My other thing ...
     */
    public SkillData(Map<String, String> skillDirectory) throws JsonProcessingException, Descriptor.FormException {
        String skillMdFileData = skillDirectory.get("SKILL.md");
        if(skillMdFileData == null){
            throw new Descriptor.FormException("skillMd file is null", "skillMd");
        }
        Matcher matcher = YAML_DATA_PATTERN.matcher(skillMdFileData);
        if(!matcher.find()){
            throw new Descriptor.FormException("skillMd file is invalid", "skillMd");
        }
        String yamlFileData = matcher.group(1);
        this.skillText = matcher.group(2);
        JsonNode root = YAML_MAPPER.readTree(yamlFileData);
        this.skillName = root.path("name").asText();
        this.skillDescription = root.path("description").asText();
        this.skillLicense = root.path("license").asText();
        this.skillCompatibility = root.path("compatibility").asText();
        this.skillMetadata = root.path("metadata").toString();
        this.allowedTools = Arrays.stream(root.path("allowed-tools").asText().split(" ")).toList();
        skillDirectory.remove("SKILL.md");
        this.skillReferences = new HashMap<>();
        this.skillReferences.putAll(skillDirectory);
    }
}
