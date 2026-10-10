package io.ohmvir.plugins.jenkinsaisynapse.configuration;

import hudson.Extension;
import hudson.model.AsyncPeriodicWork;
import hudson.model.TaskListener;
import io.ohmvir.plugins.jenkinsaisynapse.api.ModelConversation;
import java.time.Duration;

/**
 * Deletes stored conversations older than {@link ConversationsConfiguration#getRetentionDays()}.
 */
@Extension
public class ConversationRetentionWork extends AsyncPeriodicWork {
    public ConversationRetentionWork() {
        super("AI conversation retention");
    }

    @Override
    public long getRecurrencePeriod() {
        return HOUR;
    }

    @Override
    protected void execute(TaskListener listener) {
        int retentionDays = ConversationsConfiguration.get().getRetentionDays();
        if (retentionDays <= 0) {
            return;
        }
        int deleted = ModelConversation.deleteOlderThan(Duration.ofDays(retentionDays));
        if (deleted > 0) {
            listener.getLogger().println("Deleted " + deleted + " conversations older than " + retentionDays + " days");
        }
    }
}
