package dev.ohmvir.plugins.jenkinsaisynapse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.ohmvir.plugins.jenkinsaisynapse.api.ConversationEntry;
import dev.ohmvir.plugins.jenkinsaisynapse.api.ModelConversation;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.InputImageContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.InputTextContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.OutputTextContent;
import hudson.model.Run;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
public class ModelConversationTest {

    @Test
    public void conversationEntriesSurviveBuildSerializationWithoutBinaryPayloads() {
        ModelConversation conversation = ModelConversation.inMemory();
        conversation.setModelId("fake:echo");
        conversation.addAllContent(List.of(
                new InputTextContent("hello"),
                new InputImageContent(new byte[] {1, 2, 3}, "image/png"),
                new OutputTextContent("hi there")));

        ModelConversation loaded = (ModelConversation) Run.XSTREAM2.fromXML(Run.XSTREAM2.toXML(conversation));
        List<ConversationEntry> entries = loaded.getEntries();

        assertEquals("fake:echo", loaded.getModelId());
        assertEquals(conversation.getConversationId(), loaded.getConversationId());
        assertEquals(3, entries.size());
        assertEquals(ConversationEntry.DIRECTION_INPUT, entries.get(0).getDirection());
        assertEquals("hello", entries.get(0).getText());
        assertEquals("image/png", entries.get(1).getMimeType());
        assertEquals(3L, entries.get(1).getSizeBytes());
        assertNull(entries.get(1).getText());
        assertEquals(ConversationEntry.DIRECTION_OUTPUT, entries.get(2).getDirection());
        assertEquals("OutputTextContent", entries.get(2).getType());
        assertTrue(loaded.getConversation().isEmpty());
    }

    @Test
    public void longTextIsTruncated() {
        ConversationEntry entry =
                ConversationEntry.from(new OutputTextContent("x".repeat(ConversationEntry.MAX_TEXT_LENGTH + 10)));
        assertTrue(entry.getText().endsWith("[truncated 10 characters]"));
    }
}
