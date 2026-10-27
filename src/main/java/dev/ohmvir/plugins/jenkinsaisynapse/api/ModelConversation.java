package dev.ohmvir.plugins.jenkinsaisynapse.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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

    public static ModelConversation inMemory() {
        return new ModelConversation();
    }

    public UUID getConversationId() {
        return UUID.fromString(conversationId);
    }

    public Date getCreatedDate() {
        return new Date(createdAt);
    }

    public void addContent(ModelContent content) {
        addAllContent(List.of(content));
    }

    public synchronized void addAllContent(Collection<? extends ModelContent> content) {
        conversation.addAll(content);
        for (ModelContent item : content) {
            entries.add(ConversationEntry.from(item));
        }
    }

    public synchronized List<ModelContent> getConversation() {
        return Collections.unmodifiableList(new ArrayList<>(conversation));
    }

    public synchronized List<ConversationEntry> getEntries() {
        return Collections.unmodifiableList(new ArrayList<>(entries));
    }

    private Object readResolve() {
        conversation = new ArrayList<>();
        return this;
    }
}
