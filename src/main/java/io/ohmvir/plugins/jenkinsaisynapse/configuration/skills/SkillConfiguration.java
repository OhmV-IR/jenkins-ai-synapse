package io.ohmvir.plugins.jenkinsaisynapse.configuration.skills;

import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import io.ohmvir.plugins.jenkinsaisynapse.api.skills.SkillData;
import io.ohmvir.plugins.jenkinsaisynapse.configuration.SkillsManagementLink;

import java.util.List;
import java.util.Objects;
import lombok.Getter;
import org.kohsuke.stapler.QueryParameter;

public abstract class SkillConfiguration implements Describable<SkillConfiguration>, ExtensionPoint {

    public abstract List<SkillData> generateSkills();

    public abstract static class DescriptorImpl extends Descriptor<SkillConfiguration> {

    }
}
