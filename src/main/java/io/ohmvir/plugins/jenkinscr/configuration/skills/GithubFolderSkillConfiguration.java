package io.ohmvir.plugins.jenkinscr.configuration.skills;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import io.ohmvir.plugins.jenkinscr.api.skills.SkillData;
import jenkins.org.apache.commons.validator.routines.UrlValidator;
import org.jspecify.annotations.NonNull;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

public class GithubFolderSkillConfiguration extends SkillConfiguration {
    @DataBoundConstructor
    public GithubFolderSkillConfiguration(String skillId, String folderPath) throws Descriptor.FormException {
        super(skillId);
        if(folderPath.trim().isEmpty()) {
            throw new Descriptor.FormException("Folder path is required", "folderPath");
        }
        var validator = new UrlValidator(new String[]{"https"});
        if(!validator.isValid(folderPath)){
            throw new Descriptor.FormException("Invalid folder path", "folderPath");
        }
    }

    public String folderPath;

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
            var validator = new UrlValidator(new String[]{"https"});
            if(!validator.isValid(value)){
                return FormValidation.error("Invalid folder path");
            }
            return FormValidation.ok();
        }
    }
}
