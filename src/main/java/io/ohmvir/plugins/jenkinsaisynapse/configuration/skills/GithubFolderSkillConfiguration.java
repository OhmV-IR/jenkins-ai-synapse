package io.ohmvir.plugins.jenkinsaisynapse.configuration.skills;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import io.ohmvir.plugins.jenkinsaisynapse.api.skills.SkillData;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.net.URI;

public class GithubFolderSkillConfiguration extends SkillConfiguration {

    private static boolean isValidUrl(String value){
        if(value == null || value.trim().isEmpty()){
            return false;
        }
        try {
            URI uri = new URI(value);
            return uri.getScheme() != null && uri.getHost() != null;
        } catch (Exception e) {
            return false;
        }
    }
    @DataBoundConstructor
    public GithubFolderSkillConfiguration(String skillId, String folderPath) throws Descriptor.FormException {
        super(skillId);
        if(folderPath.trim().isEmpty()) {
            throw new Descriptor.FormException("Folder path is required", "folderPath");
        }
        if(isValidUrl(folderPath)){
            throw new Descriptor.FormException("Invalid folder path", "folderPath");
        }
        this.folderPath = folderPath;
    }

    private @Getter final String folderPath;

    @Override
    protected SkillData generateSkill() {
        return null;
    }

    @Extension
    public static class DescriptorImpl extends SkillConfiguration.DescriptorImpl {
        @Override
        public @NonNull String getDisplayName() {
            return "Github Folder Skill Configuration";
        }

        public FormValidation doCheckFolderPath(@QueryParameter String value) {
            if(value.trim().isEmpty()) {
                return FormValidation.error("Folder path is required");
            }
            if(isValidUrl(value)){
                return FormValidation.error("Invalid folder path");
            }
            return FormValidation.ok();
        }
    }
}
