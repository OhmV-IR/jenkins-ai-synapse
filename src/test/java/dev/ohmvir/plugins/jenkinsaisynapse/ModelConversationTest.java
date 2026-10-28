package dev.ohmvir.plugins.jenkinsaisynapse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.ohmvir.plugins.jenkinsaisynapse.api.ConversationEntry;
import dev.ohmvir.plugins.jenkinsaisynapse.api.ModelConversation;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.InputImageContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.InputTextContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.StopSequencesContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.TopKContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.TopPContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.CitationContent;
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
    public void samplingParametersAreRecordedAsText() {
        assertEquals("0.9", ConversationEntry.from(new TopPContent(0.9)).getText());
        assertEquals("40.0", ConversationEntry.from(new TopKContent(40)).getText());
        assertEquals(
                "[END, STOP]",
                ConversationEntry.from(new StopSequencesContent(List.of("END", "STOP")))
                        .getText());
    }

    @Test
    public void citationsAreRecordedAsText() {
        ConversationEntry entry =
                ConversationEntry.from(new CitationContent("the sky is blue", 1, "Facts", 25, "file-1", 10));
        assertEquals(ConversationEntry.DIRECTION_OUTPUT, entry.getDirection());
        assertEquals("the sky is blue\ndocument 1 (Facts), file file-1, characters 10-25", entry.getText());

        ConversationEntry untitled = ConversationEntry.from(new CitationContent("quote", 0, null, 5, null, 0));
        assertEquals("quote\ndocument 0, characters 0-5", untitled.getText());
    }

    @Test
    public void longTextIsTruncated() {
        ConversationEntry entry =
                ConversationEntry.from(new OutputTextContent("x".repeat(ConversationEntry.MAX_TEXT_LENGTH + 10)));
        assertTrue(entry.getText().endsWith("[truncated 10 characters]"));
    }

    @Test
    public void secretsAreMaskedInRecordedText() {
        ModelConversation conversation = ModelConversation.inMemory();
        conversation.maskSecrets(List.of("hunter2", "hunter2-long", ""));
        conversation.addAllContent(List.of(
                new InputTextContent("Check hunter2 and hunter2-long"), new OutputTextContent("no secrets here")));

        List<ConversationEntry> entries = conversation.getEntries();
        assertEquals("Check **** and ****", entries.get(0).getText());
        assertEquals("no secrets here", entries.get(1).getText());
    }

    @Test
    public void secretsAreMaskedBeforeTruncation() {
        String text = "x".repeat(ConversationEntry.MAX_TEXT_LENGTH - 3) + "hunter2";
        ConversationEntry entry = ConversationEntry.from(new OutputTextContent(text), List.of("hunter2"));
        assertTrue(entry.getText().startsWith("x".repeat(ConversationEntry.MAX_TEXT_LENGTH - 3) + "***\n"));
        assertTrue(entry.getText().endsWith("[truncated 1 characters]"));
    }
}
