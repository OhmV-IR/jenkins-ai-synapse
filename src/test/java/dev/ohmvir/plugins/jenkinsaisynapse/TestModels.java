package dev.ohmvir.plugins.jenkinsaisynapse;

import dev.ohmvir.plugins.jenkinsaisynapse.api.client.ModelClient;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelCapability;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelData;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelInputType;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelOutputType;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelProvider;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelThinkingLevel;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.RegisteredModel;
import dev.ohmvir.plugins.jenkinsaisynapse.configuration.models.ModelConfiguration;
import hudson.model.Descriptor;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Plain-Java model configurations and providers for unit tests. */
final class TestModels {
    private TestModels() {}

    static class TestConfiguration extends ModelConfiguration {
        TestConfiguration(String modelName) throws Descriptor.FormException {
            super(modelName, modelName);
        }

        @Override
        public String getProviderType() {
            return "test";
        }
    }

    /** A configuration subclass, as a provider plugin might add */
    static class SpecialTestConfiguration extends TestConfiguration {
        SpecialTestConfiguration(String modelName) throws Descriptor.FormException {
            super(modelName);
        }
    }

    static class TestProvider extends ModelProvider<TestConfiguration> {
        final Function<TestConfiguration, ModelData> describer;
        final ModelClient client;

        TestProvider(Function<TestConfiguration, ModelData> describer, ModelClient client) {
            super(TestConfiguration.class);
            this.describer = describer;
            this.client = client;
        }

        @Override
        protected ModelData describe(TestConfiguration configuration) {
            return describer.apply(configuration);
        }

        @Override
        protected ModelClient createClient(TestConfiguration configuration) {
            return client;
        }
    }

    static class SpecialTestProvider extends ModelProvider<SpecialTestConfiguration> {
        SpecialTestProvider() {
            super(SpecialTestConfiguration.class);
        }

        @Override
        protected ModelData describe(SpecialTestConfiguration configuration) {
            return textModel(configuration);
        }

        @Override
        protected ModelClient createClient(SpecialTestConfiguration configuration) {
            return (model, request, inputs) -> List.of();
        }
    }

    static ModelData textModel(ModelConfiguration configuration, ModelCapability... capabilities) {
        ModelData data = new ModelData(configuration);
        data.setInputs(new ArrayList<>(List.of(ModelInputType.TEXT)));
        data.setOutputs(new ArrayList<>(List.of(ModelOutputType.UNSTRUCTURED_TEXT)));
        data.setCapabilities(new ArrayList<>(List.of(capabilities)));
        return data;
    }

    static RegisteredModel registered(String name, ModelClient client, ModelCapability... capabilities)
            throws Descriptor.FormException {
        TestConfiguration configuration = new TestConfiguration(name);
        return new RegisteredModel(
                configuration,
                new TestProvider(config -> textModel(config, capabilities), client),
                textModel(configuration, capabilities));
    }

    static RegisteredModel thinkingModel(String name, ModelClient client, ModelThinkingLevel... levels)
            throws Descriptor.FormException {
        RegisteredModel model = registered(name, client, ModelCapability.THINKING);
        model.data().setSupportedThinkingLevels(new ArrayList<>(List.of(levels)));
        return model;
    }
}
