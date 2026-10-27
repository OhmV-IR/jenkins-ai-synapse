package dev.ohmvir.plugins.jenkinsaisynapse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import dev.ohmvir.plugins.jenkinsaisynapse.TestModels.TestConfiguration;
import dev.ohmvir.plugins.jenkinsaisynapse.TestModels.TestProvider;
import dev.ohmvir.plugins.jenkinsaisynapse.api.client.ModelClient;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.InputTextContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.ModelRequest;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.TemperatureContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.ThinkingLevelContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelRegistry;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelThinkingLevel;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.RegisteredModel;
import dev.ohmvir.plugins.jenkinsaisynapse.configuration.models.ModelConfiguration;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ModelRegistryTest {
    private static final ModelClient CLIENT = (model, request, inputs) -> List.of();

    private static ModelRequest textRequest() {
        ModelRequest request = new ModelRequest();
        request.addInput(new InputTextContent("hello"));
        return request;
    }

    @Test
    public void picksFirstSuitableModelInPreferenceOrder() throws Exception {
        RegisteredModel plain = TestModels.registered("plain", CLIENT);
        RegisteredModel tools = TestModels.registered("tools", CLIENT, ModelCapability.TOOLS);
        RegisteredModel moreTools = TestModels.registered("more-tools", CLIENT, ModelCapability.TOOLS);
        ModelRegistry registry = new ModelRegistry();
        registry.setModels(List.of(plain, tools, moreTools));

        assertSame(plain, registry.findModelForRequest(textRequest()));
        ModelRequest needsTools = textRequest();
        needsTools.requireCapability(ModelCapability.TOOLS);
        for (int i = 0; i < 10; i++) {
            assertSame(tools, registry.findModelForRequest(needsTools));
        }

        registry.setModels(List.of(moreTools, tools, plain));
        assertSame(moreTools, registry.findModelForRequest(needsTools));
    }

    @Test
    public void explicitlyNamedModelIsUsed() throws Exception {
        RegisteredModel first = TestModels.registered("first", CLIENT);
        RegisteredModel second = TestModels.registered("second", CLIENT);
        ModelRegistry registry = new ModelRegistry();
        registry.setModels(List.of(first, second));

        ModelRequest request = textRequest();
        request.setModelId("test:second");
        assertSame(second, registry.findModelForRequest(request));
    }

    @Test
    public void explicitlyNamedModelIsNotSwappedForAnother() throws Exception {
        RegisteredModel plain = TestModels.registered("plain", CLIENT);
        RegisteredModel tools = TestModels.registered("tools", CLIENT, ModelCapability.TOOLS);
        ModelRegistry registry = new ModelRegistry();
        registry.setModels(List.of(plain, tools));

        ModelRequest request = textRequest();
        request.requireCapability(ModelCapability.TOOLS);
        request.setModelId("test:plain");
        assertNull(registry.findModelForRequest(request));
        request.setModelId("test:missing");
        assertNull(registry.findModelForRequest(request));
    }

    @Test
    public void customCapabilitiesTakePartInSelection() throws Exception {
        ModelCapability computerUse = ModelCapability.of("vendor:computer-use");
        RegisteredModel plain = TestModels.registered("plain", CLIENT);
        RegisteredModel agent = TestModels.registered("agent", CLIENT, ModelCapability.of("vendor:computer-use"));
        ModelRegistry registry = new ModelRegistry();
        registry.setModels(List.of(plain, agent));

        ModelRequest request = textRequest();
        request.requireCapability(computerUse);
        assertSame(agent, registry.findModelForRequest(request));
    }

    @Test
    public void thinkingLevelMatchesNearestSupportedLevel() throws Exception {
        RegisteredModel noThinking = TestModels.registered("no-thinking", CLIENT);
        RegisteredModel onOnly = TestModels.thinkingModel("on-only", CLIENT, ModelThinkingLevel.ON);
        ModelRegistry registry = new ModelRegistry();
        registry.setModels(List.of(noThinking, onOnly));

        ModelRequest medium = textRequest();
        medium.addInput(new ThinkingLevelContent(ModelThinkingLevel.MEDIUM));
        assertSame(onOnly, registry.findModelForRequest(medium));

        ModelRequest off = textRequest();
        off.addInput(new ThinkingLevelContent(ModelThinkingLevel.OFF));
        assertSame(noThinking, registry.findModelForRequest(off));
    }

    @Test
    public void reloadKeepsConfigurationOrderAndSkipsBrokenModels() throws Exception {
        TestProvider provider = new TestProvider(
                config -> {
                    if (config.getModelName().equals("broken")) {
                        throw new IllegalStateException("API down");
                    }
                    return TestModels.textModel(config, ModelCapability.CUSTOM_TEMPERATURE);
                },
                CLIENT);
        ModelConfiguration unhandled = new ModelConfiguration("unhandled", "unhandled") {
            @Override
            public String getProviderType() {
                return "other";
            }
        };
        ModelRegistry registry = new ModelRegistry();
        registry.reload(
                List.of(
                        new TestConfiguration("b"),
                        new TestConfiguration("broken"),
                        unhandled,
                        new TestConfiguration("a"),
                        new TestConfiguration("b")),
                List.of(provider));

        assertEquals(
                List.of("test:b", "test:a"),
                registry.getModels().stream().map(RegisteredModel::getModelId).toList());
        assertSame(provider, registry.getModel("test:a").orElseThrow().provider());
    }

    @Test
    public void highestMaxTemperatureOnlyCountsModelsWithCustomTemperature() throws Exception {
        RegisteredModel fixed = TestModels.registered("fixed", CLIENT);
        fixed.data().setMaxTemperature(5.0);
        RegisteredModel custom = TestModels.registered("custom", CLIENT, ModelCapability.CUSTOM_TEMPERATURE);
        custom.data().setMaxTemperature(1.5);
        ModelRegistry registry = new ModelRegistry();
        registry.setModels(List.of(fixed, custom));
        assertEquals(1.5, registry.getHighestMaxTemperature());

        ModelRequest tooHot = textRequest();
        tooHot.addInput(new TemperatureContent(2.0));
        assertNull(registry.findModelForRequest(tooHot));
    }
}
