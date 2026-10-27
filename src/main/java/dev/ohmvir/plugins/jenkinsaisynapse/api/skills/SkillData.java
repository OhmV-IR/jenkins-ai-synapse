package dev.ohmvir.plugins.jenkinsaisynapse.api.skills;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.Descriptor;
import dev.ohmvir.plugins.jenkinsaisynapse.configuration.SkillsConfiguration;
import dev.ohmvir.plugins.jenkinsaisynapse.configuration.skills.SkillConfiguration;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jenkins.util.Timer;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

/**
 * A skill a model can use. Plain data: it can be built and read without a running Jenkins.
 */
public class SkillData {
    private @Getter final String skillName;
    private @Getter @Nullable final String skillDescription;
    private @Getter @Nullable final String skillLicense;
    private @Getter @Nullable final String skillCompatibility;
    private @Getter @Nullable final JsonNode skillMetadata;
    private @Getter @Nullable final List<String> allowedTools;
    private @Getter final String skillText;
    private final HashMap<String, String> skillReferences;

    public Set<Map.Entry<String, String>> getSkillReferencesEntries() {
        return skillReferences.entrySet();
    }

    /**
     * A map of skill names to SkillData
     */
    private static final Logger LOGGER = Logger.getLogger(SkillData.class.getName());
    private static volatile Map<String, SkillData> skillDataCache = Map.of();
    private static volatile List<DuplicateSkillRegistration> duplicateSkillRegistrations = List.of();

    public String getSkillMetadataPrettyYaml() {
        if (skillMetadata == null) {
            return null;
        }
        return skillMetadata.toPrettyString();
    }

    @Initializer(after = InitMilestone.PLUGINS_STARTED, fatal = false)
    public static void initializeSkillDataCache() {
        Timer.get().execute(() -> {
            try {
                reload();
            } catch (RuntimeException e) {
                LOGGER.log(Level.WARNING, "Failed to load skills", e);
            }
        });
    }

    public static synchronized void reload() {
        reloadSkills(SkillsConfiguration.get().getSkills());
    }

    public static SkillData getSkillData(String skillName) {
        return skillDataCache.get(skillName);
    }

    public static List<SkillData> getAllSkills() {
        return List.copyOf(skillDataCache.values());
    }

    public static List<DuplicateSkillRegistration> getDuplicateSkillRegistrations() {
        return duplicateSkillRegistrations;
    }

    public static synchronized void reloadSkills(List<SkillConfiguration> configs) {
        Map<String, SkillData> skills = new LinkedHashMap<>();
        Map<String, String> skillSources = new HashMap<>();
        List<DuplicateSkillRegistration> duplicates = new ArrayList<>();
        for (int i = 0; i < configs.size(); i++) {
            SkillConfiguration config = configs.get(i);
            String source =
                    "skill configuration #" + (i + 1) + " (" + config.getClass().getSimpleName() + ")";
            List<SkillData> generated;
            try {
                generated = config.generateSkills();
            } catch (RuntimeException e) {
                LOGGER.log(Level.WARNING, "Failed to generate skills from " + source, e);
                continue;
            }
            if (generated == null) {
                continue;
            }
            for (SkillData skillData : generated) {
                String name = skillData.getSkillName();
                if (name == null || name.isBlank()) {
                    LOGGER.warning("Failed to register a skill from " + source + " because it has no name");
                    continue;
                }
                if (skills.containsKey(name)) {
                    String existingSource = skillSources.get(name);
                    LOGGER.warning("Failed to register skill '" + name + "' from " + source
                            + " because a skill with the same name is already registered from " + existingSource);
                    duplicates.add(new DuplicateSkillRegistration(name, source, existingSource));
                    continue;
                }
                skills.put(name, skillData);
                skillSources.put(name, source);
            }
        }
        skillDataCache = Collections.unmodifiableMap(skills);
        duplicateSkillRegistrations = List.copyOf(duplicates);
    }

    public record DuplicateSkillRegistration(String skillName, String rejectedSource, String registeredSource) {}

    public Map<String, String> getSkillReferences() {
        return Collections.unmodifiableMap(skillReferences);
    }

    public SkillData(
            String skillName,
            @Nullable String skillDescription,
            @Nullable String skillLicense,
            @Nullable String skillCompatibility,
            @Nullable JsonNode skillMetadata,
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

    /**
     * The frontmatter must start the file (after an optional byte order mark) and both {@code ---} delimiters must be
     * on their own lines. Group 1 is the YAML (absent when the block is empty), group 2 is the body (absent when the
     * file ends right after the closing delimiter).
     */
    private static final Pattern YAML_DATA_PATTERN =
            Pattern.compile("\\A\\uFEFF?---[ \\t]*\\R(?:(.*?)\\R)??---[ \\t]*(?:\\R\\s*(.*))?\\z", Pattern.DOTALL);
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
        String yamlFileData = matcher.group(1) == null ? "" : matcher.group(1);
        this.skillText = matcher.group(2) == null ? "" : matcher.group(2);
        JsonNode root = YAML_MAPPER.readTree(yamlFileData);
        if (root == null || !root.isObject()) {
            throw new Descriptor.FormException("skillMd frontmatter must be a YAML mapping", "skillMd");
        }
        this.skillName = textOrNull(root.get("name"));
        if (this.skillName == null || this.skillName.isBlank()) {
            throw new Descriptor.FormException("skillMd frontmatter is missing the required 'name' field", "skillMd");
        }
        this.skillDescription = textOrNull(root.get("description"));
        this.skillLicense = textOrNull(root.get("license"));
        this.skillCompatibility = textOrNull(root.get("compatibility"));
        JsonNode metadata = root.get("metadata");
        this.skillMetadata = metadata == null || metadata.isNull() ? null : metadata;
        this.allowedTools = parseAllowedTools(root.get("allowed-tools"));
        this.skillReferences = new HashMap<>();
        this.skillReferences.putAll(skillDirectory);
        this.skillReferences.remove("SKILL.md");
    }

    private static @Nullable String textOrNull(@Nullable JsonNode node) {
        return node == null || node.isNull() ? null : node.asText();
    }

    private static @Nullable List<String> parseAllowedTools(@Nullable JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        List<String> tools = new ArrayList<>();
        if (node.isArray()) {
            node.forEach(tool -> tools.add(tool.asText().trim()));
        } else {
            tools.addAll(Arrays.asList(node.asText().trim().split("\\s+")));
        }
        return tools.stream().filter(tool -> !tool.isEmpty()).toList();
    }
}
