package io.ohmvir.plugins.jenkinscr.configuration.skills;

import hudson.Extension;
import hudson.model.Descriptor;
import io.ohmvir.plugins.jenkinscr.api.skills.SkillData;
import org.jspecify.annotations.NonNull;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.List;
import java.util.HashMap;

public class DeclaredSkillConfiguration extends SkillConfiguration {
    @Override
    protected SkillData generateSkill() {
        return new SkillData(skillName, skillDescription, skillLicense, skillCompatibility, skillMetadata, allowedTools, skillText, skillScripts, skillReferences, skillAssets);
    }

    @DataBoundConstructor
    public DeclaredSkillConfiguration(String skillId,
                                      String skillName,
                                      String skillDescription,
                                      String skillLicense,
                                      String skillCompatibility,
                                      String skillMetadata,
                                      List<String> allowedTools,
                                      String skillText,
                                      HashMap<String, String> skillScripts,
                                      HashMap<String, String> skillReferences,
                                      HashMap<String, String> skillAssets) throws Descriptor.FormException {
        super(skillId);
        this.skillName = skillName;
        this.skillDescription = skillDescription;
        this.skillLicense = skillLicense;
        this.skillCompatibility = skillCompatibility;
        this.skillMetadata = skillMetadata;
        this.allowedTools = allowedTools;
        this.skillText = skillText;
        this.skillScripts = skillScripts;
        this.skillReferences = skillReferences;
        this.skillAssets = skillAssets;
    }

    public String skillName;
    public String skillDescription;
    public String skillLicense;
    public String skillCompatibility;
    public String skillMetadata;
    public List<String> allowedTools;
    public String skillText;
    public HashMap<String,String> skillScripts;
    public HashMap<String,String> skillReferences;
    public HashMap<String,String> skillAssets;

    @Extension
    public static class DescriptorImpl extends SkillConfiguration.DescriptorImpl {
        @Override
        public @NonNull String getDisplayName() { return "Declared Skill"; }
    }
}

/*
skillName
skillDescription
skillLicense
skillCompatibility
skillMetadata
allowedTools
skillText
skillScripts
skillReferences
skillAssets
 */