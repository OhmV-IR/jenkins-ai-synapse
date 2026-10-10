package io.ohmvir.plugins.jenkinsaisynapse.configuration;

import hudson.Extension;
import hudson.model.ManagementLink;
import hudson.security.Permission;
import io.ohmvir.plugins.jenkinsaisynapse.api.ModelConversation;
import java.util.List;
import jenkins.model.Jenkins;
import org.jspecify.annotations.NonNull;

/**
 * Lists the conversations stored in {@code JENKINS_HOME/conversations}. Conversations recorded by builds are shown on
 * the build instead.
 */
@Extension
public class ConversationHistoryManagementLink extends ManagementLink {
    public List<ModelConversation> getConversations() {
        return ModelConversation.loadAll();
    }

    public int getRetentionDays() {
        return ConversationsConfiguration.get().getRetentionDays();
    }

    @Override
    public String getIconFileName() {
        return "symbol-settings-outline plugin-ionicons-api";
    }

    @Override
    public String getDisplayName() {
        return "Past Conversations";
    }

    @Override
    public String getUrlName() {
        return "conversations";
    }

    @Override
    public String getDescription() {
        return "View all conversation history for debugging purposes";
    }

    @Override
    public @NonNull Category getCategory() {
        return Category.TROUBLESHOOTING;
    }

    @Override
    public @NonNull Permission getRequiredPermission() {
        return Jenkins.ADMINISTER;
    }
}
