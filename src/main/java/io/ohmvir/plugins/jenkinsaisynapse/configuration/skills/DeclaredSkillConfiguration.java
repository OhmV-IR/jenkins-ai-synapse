package io.ohmvir.plugins.jenkinsaisynapse.configuration.skills;

import hudson.Extension;
import hudson.model.Descriptor;
import io.ohmvir.plugins.jenkinsaisynapse.api.skills.SkillData;
import java.util.*;

import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.kohsuke.stapler.DataBoundConstructor;

public class DeclaredSkillConfiguration extends SkillConfiguration {
    @Override
    public List<SkillData> generateSkills() {
        return List.of(new SkillData(
                skillName,
                skillDescription,
                skillLicense,
                skillCompatibility,
                skillMetadata,
                allowedTools,
                skillText,
                skillReferences));
    }

    @DataBoundConstructor
    public DeclaredSkillConfiguration(
            String skillName,
            String skillDescription,
            String skillLicense,
            String skillCompatibility,
            String skillMetadata,
            List<String> allowedTools,
            String skillText,
            Object referencePaths,
            Object fileContents)
            throws Descriptor.FormException {
        this.skillName = skillName;
        this.skillDescription = skillDescription;
        this.skillLicense = skillLicense;
        this.skillCompatibility = skillCompatibility;
        this.skillMetadata = skillMetadata;
        this.allowedTools = allowedTools;
        this.skillText = skillText;
        skillReferences = new HashMap<>();
        if (referencePaths instanceof String refPath && fileContents instanceof String fContent) {
            skillReferences.put(refPath, fContent);
        }
        if (referencePaths instanceof List rContents && fileContents instanceof List lContents) {
            for (int i = 0; i < rContents.size(); i++) {
                skillReferences.put(Objects.toString(rContents.get(i)), Objects.toString(lContents.get(i)));
            }
        }
    }

    public List<Map.Entry<String, String>> getEntriesAsList() {
        return new ArrayList<>(skillReferences.entrySet());
    }

    private @Getter final String skillName;
    private @Getter final String skillDescription;
    private @Getter final String skillLicense;
    private @Getter final String skillCompatibility;
    private @Getter final String skillMetadata;
    private @Getter final List<String> allowedTools;
    private @Getter final String skillText;
    private @Getter final HashMap<String, String> skillReferences;

    @Extension
    public static class DescriptorImpl extends SkillConfiguration.DescriptorImpl {
        @Override
        public @NonNull String getDisplayName() {
            return "Declared Skill";
        }
    }
}
