package dev.ohmvir.plugins.jenkinsaisynapse.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

/**
 * The content exchanged with a model. Conversations are kept with the Jenkins build that recorded them.
 */
public class ModelConversation {
    private final String conversationId = UUID.randomUUID().toString();
    private @Getter long createdAt = System.currentTimeMillis();
    private @Getter @Setter @Nullable String modelId;
    private final List<ConversationEntry> entries = new ArrayList<>();
    private transient List<ModelContent> conversation = new ArrayList<>();
    private transient List<String> secrets = List.of();

    public static ModelConversation inMemory() {
        return new ModelConversation();
    }

    public UUID getConversationId() {
        return UUID.fromString(conversationId);
    }

    public Date getCreatedDate() {
        return new Date(createdAt);
    }

    /**
     * Replaces these values with asterisks in the entries added from now on, so that secrets used by the build are not
     * stored in the build or shown on the AI Conversations page.
     */
    public synchronized void maskSecrets(Collection<String> values) {
        // Longest first, so that a secret containing another one is masked as a whole.
        secrets = values.stream()
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .sorted(Comparator.comparingInt(String::length).reversed())
                .toList();
    }

    public void addContent(ModelContent content) {
        addAllContent(List.of(content));
    }

    public synchronized void addAllContent(Collection<? extends ModelContent> content) {
        conversation.addAll(content);
        for (ModelContent item : content) {
            entries.add(ConversationEntry.from(item, secrets));
        }
    }

    /**
     * Ends the conversation of a request that failed with an entry that gives the reason.
     */
    public synchronized void addFailure(String reason) {
        entries.add(ConversationEntry.failure(reason));
    }

    public synchronized List<ModelContent> getConversation() {
        return Collections.unmodifiableList(new ArrayList<>(conversation));
    }

    public synchronized List<ConversationEntry> getEntries() {
        return Collections.unmodifiableList(new ArrayList<>(entries));
    }

    private Object readResolve() {
        conversation = new ArrayList<>();
        secrets = List.of();
        return this;
    }
}
