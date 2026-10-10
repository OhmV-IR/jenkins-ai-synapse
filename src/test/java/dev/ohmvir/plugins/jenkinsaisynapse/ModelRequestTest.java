package io.ohmvir.plugins.jenkinsaisynapse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.ohmvir.plugins.jenkinsaisynapse.api.client.ModelClient;
import io.ohmvir.plugins.jenkinsaisynapse.api.input.InputImageContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.input.InputTextContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.input.ModelInput;
import io.ohmvir.plugins.jenkinsaisynapse.api.input.ModelRequest;
import io.ohmvir.plugins.jenkinsaisynapse.api.input.ThinkingLevelContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.input.ToolCallResponseContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelInputType;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelOutputType;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelRegistry;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelThinkingLevel;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.RegisteredModel;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.FinishReasonContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.ModelOutput;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.ModelResponse;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.OutputImageContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.OutputTextContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.ThinkingContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.output.ToolCallContent;
import io.ohmvir.plugins.jenkinsaisynapse.api.tools.Tool;
import io.ohmvir.plugins.jenkinsaisynapse.api.tools.ToolArgumentDescription;
import java.util.ArrayList;
import java.util.List;
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
        new EchoTool();
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
        ModelResponse response = request.execute(TestModels.registered("m", client, ModelCapability.TOOLS));

        assertNotNull(response);
        assertEquals(2, seenInputs.size());
        ToolCallResponseContent toolResponse = assertInstanceOf(
                ToolCallResponseContent.class, seenInputs.get(1).get(1));
        assertTrue(toolResponse.isSuccessful());
        assertEquals("echo: ping", toolResponse.getResponseContent());
        assertEquals(2, response.getOutputs().size());
        // The request itself is left as it was, so it can be executed again
        assertEquals(1, request.getModelInputs().size());
    }

    @Test
    public void executeWithoutSuitableModelReturnsNull() throws Exception {
        ModelRegistry registry = new ModelRegistry();
        registry.setModels(List.of(TestModels.registered("m", (model, request, inputs) -> List.of())));
        ModelRequest request = new ModelRequest();
        request.addInput(new InputImageContent(new byte[] {1}, "image/png"));
        assertNull(request.execute(registry));
    }
}
