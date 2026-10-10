package io.ohmvir.plugins.jenkinsaisynapse.api;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import hudson.util.AtomicFileWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.Jenkins;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

/**
 * The content exchanged with a model over one or more requests.
 * <p>
 * The content objects themselves are only kept in memory, for passing back to a model. What is stored is a list of
 * {@link ConversationEntry} records. Conversations created with {@link #ModelConversation()} are stored as JSON in
 * {@code JENKINS_HOME/conversations} and removed after the retention period set in
 * {@link io.ohmvir.plugins.jenkinsaisynapse.configuration.ConversationsConfiguration}. Conversations created with
 * {@link #inMemory()} are not written anywhere on their own; the {@code aiRequest} step uses them and stores them with
 * the build through {@link ModelConversationsAction}.
 */
public class ModelConversation {
    private static final Logger LOGGER = Logger.getLogger(ModelConversation.class.getName());
    private static final ObjectMapper MAPPER =
            new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final String DIRECTORY_NAME = "conversations";
    private static final String EXTENSION = ".json";
    /** Extension of the XStream files written by earlier versions. They are only ever deleted by retention. */
    private static final String LEGACY_EXTENSION = ".xml";

    private final String conversationId;
    private @Getter long createdAt;
    private @Getter @Setter @Nullable String modelId;
    private final List<ConversationEntry> entries = new ArrayList<>();
    private transient List<ModelContent> conversation = new ArrayList<>();
    private transient boolean stored;

    /**
     * Creates a new conversation stored in {@code JENKINS_HOME/conversations}.
     */
    public ModelConversation() {
        this(UUID.randomUUID(), true);
    }

    /**
     * Loads a conversation from {@code JENKINS_HOME/conversations}. Only its {@link #getEntries() entries} are
     * available; {@link #getConversation()} only holds content added since it was loaded.
     */
    public ModelConversation(UUID conversationId) throws IOException {
        this(conversationId, true);
        load();
    }

    private ModelConversation(UUID conversationId, boolean stored) {
        this.conversationId = conversationId.toString();
        this.createdAt = System.currentTimeMillis();
        this.stored = stored;
    }

    /**
     * Creates a conversation that is not saved to {@code JENKINS_HOME/conversations}.
     */
    public static ModelConversation inMemory() {
        return new ModelConversation(UUID.randomUUID(), false);
    }

    public UUID getConversationId() {
        return UUID.fromString(conversationId);
    }

    public Date getCreatedDate() {
        return new Date(createdAt);
    }

    public boolean isStored() {
        return stored;
    }

    public void addContent(ModelContent content) throws IOException {
        addAllContent(List.of(content));
    }

    public void addAllContent(Collection<? extends ModelContent> content) throws IOException {
        synchronized (this) {
            conversation.addAll(content);
            for (ModelContent item : content) {
                entries.add(ConversationEntry.from(item));
            }
        }
        save();
    }

    /**
     * @return The content added to this conversation since it was created or loaded in this Jenkins session.
     */
    public synchronized List<ModelContent> getConversation() {
        return Collections.unmodifiableList(new ArrayList<>(conversation));
    }

    public synchronized List<ConversationEntry> getEntries() {
        return Collections.unmodifiableList(new ArrayList<>(entries));
    }

    /**
     * Writes the conversation to {@code JENKINS_HOME/conversations}. Does nothing for {@link #inMemory()}
     * conversations.
     */
    public void save() throws IOException {
        if (!stored) {
            return;
        }
        StoredConversation data = new StoredConversation();
        synchronized (this) {
            data.conversationId = conversationId;
            data.createdAt = createdAt;
            data.modelId = modelId;
            data.entries = new ArrayList<>(entries);
        }
        String json = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(data);
        File file = getFile(getConversationId());
        Files.createDirectories(file.getParentFile().toPath());
        try (AtomicFileWriter writer = new AtomicFileWriter(file.toPath(), StandardCharsets.UTF_8)) {
            writer.write(json);
            writer.commit();
        }
    }

    private synchronized void load() throws IOException {
        File file = getFile(getConversationId());
        if (!file.isFile()) {
            return;
        }
        StoredConversation data = MAPPER.readValue(file, StoredConversation.class);
        createdAt = data.createdAt;
        modelId = data.modelId;
        entries.clear();
        if (data.entries != null) {
            entries.addAll(data.entries);
        }
    }

    private Object readResolve() {
        // Transient fields are not restored when this is loaded as part of a build's ModelConversationsAction.
        conversation = new ArrayList<>();
        return this;
    }

    public static File getStorageDirectory() {
        return new File(Jenkins.get().getRootDir(), DIRECTORY_NAME);
    }

    private static File getFile(UUID conversationId) {
        return new File(getStorageDirectory(), conversationId + EXTENSION);
    }

    /**
     * @return All conversations stored in {@code JENKINS_HOME/conversations}, newest first. Files that are not named
     * after a conversation id or cannot be read are skipped.
     */
    public static List<ModelConversation> loadAll() {
        List<ModelConversation> conversations = new ArrayList<>();
        for (File file : listStoredFiles(EXTENSION)) {
            UUID conversationId = parseConversationId(file.getName());
            if (conversationId == null) {
                continue;
            }
            try {
                conversations.add(new ModelConversation(conversationId));
            } catch (IOException | RuntimeException e) {
                LOGGER.log(Level.WARNING, "Skipping unreadable conversation file " + file, e);
            }
        }
        conversations.sort(
                Comparator.comparingLong(ModelConversation::getCreatedAt).reversed());
        return conversations;
    }

    /**
     * Deletes stored conversations that were last written more than {@code maxAge} ago.
     *
     * @return The number of conversations deleted
     */
    public static int deleteOlderThan(Duration maxAge) {
        long cutoff = System.currentTimeMillis() - maxAge.toMillis();
        int deleted = 0;
        List<File> files = new ArrayList<>(listStoredFiles(EXTENSION));
        files.addAll(listStoredFiles(LEGACY_EXTENSION));
        for (File file : files) {
            if (parseConversationId(file.getName()) == null || file.lastModified() >= cutoff) {
                continue;
            }
            try {
                Files.deleteIfExists(file.toPath());
                deleted++;
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to delete expired conversation file " + file, e);
            }
        }
        return deleted;
    }

    private static List<File> listStoredFiles(String extension) {
        File[] files = getStorageDirectory().listFiles((dir, name) -> name.endsWith(extension));
        return files == null ? List.of() : Arrays.asList(files);
    }

    private static @Nullable UUID parseConversationId(String fileName) {
        int extensionIndex = fileName.lastIndexOf('.');
        String name = extensionIndex > 0 ? fileName.substring(0, extensionIndex) : fileName;
        try {
            return UUID.fromString(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** The JSON format of stored conversations. */
    static final class StoredConversation {
        public String conversationId;
        public long createdAt;
        public @Nullable String modelId;
        public List<ConversationEntry> entries;
    }
}
