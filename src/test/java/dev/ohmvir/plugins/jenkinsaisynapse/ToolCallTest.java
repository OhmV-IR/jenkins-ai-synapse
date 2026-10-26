package dev.ohmvir.plugins.jenkinsaisynapse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.ToolCallResponseContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.ToolCallContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.tools.Tool;
import dev.ohmvir.plugins.jenkinsaisynapse.api.tools.ToolArgumentDescription;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ToolCallTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static class RepeatTool extends Tool {
        public RepeatTool() throws NoSuchMethodException {}

        @Override
        public String getName() {
            return "repeat";
        }

        @Override
        public String getDescription() {
            return "Repeats text";
        }

        @Override
        public List<ToolArgumentDescription> getArguments() {
            return List.of(
                    new ToolArgumentDescription("text", String.class, "Text to repeat", true),
                    new ToolArgumentDescription("times", Integer.class, "How many times, defaults to 1", false));
        }

        public String repeat(String text, Integer times) {
            if (text.equals("boom")) {
                throw new IllegalStateException("exploded");
            }
            return text.repeat(times == null ? 1 : times);
        }
    }

    public static class NoOutputTool extends Tool {
        public NoOutputTool() throws NoSuchMethodException {}

        @Override
        public String getName() {
            return "noOutput";
        }

        @Override
        public String getDescription() {
            return "Does nothing";
        }

        @Override
        public List<ToolArgumentDescription> getArguments() {
            return List.of();
        }

        public void noOutput() {}
    }

    private static ToolCallContent call(String json) throws Exception {
        return new ToolCallContent("call-1", json == null ? null : (ObjectNode) MAPPER.readTree(json), "repeat");
    }

    @Test
    public void validCallSucceeds() throws Exception {
        ToolCallResponseContent response = new RepeatTool().callTool(call("{\"text\": \"ab\", \"times\": 2}"));
        assertTrue(response.isSuccessful());
        assertEquals("abab", response.getResponseContent());
        assertEquals("call-1", response.getToolUseId());
        assertInstanceOf(RepeatTool.class, response.getCalledTool());
    }

    @Test
    public void optionalArgumentMayBeOmitted() throws Exception {
        ToolCallResponseContent response = new RepeatTool().callTool(call("{\"text\": \"ab\"}"));
        assertTrue(response.isSuccessful());
        assertEquals("ab", response.getResponseContent());
    }

    @Test
    public void missingRequiredArgumentIsAnError() throws Exception {
        ToolCallResponseContent response = new RepeatTool().callTool(call("{\"times\": 2}"));
        assertFalse(response.isSuccessful());
        assertEquals("call-1", response.getToolUseId());
        assertTrue(response.getResponseContent().contains("text"), response.getResponseContent());
    }

    @Test
    public void missingArgumentsObjectIsAnError() throws Exception {
        assertFalse(new RepeatTool().callTool(call(null)).isSuccessful());
    }

    @Test
    public void wronglyTypedArgumentIsAnError() throws Exception {
        assertFalse(new RepeatTool().callTool(call("{\"text\": 5}")).isSuccessful());
        assertFalse(new RepeatTool()
                .callTool(call("{\"text\": \"a\", \"times\": \"2\"}"))
                .isSuccessful());
        assertFalse(new RepeatTool()
                .callTool(call("{\"text\": \"a\", \"times\": 2.5}"))
                .isSuccessful());
    }

    @Test
    public void unknownArgumentIsAnError() throws Exception {
        assertFalse(new RepeatTool()
                .callTool(call("{\"text\": \"a\", \"other\": 1}"))
                .isSuccessful());
    }

    @Test
    public void exceptionInToolIsAnError() throws Exception {
        ToolCallResponseContent response = new RepeatTool().callTool(call("{\"text\": \"boom\"}"));
        assertFalse(response.isSuccessful());
        assertTrue(response.getResponseContent().contains("exploded"), response.getResponseContent());
    }

    @Test
    public void voidToolSucceedsWithEmptyContent() throws Exception {
        ToolCallResponseContent response = new NoOutputTool().callTool(call("{}"));
        assertTrue(response.isSuccessful());
        assertEquals("", response.getResponseContent());
    }
}
