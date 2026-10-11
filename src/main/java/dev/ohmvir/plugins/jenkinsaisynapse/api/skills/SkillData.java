package dev.ohmvir.plugins.jenkinsaisynapse.api.skills;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import dev.ohmvir.plugins.jenkinsaisynapse.configuration.SkillsManagementLink;
import dev.ohmvir.plugins.jenkinsaisynapse.configuration.skills.SkillConfiguration;
import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.Describable;
import hudson.model.Descriptor;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class SkillData implements Describable<SkillData>, ExtensionPoint {
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

    private static final Logger LOGGER = Logger.getLogger(SkillData.class.getName());

    /**
     * A map of skill names to SkillData. Replaced wholesale on every reload so readers never see a partial registry.
     */
    private static volatile Map<String, SkillData> skillDataCache = Map.of();

    /**
     * Skills that were rejected during the last reload because another skill with the same name was already registered.
     */
    private static volatile List<DuplicateSkillRegistration> duplicateSkillRegistrations = List.of();

    public String getSkillMetadataPrettyYaml() {
        if (skillMetadata == null) {
            return null;
        }
        return skillMetadata.toPrettyString();
    }

    @Initializer(after = InitMilestone.PLUGINS_STARTED)
    public static void initializeSkillDataCache() {
        reloadSkills(SkillsManagementLink.get().getSkills());
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

    /**
     * Rebuilds the skill registry from the given configurations. When two skills share a name, the first one
     * registered wins and the later one is rejected, logged and recorded in {@link #getDuplicateSkillRegistrations()}.
     */
    public static synchronized void reloadSkills(List<SkillConfiguration> configs) {
        Map<String, SkillData> skills = new LinkedHashMap<>();
        Map<String, String> skillSources = new HashMap<>();
        List<DuplicateSkillRegistration> duplicates = new ArrayList<>();
        for (int i = 0; i < configs.size(); i++) {
            SkillConfiguration config = configs.get(i);
            String source =
                    "skill configuration #" + (i + 1) + " (" + config.getClass().getSimpleName() + ")";
            for (SkillData skillData : config.generateSkills()) {
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

    /**
     * @param skillName the name shared by both skills
     * @param rejectedSource where the skill that failed to register came from
     * @param registeredSource where the skill that kept the name came from
     */
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

    /**
     * The spec defines allowed-tools as a space-delimited string, but a YAML list is accepted too.
     */
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

    @Extension
    public static class DescriptorImpl extends Descriptor<SkillData> {
        @Override
        public @NonNull String getDisplayName() {
            return "Skill Data";
        }
    }
}
