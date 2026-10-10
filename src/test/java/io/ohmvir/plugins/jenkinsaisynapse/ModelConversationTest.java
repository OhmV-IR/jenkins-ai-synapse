package io.ohmvir.plugins.jenkinsaisynapse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.ohmvir.plugins.jenkinsaisynapse.api.ConversationEntry;
import io.ohmvir.plugins.jenkinsaisynapse.api.ModelConversation;
import io.ohmvir.plugins.jenkinsaisynapse.api.input.InputImageContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.input.InputTextContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.OutputTextContent;
import java.io.File;
import java.nio.file.Files;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
public class ModelConversationTest {

    @Test
    public void storedConversationsAreJsonAndCanBeLoaded(JenkinsRule j) throws Exception {
        ModelConversation conversation = new ModelConversation();
        conversation.setModelId("fake:echo");
        conversation.addAllContent(List.of(
                new InputTextContent("hello"),
                new InputImageContent(new byte[] {1, 2, 3}, "image/png"),
                new OutputTextContent("hi there")));

        File file = new File(ModelConversation.getStorageDirectory(), conversation.getConversationId() + ".json");
        String json = Files.readString(file.toPath());
        assertTrue(json.contains("\"text\" : \"hi there\""), json);
        assertFalse(json.contains("imageData"), json);

        ModelConversation loaded = new ModelConversation(conversation.getConversationId());
        assertEquals("fake:echo", loaded.getModelId());
        assertEquals(conversation.getCreatedAt(), loaded.getCreatedAt());
        List<ConversationEntry> entries = loaded.getEntries();
        assertEquals(3, entries.size());
        assertEquals(ConversationEntry.DIRECTION_INPUT, entries.get(0).getDirection());
        assertEquals("hello", entries.get(0).getText());
        assertEquals("image/png", entries.get(1).getMimeType());
        assertEquals(3L, entries.get(1).getSizeBytes());
        assertNull(entries.get(1).getText());
        assertEquals(ConversationEntry.DIRECTION_OUTPUT, entries.get(2).getDirection());
        assertEquals("OutputTextContent", entries.get(2).getType());
    }

    @Test
    public void inMemoryConversationsAreNotStored(JenkinsRule j) throws Exception {
        ModelConversation conversation = ModelConversation.inMemory();
        conversation.addContent(new InputTextContent("hello"));
        assertEquals(1, conversation.getConversation().size());
        assertFalse(
                new File(ModelConversation.getStorageDirectory(), conversation.getConversationId() + ".json").exists());
    }

    @Test
    public void longTextIsTruncated(JenkinsRule j) {
        ConversationEntry entry =
                ConversationEntry.from(new OutputTextContent("x".repeat(ConversationEntry.MAX_TEXT_LENGTH + 10)));
        assertTrue(entry.getText().endsWith("[truncated 10 characters]"));
    }

    @Test
    public void loadAllSkipsUnrelatedFiles(JenkinsRule j) throws Exception {
        ModelConversation conversation = new ModelConversation();
        conversation.addContent(new InputTextContent("hello"));
        File directory = ModelConversation.getStorageDirectory();
        Files.writeString(new File(directory, "notes.json").toPath(), "{}");
        Files.writeString(new File(directory, UUID.randomUUID() + ".json").toPath(), "not json");
        Files.writeString(new File(directory, UUID.randomUUID() + ".xml").toPath(), "<legacy/>");

        List<ModelConversation> conversations = ModelConversation.loadAll();
        assertEquals(1, conversations.size());
        assertEquals(conversation.getConversationId(), conversations.get(0).getConversationId());
    }

    @Test
    public void expiredConversationsAreDeleted(JenkinsRule j) throws Exception {
        ModelConversation recent = new ModelConversation();
        recent.addContent(new InputTextContent("recent"));
        ModelConversation old = new ModelConversation();
        old.addContent(new InputTextContent("old"));
        File directory = ModelConversation.getStorageDirectory();
        File oldFile = new File(directory, old.getConversationId() + ".json");
        File legacyFile = new File(directory, UUID.randomUUID() + ".xml");
        Files.writeString(legacyFile.toPath(), "<legacy/>");
        long tenDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(10);
        assertTrue(oldFile.setLastModified(tenDaysAgo));
        assertTrue(legacyFile.setLastModified(tenDaysAgo));

        assertEquals(2, ModelConversation.deleteOlderThan(Duration.ofDays(7)));

        assertFalse(oldFile.exists());
        assertFalse(legacyFile.exists());
        assertTrue(new File(directory, recent.getConversationId() + ".json").exists());
    }
}
