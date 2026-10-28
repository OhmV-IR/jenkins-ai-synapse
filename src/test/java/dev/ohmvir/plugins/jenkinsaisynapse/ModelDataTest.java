package dev.ohmvir.plugins.jenkinsaisynapse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.ohmvir.plugins.jenkinsaisynapse.TestModels.TestConfiguration;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.InputFileContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.InputImageContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.InputTextContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.ModelRequest;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.TemperatureContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.ThinkingLevelContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelData;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelThinkingLevel;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.OutputImageContent;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ModelDataTest {
    private static ModelData textModel(ModelCapability... capabilities) throws Exception {
        return TestModels.textModel(new TestConfiguration("text"), capabilities);
    }

    private static ModelRequest textRequest() {
        ModelRequest request = new ModelRequest();
        request.addInput(new InputTextContent("hello"));
        return request;
    }

    private static void assertSingleReason(String expected, ModelData data, ModelRequest request) {
        assertEquals(List.of(expected), data.unsupportedReasons(request));
        assertFalse(data.supportsRequest(request));
    }

    @Test
    public void supportedRequestHasNoReasons() throws Exception {
        ModelData data = textModel(ModelCapability.TOOLS);
        ModelRequest request = textRequest();
        request.requireCapability(ModelCapability.TOOLS);

        assertEquals(List.of(), data.unsupportedReasons(request));
        assertTrue(data.supportsRequest(request));
        assertEquals(List.of(), data.unsupportedReasons(textRequest()));
    }

    @Test
    public void reportsMissingInputType() throws Exception {
        ModelRequest request = textRequest();
        request.addInput(new InputFileContent("notes.txt", new byte[] {1}, "text/plain"));
        assertSingleReason("does not support the FILE input type", textModel(), request);
    }

    @Test
    public void reportsMissingImageInput() throws Exception {
        ModelRequest request = textRequest();
        request.addInput(new InputImageContent(new byte[] {1}, "image/png"));
        assertTrue(textModel().unsupportedReasons(request).contains("does not support the IMAGE input type"));
        assertFalse(textModel().supportsRequest(request));
    }

    @Test
    public void reportsMissingOutputType() throws Exception {
        ModelRequest request = textRequest();
        request.requestOutput(OutputImageContent.KIND);
        List<String> reasons = textModel().unsupportedReasons(request);
        assertTrue(reasons.contains("does not support the IMAGE output type"), reasons.toString());
        assertFalse(textModel().supportsRequest(request));
    }

    @Test
    public void reportsMissingCapability() throws Exception {
        ModelRequest request = textRequest();
        request.requireCapability(ModelCapability.TOOLS);
        assertSingleReason("does not support the TOOLS capability", textModel(), request);
    }

    @Test
    public void reportsEveryMissingCapabilityInAStableOrder() throws Exception {
        ModelRequest request = textRequest();
        request.requireCapability(ModelCapability.CUSTOM_TOP_K);
        request.requireCapability(ModelCapability.CUSTOM_STOP_SEQUENCES);
        request.requireCapability(ModelCapability.TOOLS);
        assertEquals(
                List.of(
                        "does not support the CUSTOM_STOP_SEQUENCES capability",
                        "does not support the CUSTOM_TOP_K capability",
                        "does not support the TOOLS capability"),
                textModel(ModelCapability.THINKING).unsupportedReasons(request));
    }

    @Test
    public void reportsUnsupportedThinkingLevel() throws Exception {
        ModelRequest request = textRequest();
        request.addInput(new ThinkingLevelContent(ModelThinkingLevel.HIGH));
        List<String> reasons = textModel(ModelCapability.THINKING).unsupportedReasons(request);
        assertEquals(List.of("does not support thinking level HIGH"), reasons);

        ModelData thinking = textModel(ModelCapability.THINKING);
        thinking.setSupportedThinkingLevels(List.of(ModelThinkingLevel.ON));
        assertEquals(List.of(), thinking.unsupportedReasons(request));
        assertTrue(thinking.supportsRequest(request));
    }

    @Test
    public void reportsTemperatureAboveTheMaximum() throws Exception {
        ModelData data = textModel(ModelCapability.CUSTOM_TEMPERATURE);
        data.setMaxTemperature(1.0);
        ModelRequest request = textRequest();
        request.addInput(new TemperatureContent(1.5));
        assertSingleReason("temperature 1.5 exceeds the model's maximum of 1.0", data, request);

        ModelRequest withinMaximum = textRequest();
        withinMaximum.addInput(new TemperatureContent(0.5));
        assertEquals(List.of(), data.unsupportedReasons(withinMaximum));
        assertTrue(data.supportsRequest(withinMaximum));
    }

    @Test
    public void reportsNegativeTemperature() throws Exception {
        ModelRequest request = textRequest();
        request.addInput(new TemperatureContent(-0.5));
        assertSingleReason("temperature -0.5 is negative", textModel(ModelCapability.CUSTOM_TEMPERATURE), request);
    }

    @Test
    public void temperatureWithoutTheCapabilityReportsBothReasons() throws Exception {
        ModelData data = textModel();
        data.setMaxTemperature(1.0);
        ModelRequest request = textRequest();
        request.addInput(new TemperatureContent(2.0));
        assertEquals(
                List.of(
                        "temperature 2.0 exceeds the model's maximum of 1.0",
                        "does not support the CUSTOM_TEMPERATURE capability"),
                data.unsupportedReasons(request));
    }

    @Test
    public void supportsRequestAgreesWithTheReasons() throws Exception {
        ModelData data = textModel(ModelCapability.TOOLS);
        ModelRequest ok = textRequest();
        ModelRequest needsTopK = textRequest();
        needsTopK.requireCapability(ModelCapability.CUSTOM_TOP_K);
        ModelRequest needsImage = textRequest();
        needsImage.requestOutput(OutputImageContent.KIND);
        for (ModelRequest request : List.of(ok, needsTopK, needsImage)) {
            assertEquals(data.unsupportedReasons(request).isEmpty(), data.supportsRequest(request));
        }
        assertTrue(data.supportsRequest(ok));
        assertFalse(data.supportsRequest(needsTopK));
        assertFalse(data.supportsRequest(needsImage));
    }
}
