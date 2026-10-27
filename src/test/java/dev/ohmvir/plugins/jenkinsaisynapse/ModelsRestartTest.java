package dev.ohmvir.plugins.jenkinsaisynapse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.model.Descriptor;
import dev.ohmvir.plugins.jenkinsaisynapse.api.client.ModelClient;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelData;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelProvider;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelRegistry;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.RegisteredModel;
import dev.ohmvir.plugins.jenkinsaisynapse.configuration.ModelsConfiguration;
import dev.ohmvir.plugins.jenkinsaisynapse.configuration.models.ModelConfiguration;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.jenkinsci.Symbol;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestExtension;
import org.jvnet.hudson.test.junit.jupiter.JenkinsSessionExtension;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * The models must still be there, in {@link ModelsConfiguration} and in the {@link ModelRegistry}, after Jenkins
 * restarts, whichever way they were configured.
 */
public class ModelsRestartTest {
    @RegisterExtension
    private final JenkinsSessionExtension sessions = new JenkinsSessionExtension();

    @Test
    public void modelsSetInCodeSurviveRestart() throws Throwable {
        sessions.then(j -> {
            ModelsConfiguration.get().setModels(List.of(new SessionModelConfiguration("alpha", "Alpha")));
            forgetRegisteredModels();
        });
        sessions.then(j -> assertModels(List.of("session:alpha")));
    }

    @Test
    public void modelsSavedFromManagementPageSurviveRestart() throws Throwable {
        sessions.then(j -> {
            ModelsConfiguration.get()
                    .setModels(List.of(
                            new SessionModelConfiguration("alpha", "Alpha"),
                            new SessionModelConfiguration("beta", "Beta")));
            try (JenkinsRule.WebClient wc = j.createWebClient()) {
                j.submit(wc.goTo("manage/models/").getFormByName("config"));
            }
            assertModels(List.of("session:alpha", "session:beta"));
            forgetRegisteredModels();
        });
        sessions.then(j -> {
            assertModels(List.of("session:alpha", "session:beta"));
            // The browser leaves the field out when the last model is removed
            ModelsConfiguration.get().setModels(List.of());
            try (JenkinsRule.WebClient wc = j.createWebClient()) {
                j.submit(wc.goTo("manage/models/").getFormByName("config"));
            }
            assertModels(List.of());
        });
        sessions.then(j -> assertModels(List.of()));
    }

    @Test
    public void legacyModelsFileIsMigratedAndSurvivesRestart() throws Throwable {
        sessions.then(j -> {
            File root = j.jenkins.getRootDir();
            Files.deleteIfExists(new File(root, ModelsConfiguration.class.getName() + ".xml").toPath());
            String modelClass = SessionModelConfiguration.class.getName().replace("$", "_-");
            String legacyXml = """
                    <?xml version='1.1' encoding='UTF-8'?>
                    <dev.ohmvir.plugins.jenkinsaisynapse.configuration.ModelsManagementLink>
                      <modelConfigurations>
                        <%1$s>
                          <modelName>legacy</modelName>
                          <modelDisplayName>Legacy</modelDisplayName>
                        </%1$s>
                      </modelConfigurations>
                    </dev.ohmvir.plugins.jenkinsaisynapse.configuration.ModelsManagementLink>
                    """.formatted(modelClass);
            Files.writeString(new File(root, "models.xml").toPath(), legacyXml);
            forgetRegisteredModels();
        });
        sessions.then(j -> {
            assertModels(List.of("session:legacy"));
            assertTrue(new File(j.jenkins.getRootDir(), "models.xml.migrated").exists());
        });
        sessions.then(j -> assertModels(List.of("session:legacy")));
    }

    /**
     * The registry is static, so it would still hold the models in the next Jenkins session if it were not emptied.
     */
    private static void forgetRegisteredModels() {
        ModelRegistry.get().setModels(List.of());
    }

    private static void assertModels(List<String> modelIds) throws InterruptedException {
        assertEquals(
                modelIds,
                ModelsConfiguration.get().getModels().stream()
                        .map(ModelConfiguration::getModelId)
                        .toList());
        // The registry is loaded in the background after startup, so give it time to catch up
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        while (!registeredModelIds().equals(modelIds) && System.nanoTime() < deadline) {
            Thread.sleep(50);
        }
        assertEquals(modelIds, registeredModelIds());
    }

    private static List<String> registeredModelIds() {
        return ModelRegistry.get().getModels().stream()
                .map(RegisteredModel::getModelId)
                .toList();
    }

    public static class SessionModelConfiguration extends ModelConfiguration {
        @DataBoundConstructor
        public SessionModelConfiguration(String modelName, String modelDisplayName) throws Descriptor.FormException {
            super(modelName, modelDisplayName);
        }

        @Override
        public String getProviderType() {
            return "session";
        }

        @TestExtension
        @Symbol("sessionModel")
        public static class DescriptorImpl extends ModelConfiguration.DescriptorImpl {}
    }

    @TestExtension
    public static class SessionProvider extends ModelProvider<SessionModelConfiguration> {
        public SessionProvider() {
            super(SessionModelConfiguration.class);
        }

        @Override
        protected ModelData describe(SessionModelConfiguration configuration) {
            return new ModelData(configuration);
        }

        @Override
        protected ModelClient createClient(SessionModelConfiguration configuration) {
            return (model, request, inputs) -> List.of();
        }
    }
}
