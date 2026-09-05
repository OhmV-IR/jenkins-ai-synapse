package io.ohmvir.plugins.jenkinsaisynapse.configuration.skills;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import io.ohmvir.plugins.jenkinsaisynapse.api.skills.SkillData;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import io.ohmvir.plugins.jenkinsaisynapse.api.tools.ToolRegistry;
import io.ohmvir.plugins.jenkinsaisynapse.configuration.ToolsManagementLink;
import jenkins.model.Jenkins;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.interceptor.RequirePOST;

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
                    YAML_MAPPER.readTree(skillMetadata),
                    allowedTools.stream().map(AllowedTool::toolName).toList(),
                    skillText,
                    skillReferences.stream().collect(Collectors.toMap(
                            SkillReferenceEntry::relativeFilePath,
                            SkillReferenceEntry::fileContent
                    ))));
        } catch (Exception e){
            LOGGER.severe("Failed to generate SkillData for DeclaredSkillConfiguration due to " + e.getMessage());
            return List.of();
        }
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
        try{
            YAML_MAPPER.readTree(skillMetadata);
        } catch(Exception e){
            throw new Descriptor.FormException("Skill metadata is not a valid YAML object", "skillMetadata");
        }
        this.skillMetadata = skillMetadata;
        if(allowedTools.stream().anyMatch(toolName -> toolName.toolName.trim().isEmpty())){
            throw new Descriptor.FormException("Allowed tool tool name cannot be blank", "allowedTools");
        }
        this.allowedTools = allowedTools;
        this.skillText = skillText;
        if(skillReferences.stream().anyMatch(
                ref -> ref.fileContent.trim().isEmpty() || ref.relativeFilePath.trim().isEmpty()
        )){
            throw new Descriptor.FormException("Skill Reference cannot have blank file content or relative path", "skillReferences");
        }
        this.skillReferences = skillReferences;
    }

    private @Getter final String skillName;
    private @Getter final String skillDescription;
    private @Getter final String skillLicense;
    private @Getter final String skillCompatibility;
    private @Getter final String skillMetadata;
    private @Getter final List<AllowedTool> allowedTools;
    private @Getter final String skillText;
    private @Getter final List<SkillReferenceEntry> skillReferences;

    public record SkillReferenceEntry(@Getter String relativeFilePath, @Getter String fileContent) implements Describable<SkillReferenceEntry>, ExtensionPoint {
        @DataBoundConstructor
        public SkillReferenceEntry {
        }

        @Extension
        public static class DescriptorImpl extends Descriptor<SkillReferenceEntry> {
            @Override
            public @NonNull String getDisplayName() {
                return "Item";
            }

            public FormValidation doCheckRelativeFilePath(@QueryParameter String value) {
                if(value.trim().isEmpty()){
                    return FormValidation.error("Relative file path cannot be empty");
                }
                return FormValidation.ok();
            }

            public FormValidation doCheckFileContent(@QueryParameter String value) {
                if(value.trim().isEmpty()){
                    return FormValidation.error("File content cannot be empty");
                }
                return FormValidation.ok();
            }
        }
    }

    public record AllowedTool(@Getter String toolName) implements Describable<AllowedTool>, ExtensionPoint {
        @DataBoundConstructor
        public AllowedTool {

        }

        @Extension
        public static class DescriptorImpl extends Descriptor<AllowedTool> {
            @Override
            public @NonNull String getDisplayName() {
                return "Allowed Tool";
            }

            public ListBoxModel doFillToolNameItems() {
                ListBoxModel items = new ListBoxModel();
                ToolRegistry.getAllTools().forEach(
                        tool -> items.add(tool.getName())
                );
                return items;
            }

            public FormValidation doCheckToolName(@QueryParameter String value) {
                if(value.trim().isEmpty()){
                    return FormValidation.error("Tool name cannot be empty");
                }
                return FormValidation.ok();
            }
        }
    }

    @Extension
    public static class DescriptorImpl extends SkillConfiguration.DescriptorImpl {
        @Override
        public @NonNull String getDisplayName() {
            return "Declared Skill";
        }

        public FormValidation doCheckSkillMetadata(@QueryParameter String value){
            try{
                YAML_MAPPER.readTree(value);
                return FormValidation.ok();
            } catch(Exception e){
                return FormValidation.error("skillMetadata must be a valid YAML object: " + e.getMessage());
            }
        }
    }
}
