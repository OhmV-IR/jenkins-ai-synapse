package io.ohmvir.plugins.jenkinsaisynapse.configuration;

import hudson.Extension;
import hudson.ExtensionList;
import hudson.util.FormValidation;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import lombok.Getter;
import org.jenkinsci.Symbol;
import org.jspecify.annotations.NonNull;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

/**
 * Retention settings for conversations stored in {@code JENKINS_HOME/conversations}. Conversations recorded by the
 * {@code aiRequest} step are stored with their build instead, and are discarded with it.
 */
@Extension
@Symbol("aiConversations")
public class ConversationsConfiguration extends GlobalConfiguration {
    public static final int DEFAULT_RETENTION_DAYS = 30;

    /** Number of days to keep stored conversations for. 0 keeps them forever. */
    private @Getter int retentionDays = DEFAULT_RETENTION_DAYS;

    public ConversationsConfiguration() {
        load();
    }

    public static ConversationsConfiguration get() {
        return ExtensionList.lookupSingleton(ConversationsConfiguration.class);
    }

    @DataBoundSetter
    public void setRetentionDays(int retentionDays) {
        if (retentionDays < 0) {
            throw new IllegalArgumentException("Retention days must not be negative");
        }
        this.retentionDays = retentionDays;
        save();
    }

    @POST
    public FormValidation doCheckRetentionDays(@QueryParameter int value) {
        Jenkins.get().checkPermission(Jenkins.ADMINISTER);
        if (value < 0) {
            return FormValidation.error("Retention days must not be negative");
        }
        if (value == 0) {
            return FormValidation.warning("Conversations will be kept forever");
        }
        return FormValidation.ok();
    }

    @Override
    public @NonNull String getDisplayName() {
        return "AI Conversations";
    }
}
