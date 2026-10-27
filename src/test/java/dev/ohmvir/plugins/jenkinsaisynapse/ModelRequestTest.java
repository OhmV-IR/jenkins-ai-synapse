package dev.ohmvir.plugins.jenkinsaisynapse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.ohmvir.plugins.jenkinsaisynapse.api.client.ModelClient;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        // Attached up front so expansion does not look the tool up from Jenkins
        request.attachTool(new ReadSkillFileTool());
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
}
