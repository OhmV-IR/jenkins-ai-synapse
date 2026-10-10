package io.ohmvir.plugins.jenkinsaisynapse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.ohmvir.plugins.jenkinsaisynapse.api.input.ToolCallResponseContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.tools.Tool;
import io.ohmvir.plugins.jenkinsaisynapse.api.tools.ToolArgumentDescription;
import io.ohmvir.plugins.jenkinsaisynapse.api.tools.ToolResult;
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

    private static ObjectNode args(String json) throws Exception {
        return (ObjectNode) MAPPER.readTree(json);
    }

    @Test
    public void validCallSucceeds() throws Exception {
        ToolResult result = new RepeatTool().callTool(args("{\"text\": \"ab\", \"times\": 2}"));
        assertFalse(result.isError());
        assertEquals("abab", result.content());
    }

    @Test
    public void optionalArgumentMayBeOmitted() throws Exception {
        ToolResult result = new RepeatTool().callTool(args("{\"text\": \"ab\"}"));
        assertFalse(result.isError());
        assertEquals("ab", result.content());
    }

    @Test
    public void missingRequiredArgumentIsAnError() throws Exception {
        ToolResult result = new RepeatTool().callTool(args("{\"times\": 2}"));
        assertTrue(result.isError());
        assertTrue(result.content().contains("text"), result.content());
    }

    @Test
    public void missingArgumentsObjectIsAnError() throws Exception {
        assertTrue(new RepeatTool().callTool(null).isError());
    }

    @Test
    public void wronglyTypedArgumentIsAnError() throws Exception {
        assertTrue(new RepeatTool().callTool(args("{\"text\": 5}")).isError());
        assertTrue(new RepeatTool()
                .callTool(args("{\"text\": \"a\", \"times\": \"2\"}"))
                .isError());
        assertTrue(new RepeatTool()
                .callTool(args("{\"text\": \"a\", \"times\": 2.5}"))
                .isError());
    }

    @Test
    public void unknownArgumentIsAnError() throws Exception {
        assertTrue(new RepeatTool()
                .callTool(args("{\"text\": \"a\", \"other\": 1}"))
                .isError());
    }

    @Test
    public void exceptionInToolIsAnError() throws Exception {
        ToolResult result = new RepeatTool().callTool(args("{\"text\": \"boom\"}"));
        assertTrue(result.isError());
        assertTrue(result.content().contains("exploded"), result.content());
    }

    @Test
    public void voidToolSucceedsWithEmptyContent() throws Exception {
        ToolResult result = new NoOutputTool().callTool(args("{}"));
        assertFalse(result.isError());
        assertEquals("", result.content());
    }

    @Test
    public void failedResultIsReportedAsUnsuccessful() {
        ToolCallResponseContent response = new ToolCallResponseContent("id", ToolResult.error("bad"), null);
        assertFalse(response.isSuccessful());
        assertTrue(response.isError());
        assertEquals("bad", response.getResponseContent());
    }
}
