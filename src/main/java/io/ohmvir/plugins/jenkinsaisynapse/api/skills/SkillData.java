package io.ohmvir.plugins.jenkinsaisynapse.api.skills;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.Describable;
import hudson.model.Descriptor;
import io.ohmvir.plugins.jenkinsaisynapse.configuration.SkillsManagementLink;
import io.ohmvir.plugins.jenkinsaisynapse.configuration.skills.SkillConfiguration;
import io.ohmvir.plugins.jenkinsaisynapse.utils.SnapshotCache;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class SkillData implements Describable<SkillData>, ExtensionPoint {
    private static final Logger LOGGER = Logger.getLogger(SkillData.class.getName());
    private @Getter final String skillName;
    private @Getter final String skillDescription;
    private @Getter @Nullable final String skillLicense;
    private @Getter @Nullable final String skillCompatibility;
    private @Getter final JsonNode skillMetadata;
    private @Getter @Nullable final List<String> allowedTools;
    private @Getter final String skillText;
    private final HashMap<String, String> skillReferences;

    public Set<Map.Entry<String, String>> getSkillReferencesEntries() {
        return skillReferences.entrySet();
    }

    /**
     * A map of skill names to SkillData
     */
    private static final SnapshotCache<SkillData> skillDataCache =
            new SnapshotCache<>("skill data", SkillData::generateAllSkills);

    public String getSkillMetadataPrettyYaml() {
        if (skillMetadata == null) {
            return null;
        }
        return skillMetadata.toPrettyString();
    }

    /**
     * Generates skills in the background, since some skill configurations fetch them over the network.
     */
    @Initializer(after = InitMilestone.PLUGINS_STARTED, fatal = false)
    public static void initializeSkillDataCache() {
        skillDataCache.reloadInBackground();
    }

    /**
     * Rebuilds the skills from the current skill configurations and swaps them in.
     * Call this after the skill configurations change.
     */
    public static void reload() {
        skillDataCache.reload();
    }

    private static Map<String, SkillData> generateAllSkills() {
        Map<String, SkillData> skills = new LinkedHashMap<>();
        for (SkillConfiguration config : SkillsManagementLink.get().getSkills()) {
            List<SkillData> generated;
            try {
                generated = config.generateSkills();
            } catch (RuntimeException e) {
                LOGGER.log(
                        Level.WARNING,
                        "Failed to generate skills for " + config.getClass().getName(),
                        e);
                continue;
            }
            if (generated == null) {
                continue;
            }
            generated.forEach(skillData -> skills.put(skillData.getSkillName(), skillData));
        }
        return skills;
    }

    public static @Nullable SkillData getSkillData(String skillName) {
        return skillDataCache.get().get(skillName);
    }

    public static List<SkillData> getAllSkills() {
        return List.copyOf(skillDataCache.get().values());
    }

    public Map<String, String> getSkillReferences() {
        return Collections.unmodifiableMap(skillReferences);
    }

    public SkillData(
            String skillName,
            String skillDescription,
            @Nullable String skillLicense,
            @Nullable String skillCompatibility,
            JsonNode skillMetadata,
            @Nullable List<String> allowedTools,
            String skillText,
            Map<String, String> skillReferences) {
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

    private static final Pattern YAML_DATA_PATTERN =
            Pattern.compile("---\\s*(.*?)\\s*---\\s*(.*)\\s*", Pattern.DOTALL | Pattern.MULTILINE);
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    /**
     *
     * @param skillDirectory A hashmap representing the files in the directory and their contents. Eg entry SKILL.md -> My skill text
     *                       and references/my-other-thing.md -> My other thing ...
     */
    public SkillData(Map<String, String> skillDirectory) throws JsonProcessingException, Descriptor.FormException {
        String skillMdFileData = skillDirectory.get("SKILL.md");
        if (skillMdFileData == null) {
            throw new Descriptor.FormException("skillMd file is null", "skillMd");
        }
        Matcher matcher = YAML_DATA_PATTERN.matcher(skillMdFileData);
        if (!matcher.find()) {
            throw new Descriptor.FormException("skillMd file is invalid", "skillMd");
        }
        String yamlFileData = matcher.group(1);
        this.skillText = matcher.group(2);
        JsonNode root = YAML_MAPPER.readTree(yamlFileData);
        this.skillName = root.path("name").asText();
        this.skillDescription = root.path("description").asText();
        this.skillLicense = root.path("license").asText();
        this.skillCompatibility = root.path("compatibility").asText();
        this.skillMetadata = root.path("metadata");
        this.allowedTools =
                Arrays.stream(root.path("allowed-tools").asText().split(" ")).toList();
        this.skillReferences = new HashMap<>();
        this.skillReferences.putAll(skillDirectory);
        this.skillReferences.remove("SKILL.md");
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<SkillData> {
        @Override
        public @NonNull String getDisplayName() {
            return "Skill Data";
        }
    }
}
