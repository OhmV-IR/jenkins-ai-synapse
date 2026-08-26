package io.ohmvir.plugins.jenkinscr.configuration.skills;

import hudson.Extension;
import hudson.model.Descriptor;
import io.ohmvir.plugins.jenkinscr.api.skills.SkillData;
import org.jspecify.annotations.NonNull;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.*;

public class DeclaredSkillConfiguration extends SkillConfiguration {
    @Override
    protected SkillData generateSkill() {
        return new SkillData(skillName, skillDescription, skillLicense, skillCompatibility, skillMetadata, allowedTools, skillText, skillReferences);
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
                                      Object referencePaths,
                                      Object fileContents) throws Descriptor.FormException {
        super(skillId);
        this.skillName = skillName;
        this.skillDescription = skillDescription;
        this.skillLicense = skillLicense;
        this.skillCompatibility = skillCompatibility;
        this.skillMetadata = skillMetadata;
        this.allowedTools = allowedTools;
        this.skillText = skillText;
        skillReferences = new HashMap<>();
        if(referencePaths instanceof String refPath && fileContents instanceof String fContent){
            skillReferences.put(refPath, fContent);
        }
        if(referencePaths instanceof List rContents && fileContents instanceof List lContents){
            for(int i = 0; i < rContents.size(); i++){
                skillReferences.put(Objects.toString(rContents.get(i)), Objects.toString(lContents.get(i)));
            }
        }
    }

    public List<Map.Entry<String, String>> getEntriesAsList(){
        return new ArrayList<>(skillReferences.entrySet());
    }

    public String skillName;
    public String skillDescription;
    public String skillLicense;
    public String skillCompatibility;
    public String skillMetadata;
    public List<String> allowedTools;
    public String skillText;
    public HashMap<String,String> skillReferences;

    @Extension
    public static class DescriptorImpl extends SkillConfiguration.DescriptorImpl {
        @Override
        public @NonNull String getDisplayName() { return "Declared Skill"; }
    }
}