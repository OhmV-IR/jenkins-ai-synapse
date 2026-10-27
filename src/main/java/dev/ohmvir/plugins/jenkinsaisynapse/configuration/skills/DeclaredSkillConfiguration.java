package dev.ohmvir.plugins.jenkinsaisynapse.configuration.skills;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import dev.ohmvir.plugins.jenkinsaisynapse.api.skills.ReadSkillFileTool;
import dev.ohmvir.plugins.jenkinsaisynapse.api.skills.SkillData;
import dev.ohmvir.plugins.jenkinsaisynapse.api.tools.ToolRegistry;
import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import jenkins.model.Jenkins;
import lombok.Getter;
import org.jenkinsci.Symbol;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

public class DeclaredSkillConfiguration extends SkillConfiguration {
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());
    private static final Logger LOGGER = Logger.getLogger(DeclaredSkillConfiguration.class.getName());

    @Override
    public List<SkillData> generateSkills() {
        try {
            return List.of(new SkillData(
                    skillName,
                    skillDescription,
                    skillLicense,
                    skillCompatibility,
                    parseSkillMetadata(skillMetadata),
                    allowedTools.stream().map(AllowedTool::getToolName).toList(),
                    skillText,
                    skillReferences.stream()
                            .collect(Collectors.toMap(
                                    SkillReferenceEntry::getRelativeFilePath,
                                    SkillReferenceEntry::getFileContent,
                                    (first, second) -> second,
                                    LinkedHashMap::new))));
        } catch (Exception e) {
            LOGGER.severe("Failed to generate SkillData for DeclaredSkillConfiguration due to " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Parses the optional skill metadata. Null, blank and comment-only YAML mean the skill has no metadata (null);
     * anything else must be a YAML mapping.
     *
     * @throws Descriptor.FormException if the metadata is not valid YAML or is not a mapping
     */
    static @Nullable JsonNode parseSkillMetadata(@Nullable String skillMetadata) throws Descriptor.FormException {
        if (skillMetadata == null || skillMetadata.isBlank()) {
            return null;
        }
        JsonNode node;
        try {
            node = YAML_MAPPER.readTree(skillMetadata);
        } catch (JsonProcessingException e) {
            throw new Descriptor.FormException(
                    "Skill metadata is not valid YAML: " + e.getOriginalMessage(), e, "skillMetadata");
        }
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (!node.isObject()) {
            throw new Descriptor.FormException(
                    "Skill metadata must be a YAML mapping of keys to values, not a " + node.getNodeType(),
                    "skillMetadata");
        }
        return node;
    }

    @DataBoundConstructor
    public DeclaredSkillConfiguration(
            String skillName,
            String skillDescription,
            String skillLicense,
            String skillCompatibility,
            String skillMetadata,
            List<AllowedTool> allowedTools,
            String skillText,
            List<SkillReferenceEntry> skillReferences)
            throws Descriptor.FormException {
        this.skillName = skillName;
        this.skillDescription = skillDescription;
        this.skillLicense = skillLicense;
        this.skillCompatibility = skillCompatibility;
        parseSkillMetadata(skillMetadata);
        this.skillMetadata = skillMetadata;
        if (allowedTools != null
                && allowedTools.stream()
                        .anyMatch(toolName -> toolName.toolName.trim().isEmpty())) {
            throw new Descriptor.FormException("Allowed tool tool name cannot be blank", "allowedTools");
        }
        this.allowedTools = allowedTools == null ? new ArrayList<>() : allowedTools;
        this.skillText = skillText;
        if (skillReferences != null
                && skillReferences.stream()
                        .anyMatch(ref -> ref.fileContent.trim().isEmpty()
                                || ref.relativeFilePath.trim().isEmpty())) {
            throw new Descriptor.FormException(
                    "Skill Reference cannot have blank file content or relative path", "skillReferences");
        }
        if (skillReferences != null) {
            Set<String> seenPaths = new HashSet<>();
            for (SkillReferenceEntry ref : skillReferences) {
                if (ref.relativeFilePath.trim().equals(ReadSkillFileTool.SKILL_FILE)) {
                    throw new Descriptor.FormException(
                            "Skill Reference cannot be named " + ReadSkillFileTool.SKILL_FILE
                                    + " because it is generated from the skill itself",
                            "skillReferences");
                }
                if (!seenPaths.add(ref.relativeFilePath)) {
                    throw new Descriptor.FormException(
                            "Skill References must have unique relative paths, but '" + ref.relativeFilePath
                                    + "' is used more than once",
                            "skillReferences");
                }
            }
        }
        this.skillReferences = skillReferences == null ? new ArrayList<>() : skillReferences;
    }

    private @Getter final String skillName;
    private @Getter final String skillDescription;
    private @Getter final String skillLicense;
    private @Getter final String skillCompatibility;
    private @Getter final String skillMetadata;
    private @Getter final List<AllowedTool> allowedTools;
    private @Getter final String skillText;
    private @Getter final List<SkillReferenceEntry> skillReferences;

    public static class SkillReferenceEntry implements Describable<SkillReferenceEntry>, ExtensionPoint {
        private @Getter final String relativeFilePath;
        private @Getter final String fileContent;

        @DataBoundConstructor
        public SkillReferenceEntry(String relativeFilePath, String fileContent) throws Descriptor.FormException {
            if (relativeFilePath.trim().isEmpty()) {
                throw new Descriptor.FormException("Relative file path cannot be empty", "relativeFilePath");
            }
            this.relativeFilePath = relativeFilePath;
            if (fileContent.trim().isEmpty()) {
                throw new Descriptor.FormException("File content cannot be empty", "fileContent");
            }
            this.fileContent = fileContent;
        }

        @Extension
        public static class DescriptorImpl extends Descriptor<SkillReferenceEntry> {
            @Override
            public @NonNull String getDisplayName() {
                return "Item";
            }

            @POST
            public FormValidation doCheckRelativeFilePath(@QueryParameter String value) {
                Jenkins.get().checkPermission(Jenkins.ADMINISTER);
                if (value == null || value.trim().isEmpty()) {
                    return FormValidation.error("Relative file path cannot be empty");
                }
                return FormValidation.ok();
            }

            @POST
            public FormValidation doCheckFileContent(@QueryParameter String value) {
                Jenkins.get().checkPermission(Jenkins.ADMINISTER);
                if (value == null || value.trim().isEmpty()) {
                    return FormValidation.error("File content cannot be empty");
                }
                return FormValidation.ok();
            }
        }
    }

    public static class AllowedTool implements Describable<AllowedTool>, ExtensionPoint {
        private @Getter final String toolName;

        @DataBoundConstructor
        public AllowedTool(String toolName) throws Descriptor.FormException {
            if (toolName.trim().isEmpty()) {
                throw new Descriptor.FormException("Tool name cannot be empty", "toolName");
            }
            this.toolName = toolName;
        }

        @Extension
        public static class DescriptorImpl extends Descriptor<AllowedTool> {
            @Override
            public @NonNull String getDisplayName() {
                return "Allowed Tool";
            }

            @POST
            public ListBoxModel doFillToolNameItems() {
                Jenkins.get().checkPermission(Jenkins.ADMINISTER);
                ListBoxModel items = new ListBoxModel();
                ToolRegistry.getAllTools().forEach(tool -> items.add(tool.getName()));
                return items;
            }

            @POST
            public FormValidation doCheckToolName(@QueryParameter String value) {
                Jenkins.get().checkPermission(Jenkins.ADMINISTER);
                if (value == null || value.trim().isEmpty()) {
                    return FormValidation.error("Tool name cannot be empty");
                }
                return FormValidation.ok();
            }
        }
    }

    @Extension
    @Symbol("declaredSkill")
    public static class DescriptorImpl extends SkillConfiguration.DescriptorImpl {
        @Override
        public @NonNull String getDisplayName() {
            return "Declared Skill";
        }

        @POST
        public FormValidation doCheckSkillMetadata(@QueryParameter String value) {
            Jenkins.get().checkPermission(Jenkins.ADMINISTER);
            try {
                parseSkillMetadata(value);
                return FormValidation.ok();
            } catch (Descriptor.FormException e) {
                return FormValidation.error(e.getMessage());
            }
        }
    }
}
