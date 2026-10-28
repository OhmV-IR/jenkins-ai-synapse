package dev.ohmvir.plugins.jenkinsaisynapse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.ohmvir.plugins.jenkinsaisynapse.api.ConversationEntry;
import dev.ohmvir.plugins.jenkinsaisynapse.api.ModelConversation;
import dev.ohmvir.plugins.jenkinsaisynapse.api.client.ModelClient;
import dev.ohmvir.plugins.jenkinsaisynapse.api.client.ModelRequestException;
import dev.ohmvir.plugins.jenkinsaisynapse.api.client.ModelRequestListener;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.InputImageContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.InputSkillContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.InputTextContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.ModelInput;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.ModelRequest;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.SystemPromptContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.ThinkingLevelContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.ToolCallResponseContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelInputType;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelOutputType;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelRegistry;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelThinkingLevel;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.RegisteredModel;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.FinishReasonContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.ModelOutput;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.ModelResponse;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.OutputImageContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.OutputTextContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.ThinkingContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.ToolCallContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.skills.ReadSkillFileTool;
import dev.ohmvir.plugins.jenkinsaisynapse.api.skills.SkillData;
import dev.ohmvir.plugins.jenkinsaisynapse.api.tools.Tool;
import dev.ohmvir.plugins.jenkinsaisynapse.api.tools.ToolArgumentDescription;
import dev.ohmvir.plugins.jenkinsaisynapse.configuration.RequestsConfiguration;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

public class ModelRequestTest {
    public static class EchoTool extends Tool {
        public EchoTool() throws NoSuchMethodException {}

        @Override
        public String getName() {
            return "echo";
        }

        @Override
        public String getDescription() {
            return "Repeats its input";
        }

        @Override
        public List<ToolArgumentDescription> getArguments() {
            return List.of(new ToolArgumentDescription("text", String.class, "Text to repeat", true));
        }

        public String echo(String text) {
            return "echo: " + text;
        }
    }

    @Test
    public void requirementsComeFromInputsOutputsAndExplicitCapabilities() {
        ModelRequest request = new ModelRequest();
        request.addInput(new InputTextContent("describe this"));
        request.addInput(new InputImageContent(new byte[] {1}, "image/png"));
        request.addInput(new ThinkingLevelContent(ModelThinkingLevel.HIGH));
        request.requestOutput(ThinkingContent.KIND);
        request.requestOutput(OutputImageContent.KIND);
        request.requireCapability(ModelCapability.of("vendor:computer-use"));

        assertEquals(Set.of(ModelInputType.TEXT, ModelInputType.IMAGE), request.getInputTypes());
        assertEquals(Set.of(ModelOutputType.IMAGE), request.getOutputTypes());
        assertEquals(
                Set.of(ModelCapability.THINKING, ModelCapability.of("vendor:computer-use")),
                request.getRequiredCapabilities());

        request.removeOutputTypeRequest(ModelOutputType.IMAGE);
        assertEquals(Set.of(), request.getOutputTypes());
    }

    @Test
    public void thinkingOffDoesNotNeedAThinkingModel() {
        ModelRequest request = new ModelRequest();
        request.addInput(new ThinkingLevelContent(ModelThinkingLevel.OFF));
        assertEquals(Set.of(), request.getRequiredCapabilities());
    }

    @Test
    public void getInputReturnsNullWhenAbsent() {
        ModelRequest request = new ModelRequest();
        assertNull(request.getInput(InputTextContent.class));
        request.addInput(new InputTextContent("hi"));
        assertEquals("hi", request.getInput(InputTextContent.class).getText());
    }

    @Test
    public void executeReturnsTheModelsOutputs() throws Exception {
        ModelClient client = (model, request, inputs) -> List.of(new OutputTextContent("hello back"));
        ModelRegistry registry = new ModelRegistry();
        registry.setModels(List.of(TestModels.registered("m", client)));

        ModelRequest request = new ModelRequest();
        request.addInput(new InputTextContent("hello"));
        request.requestOutput(OutputTextContent.KIND);
        ModelResponse response = request.execute(registry);

        assertNotNull(response);
        assertEquals(1, response.getOutputs().size());
        assertEquals("hello back", ((OutputTextContent) response.getOutputs().get(0)).getText());
    }

    @Test
    public void executeFailsWhenARequestedOutputIsMissing() throws Exception {
        RegisteredModel model =
                TestModels.registered("m", (data, request, inputs) -> List.of(new OutputTextContent("text only")));
        model.data().getCapabilities().add(ModelCapability.PREMATURE_STOP);

        ModelRequest request = new ModelRequest();
        request.addInput(new InputTextContent("hello"));
        request.requestOutput(FinishReasonContent.KIND);
        assertNull(request.execute(model));
    }

    @Test
    public void executeRunsToolCallsUntilTheModelIsDone() throws Exception {
        EchoTool echoTool = new EchoTool();
        ObjectNode arguments = new ObjectMapper().createObjectNode().put("text", "ping");
        List<List<ModelInput>> seenInputs = new ArrayList<>();
        ModelClient client = (model, request, inputs) -> {
            seenInputs.add(inputs);
            List<ModelOutput> outputs = new ArrayList<>();
            if (seenInputs.size() == 1) {
                outputs.add(new ToolCallContent("call-1", arguments, "echo"));
            } else {
                outputs.add(new OutputTextContent("done"));
            }
            return outputs;
        };

        ModelRequest request = new ModelRequest();
        request.addInput(new InputTextContent("use the tool"));
        request.attachTool(echoTool);
        ModelResponse response = request.execute(TestModels.registered("m", client, ModelCapability.TOOLS));

        assertNotNull(response);
        assertEquals(2, seenInputs.size());
        ToolCallResponseContent toolResponse = assertInstanceOf(
                ToolCallResponseContent.class,
                seenInputs.get(1).stream()
                        .filter(ToolCallResponseContent.class::isInstance)
                        .findFirst()
                        .orElseThrow());
        assertTrue(toolResponse.isSuccessful());
        assertEquals("echo: ping", toolResponse.getResponseContent());
        assertEquals(2, response.getOutputs().size());
        // The request itself is left as it was, so it can be executed again
        assertEquals(2, request.getModelInputs().size());
    }

    @Test
    public void executeWithoutSuitableModelReturnsNull() throws Exception {
        ModelRegistry registry = new ModelRegistry();
        registry.setModels(List.of(TestModels.registered("m", (model, request, inputs) -> List.of())));
        ModelRequest request = new ModelRequest();
        request.addInput(new InputImageContent(new byte[] {1}, "image/png"));
        assertNull(request.execute(registry));
    }

    private static ModelRequest skillRequest() throws Exception {
        ModelRequest request = new ModelRequest();
        request.addInput(new InputTextContent("use the skill"));
        request.attachSkill(new SkillData("execute-skill", "A skill", null, null, null, null, "Do things", Map.of()));
        return request;
    }

    private static List<SystemPromptContent> systemPrompts(List<ModelInput> inputs) {
        return inputs.stream()
                .filter(SystemPromptContent.class::isInstance)
                .map(SystemPromptContent.class::cast)
                .toList();
    }

    @Test
    public void executeExpandsSkillsBeforeSelectingAModel() throws Exception {
        List<List<ModelInput>> seenInputs = new ArrayList<>();
        ModelClient client = (model, request, inputs) -> {
            seenInputs.add(inputs);
            return List.of(new OutputTextContent("done"));
        };
        ModelRegistry registry = new ModelRegistry();
        // No SKILLS capability, as the skill is sent as a system prompt and the skill-file tool
        registry.setModels(List.of(
                TestModels.registered("m", client, ModelCapability.TOOLS, ModelCapability.ADJUSTABLE_SYSTEM_PROMPT)));

        ModelRequest request = skillRequest();
        assertNotNull(request.execute(registry));

        assertEquals(1, seenInputs.size());
        assertTrue(seenInputs.get(0).stream().noneMatch(InputSkillContent.class::isInstance));
        List<SystemPromptContent> prompts = systemPrompts(seenInputs.get(0));
        assertEquals(1, prompts.size());
        assertTrue(prompts.get(0).getSystemPrompt().contains("<name>execute-skill</name>"));
        assertEquals(Set.of(ReadSkillFileTool.NAME), request.getAvailableTools().keySet());
    }

    @Test
    public void executeOnAModelExpandsSkillsOnce() throws Exception {
        List<List<ModelInput>> seenInputs = new ArrayList<>();
        ModelClient client = (model, request, inputs) -> {
            seenInputs.add(inputs);
            return List.of(new OutputTextContent("done"));
        };
        RegisteredModel model =
                TestModels.registered("m", client, ModelCapability.TOOLS, ModelCapability.ADJUSTABLE_SYSTEM_PROMPT);

        ModelRequest request = skillRequest();
        assertNotNull(request.execute(model));
        assertNotNull(request.execute(model));

        assertEquals(2, seenInputs.size());
        for (List<ModelInput> inputs : seenInputs) {
            assertTrue(inputs.stream().noneMatch(InputSkillContent.class::isInstance));
            assertEquals(1, systemPrompts(inputs).size());
        }
        assertEquals(
                systemPrompts(seenInputs.get(0)).get(0).getSystemPrompt(),
                systemPrompts(seenInputs.get(1)).get(0).getSystemPrompt());
    }

    @Test
    public void copyDoesNotShareCollectionsWithOriginal() throws Exception {
        ModelRequest original = skillRequest();
        original.requireCapability(ModelCapability.TOOLS);
        original.setModelId("m");

        ModelRequest copy = new ModelRequest(original);
        int originalInputs = original.getModelInputs().size();
        copy.addInput(new InputTextContent("extra"));
        copy.requireCapability(ModelCapability.ADJUSTABLE_SYSTEM_PROMPT);
        copy.expandSkills();

        assertEquals("m", copy.getModelId());
        assertEquals(originalInputs, original.getModelInputs().size());
        assertTrue(original.hasInputType(InputSkillContent.class));
        assertFalse(original.getRequiredCapabilities().contains(ModelCapability.ADJUSTABLE_SYSTEM_PROMPT));
        assertFalse(copy.hasInputType(InputSkillContent.class));
    }

    private static ModelRequest toolRequest() throws Exception {
        ModelRequest request = new ModelRequest();
        request.addInput(new InputTextContent("use the tool"));
        request.attachTool(new EchoTool());
        return request;
    }

    /** Runs the request against a model that calls the echo tool every time it is asked for a step. */
    private static ModelResponse runLooping(ModelRequest request, AtomicInteger steps, AtomicInteger toolCalls)
            throws Exception {
        ObjectNode arguments = new ObjectMapper().createObjectNode().put("text", "ping");
        ModelClient client = (model, modelRequest, inputs) ->
                List.of(new ToolCallContent("call-" + steps.incrementAndGet(), arguments, "echo"));
        RegisteredModel registered = TestModels.registered("m", client, ModelCapability.TOOLS);
        return request.executeOrThrow(registered, ModelRequestListener.NOOP, (tool, toolCall) -> {
            toolCalls.incrementAndGet();
            return tool.callTool(toolCall);
        });
    }

    @Test
    public void toolLoopStopsAtTheDefaultStepLimit() throws Exception {
        AtomicInteger steps = new AtomicInteger();
        AtomicInteger toolCalls = new AtomicInteger();
        ModelRequest request = toolRequest();
        assertEquals(RequestsConfiguration.DEFAULT_MAX_STEPS, request.getMaxSteps());

        ModelRequestException e =
                assertThrows(ModelRequestException.class, () -> runLooping(request, steps, toolCalls));

        assertEquals(RequestsConfiguration.DEFAULT_MAX_STEPS, steps.get());
        // The tool calls of the last step are not run, as their results could not be sent back
        assertEquals(RequestsConfiguration.DEFAULT_MAX_STEPS - 1, toolCalls.get());
        assertTrue(e.getMessage().contains(String.valueOf(RequestsConfiguration.DEFAULT_MAX_STEPS)));
        assertTrue(e.getMessage().contains("maxSteps"));
    }

    @Test
    public void toolLoopStepLimitIsConfigurable() throws Exception {
        AtomicInteger steps = new AtomicInteger();
        AtomicInteger toolCalls = new AtomicInteger();
        ModelRequest request = toolRequest();
        request.setMaxSteps(3);

        assertThrows(ModelRequestException.class, () -> runLooping(request, steps, toolCalls));

        assertEquals(3, steps.get());
        assertEquals(2, toolCalls.get());
    }

    @Test
    public void finalAnswerOnTheLastStepIsNotAnError() throws Exception {
        AtomicInteger steps = new AtomicInteger();
        ObjectNode arguments = new ObjectMapper().createObjectNode().put("text", "ping");
        ModelClient client = (model, modelRequest, inputs) -> {
            int step = steps.incrementAndGet();
            return step < 3
                    ? List.of(new ToolCallContent("call-" + step, arguments, "echo"))
                    : List.of(new OutputTextContent("done"));
        };
        ModelRequest request = toolRequest();
        request.setMaxSteps(3);

        assertNotNull(request.execute(TestModels.registered("m", client, ModelCapability.TOOLS)));
        assertEquals(3, steps.get());
    }

    @Test
    public void interruptedRequestDoesNotStartToolCalls() throws Exception {
        AtomicInteger steps = new AtomicInteger();
        AtomicInteger toolCalls = new AtomicInteger();
        ModelRequest request = toolRequest();

        Thread.currentThread().interrupt();
        try {
            InterruptedException e =
                    assertThrows(InterruptedException.class, () -> runLooping(request, steps, toolCalls));
            assertTrue(e.getMessage().contains("cancelled"), e.getMessage());
            assertTrue(e.getMessage().contains("echo"), e.getMessage());
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }

        assertEquals(1, steps.get());
        assertEquals(0, toolCalls.get());
    }

    @Test
    public void cancellationBetweenToolCallsSkipsTheRemainingOnes() throws Exception {
        ObjectNode arguments = new ObjectMapper().createObjectNode().put("text", "ping");
        ModelClient client = (model, modelRequest, inputs) -> List.of(
                new ToolCallContent("call-1", arguments, "echo"), new ToolCallContent("call-2", arguments, "echo"));
        AtomicInteger toolCalls = new AtomicInteger();
        ModelRequest request = toolRequest();
        RegisteredModel registered = TestModels.registered("m", client, ModelCapability.TOOLS);

        try {
            assertThrows(
                    InterruptedException.class,
                    () -> request.executeOrThrow(registered, ModelRequestListener.NOOP, (tool, toolCall) -> {
                        toolCalls.incrementAndGet();
                        // The build is aborted while the first tool call runs
                        Thread.currentThread().interrupt();
                        return tool.callTool(toolCall);
                    }));
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }

        assertEquals(1, toolCalls.get());
    }

    @Test
    public void maxStepsMustBePositiveAndIsCopied() {
        ModelRequest request = new ModelRequest();
        assertThrows(IllegalArgumentException.class, () -> request.setMaxSteps(0));
        assertThrows(IllegalArgumentException.class, () -> request.setMaxSteps(-1));
        assertEquals(RequestsConfiguration.DEFAULT_MAX_STEPS, request.getMaxSteps());

        request.setMaxSteps(7);
        assertEquals(7, new ModelRequest(request).getMaxSteps());
    }

    @Test
    public void maxStepsDefaultsTo25WithoutJenkinsAndAnExplicitValueWins() {
        // This test runs without Jenkins, so the default is the built-in one
        assertEquals(25, ModelRequest.getDefaultMaxSteps());
        ModelRequest request = new ModelRequest();
        assertEquals(25, request.getMaxSteps());
        assertEquals(25, new ModelRequest(request).getMaxSteps());

        request.setMaxSteps(3);
        assertEquals(3, request.getMaxSteps());
        assertEquals(3, new ModelRequest(request).getMaxSteps());
    }

    private static List<String> entryTypes(ModelConversation conversation) {
        return conversation.getEntries().stream().map(ConversationEntry::getType).toList();
    }

    @Test
    public void conversationHoldsEachStepOnceWhenTheRequestSucceeds() throws Exception {
        AtomicInteger steps = new AtomicInteger();
        ObjectNode arguments = new ObjectMapper().createObjectNode().put("text", "ping");
        ModelClient client = (model, modelRequest, inputs) -> steps.incrementAndGet() == 1
                ? List.of(new ToolCallContent("call-1", arguments, "echo"))
                : List.of(new OutputTextContent("done"));
        ModelConversation conversation = ModelConversation.inMemory();
        ModelRequest request = toolRequest();
        request.setAssociatedConversation(conversation);

        request.executeOrThrow(
                TestModels.registered("m", client, ModelCapability.TOOLS),
                ModelRequestListener.NOOP,
                (tool, toolCall) -> tool.callTool(toolCall));

        assertEquals(
                List.of(
                        "InputTextContent",
                        "InputToolContent",
                        "ToolCallContent",
                        "ToolCallResponseContent",
                        "OutputTextContent"),
                entryTypes(conversation));
        assertEquals(5, conversation.getConversation().size());
    }

    @Test
    public void conversationKeepsTheStepsCompletedBeforeTheClientFails() throws Exception {
        AtomicInteger steps = new AtomicInteger();
        ObjectNode arguments = new ObjectMapper().createObjectNode().put("text", "ping");
        ModelClient client = (model, modelRequest, inputs) -> {
            if (steps.incrementAndGet() == 2) {
                throw new IOException("connection reset");
            }
            return List.of(new ToolCallContent("call-1", arguments, "echo"));
        };
        ModelConversation conversation = ModelConversation.inMemory();
        ModelRequest request = toolRequest();
        request.setAssociatedConversation(conversation);

        assertThrows(
                ModelRequestException.class,
                () -> request.executeOrThrow(
                        TestModels.registered("m", client, ModelCapability.TOOLS),
                        ModelRequestListener.NOOP,
                        (tool, toolCall) -> tool.callTool(toolCall)));

        assertEquals(2, steps.get());
        assertEquals(
                List.of("InputTextContent", "InputToolContent", "ToolCallContent", "ToolCallResponseContent"),
                entryTypes(conversation));
        assertEquals("echo: ping", conversation.getEntries().get(3).getText());
    }

    @Test
    public void conversationKeepsTheOutputOfARequestThatMissesARequestedOutput() throws Exception {
        ModelClient client = (model, modelRequest, inputs) -> List.of(new OutputTextContent("only text"));
        ModelConversation conversation = ModelConversation.inMemory();
        ModelRequest request = new ModelRequest();
        request.addInput(new InputTextContent("draw"));
        request.requestOutput(OutputImageContent.KIND);
        request.setAssociatedConversation(conversation);

        assertThrows(
                ModelRequestException.class,
                () -> request.executeOrThrow(
                        TestModels.registered("m", client, ModelCapability.TOOLS),
                        ModelRequestListener.NOOP,
                        (tool, toolCall) -> tool.callTool(toolCall)));

        assertEquals(List.of("InputTextContent", "OutputTextContent"), entryTypes(conversation));
    }

    @Test
    public void conversationKeepsToolResultsOfACancelledRequest() throws Exception {
        ObjectNode arguments = new ObjectMapper().createObjectNode().put("text", "ping");
        ModelClient client = (model, modelRequest, inputs) -> List.of(
                new ToolCallContent("call-1", arguments, "echo"), new ToolCallContent("call-2", arguments, "echo"));
        ModelConversation conversation = ModelConversation.inMemory();
        ModelRequest request = toolRequest();
        request.setAssociatedConversation(conversation);
        RegisteredModel registered = TestModels.registered("m", client, ModelCapability.TOOLS);

        try {
            assertThrows(
                    InterruptedException.class,
                    () -> request.executeOrThrow(registered, ModelRequestListener.NOOP, (tool, toolCall) -> {
                        // The build is aborted while the first tool call runs
                        Thread.currentThread().interrupt();
                        return tool.callTool(toolCall);
                    }));
        } finally {
            Thread.interrupted();
        }

        assertEquals(
                List.of(
                        "InputTextContent",
                        "InputToolContent",
                        "ToolCallContent",
                        "ToolCallContent",
                        "ToolCallResponseContent"),
                entryTypes(conversation));
    }
}
