package io.ohmvir.plugins.jenkinsaisynapse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.ohmvir.plugins.jenkinsaisynapse.TestModels.SpecialTestConfiguration;
import io.ohmvir.plugins.jenkinsaisynapse.TestModels.SpecialTestProvider;
import io.ohmvir.plugins.jenkinsaisynapse.TestModels.TestConfiguration;
import io.ohmvir.plugins.jenkinsaisynapse.TestModels.TestProvider;
import io.ohmvir.plugins.jenkinsaisynapse.api.client.ModelClient;
import io.ohmvir.plugins.jenkinsaisynapse.api.models.ModelProvider;
import io.ohmvir.plugins.jenkinsaisynapse.configuration.models.ModelConfiguration;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ModelProviderTest {
    private static final ModelClient CLIENT = (model, request, inputs) -> List.of();

    @Test
    public void handlesSubclassesOfItsConfigurationType() throws Exception {
        TestProvider provider = new TestProvider(TestModels::textModel, CLIENT);
        SpecialTestConfiguration subclassed = new SpecialTestConfiguration("special");
        assertSame(provider, ModelProvider.forConfiguration(subclassed, List.of(provider)));
        assertEquals("test:special", provider.describeModel(subclassed).getModelId());
        assertSame(CLIENT, provider.createModelClient(subclassed));
    }

    @Test
    public void mostSpecificProviderWinsWhateverTheOrder() throws Exception {
        TestProvider general = new TestProvider(TestModels::textModel, CLIENT);
        SpecialTestProvider special = new SpecialTestProvider();
        SpecialTestConfiguration specialConfig = new SpecialTestConfiguration("special");
        TestConfiguration plainConfig = new TestConfiguration("plain");

        assertSame(special, ModelProvider.forConfiguration(specialConfig, List.of(general, special)));
        assertSame(special, ModelProvider.forConfiguration(specialConfig, List.of(special, general)));
        assertSame(general, ModelProvider.forConfiguration(plainConfig, List.of(special, general)));
    }

    @Test
    public void firstOfEquallySpecificProvidersWins() throws Exception {
        TestProvider first = new TestProvider(TestModels::textModel, CLIENT);
        TestProvider second = new TestProvider(TestModels::textModel, CLIENT);
        assertSame(first, ModelProvider.forConfiguration(new TestConfiguration("m"), List.of(first, second)));
    }

    @Test
    public void unhandledConfigurationsAreRejected() throws Exception {
        ModelConfiguration other = new ModelConfiguration("other", "other") {
            @Override
            public String getProviderType() {
                return "other";
            }
        };
        TestProvider provider = new TestProvider(TestModels::textModel, CLIENT);
        assertNull(ModelProvider.forConfiguration(other, List.of(provider)));
        assertThrows(IllegalArgumentException.class, () -> provider.createModelClient(other));
        assertThrows(IllegalArgumentException.class, () -> provider.describeModel(other));
    }
}
