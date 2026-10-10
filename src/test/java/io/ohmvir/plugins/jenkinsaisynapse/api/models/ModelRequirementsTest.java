package io.ohmvir.plugins.jenkinsaisynapse.api.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.model.Descriptor;
import io.ohmvir.plugins.jenkinsaisynapse.api.message.MediaPart;
import io.ohmvir.plugins.jenkinsaisynapse.api.message.Message;
import io.ohmvir.plugins.jenkinsaisynapse.api.message.TextPart;
import io.ohmvir.plugins.jenkinsaisynapse.api.request.GenerationParams;
import io.ohmvir.plugins.jenkinsaisynapse.api.request.ModelRequest;
import io.ohmvir.plugins.jenkinsaisynapse.configuration.models.ModelConfiguration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ModelRequirementsTest {
    private static ModelData model(List<ModelCapability> capabilities, List<ModelInputType> inputs)
            throws Descriptor.FormException {
        ModelData model = new ModelData(new ModelConfiguration("m", "M") {
            @Override
            public String getProviderType() {
                return "test";
            }
        });
        model.setCapabilities(new ArrayList<>(capabilities));
        model.setInputs(new ArrayList<>(inputs));
        return model;
    }

    @Test
    void temperatureMustNotExceedTheModelsMaximum() throws Exception {
        ModelData model = model(List.of(ModelCapability.CUSTOM_TEMPERATURE), List.of(ModelInputType.TEXT));
        model.setMaxTemperature(1.0);
        ModelRequest warm = ModelRequest.builder()
                .user("hi")
                .params(GenerationParams.builder().temperature(0.7).build())
                .build();
        ModelRequest hot = warm.toBuilder()
                .params(GenerationParams.builder().temperature(1.5).build())
                .build();

        assertTrue(model.supportsRequest(warm));
        assertEquals(List.of("temperature 1.5 (at most 1.0)"), model.getUnsupportedRequirements(hot));
    }

    @Test
    void requirementsFollowTheRequestsContent() throws Exception {
        ModelData textOnly = model(List.of(), List.of(ModelInputType.TEXT));
        ModelRequest request = ModelRequest.builder()
                .systemPrompt("be brief")
                .message(Message.user(
                        new TextPart("describe"), new MediaPart(MediaPart.Kind.IMAGE, null, "image/png", new byte[1])))
                .build();

        assertEquals(
                List.of("input type IMAGE", "capability ADJUSTABLE_SYSTEM_PROMPT"),
                textOnly.getUnsupportedRequirements(request));
    }
}
