package dev.ohmvir.plugins.jenkinsaisynapse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelData;
import dev.ohmvir.plugins.jenkinsaisynapse.api.models.ModelDataRetriever;
import dev.ohmvir.plugins.jenkinsaisynapse.api.skills.SkillData;
import dev.ohmvir.plugins.jenkinsaisynapse.api.tools.Tool;
import dev.ohmvir.plugins.jenkinsaisynapse.api.tools.ToolArgumentDescription;
import dev.ohmvir.plugins.jenkinsaisynapse.api.tools.ToolRegistry;
import dev.ohmvir.plugins.jenkinsaisynapse.configuration.ModelsManagementLink;
import dev.ohmvir.plugins.jenkinsaisynapse.configuration.SkillsManagementLink;
import dev.ohmvir.plugins.jenkinsaisynapse.configuration.models.ModelConfiguration;
import dev.ohmvir.plugins.jenkinsaisynapse.configuration.skills.BasicSkillConfiguration;
import dev.ohmvir.plugins.jenkinsaisynapse.configuration.skills.BasicSkillFileConfiguration;
import hudson.model.Descriptor;
import hudson.util.ListBoxModel;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestExtension;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
public class ConfigurationCacheTest {
    @Test
    public void modelEditsAndDeletionsTakeEffectOnSave(JenkinsRule j) throws Exception {
        ModelsManagementLink link = ModelsManagementLink.get();
        link.setModelConfigurations(List.of(
                new TestModelConfiguration("a", "Model A", 1.0), new TestModelConfiguration("b", "Model B", 1.0)));
        assertEquals(1.0, ModelData.get("test:a").getMaxTemperature());
        assertNotNull(ModelData.get("test:b"));

        link.setModelConfigurations(List.of(new TestModelConfiguration("a", "Model A", 2.0)));
        assertEquals(2.0, ModelData.get("test:a").getMaxTemperature());
        assertNull(ModelData.get("test:b"));

        ListBoxModel models = ModelData.getAllModelsListBox();
        assertEquals(1, models.size());
        assertEquals("test:Model A", models.get(0).name);
        assertEquals("test:a", models.get(0).value);
    }

    @Test
    public void modelWithoutMetadataIsUnavailable(JenkinsRule j) throws Exception {
        ModelsManagementLink.get()
                .setModelConfigurations(List.of(
                        new UnsupportedModelConfiguration("x", "Model X"),
                        new TestModelConfiguration("a", "Model A", 1.0)));

        assertNull(ModelData.get("unsupported:x"));
        assertNotNull(ModelData.get("test:a"));
        assertEquals(2, ModelData.getAllModelsListBox().size());
    }

    @Test
    public void invalidAndDeletedSkillsAreDropped(JenkinsRule j) throws Exception {
        SkillsManagementLink link = SkillsManagementLink.get();
        link.setSkills(List.of(
                new BasicSkillFileConfiguration("not a valid skill file"),
                new BasicSkillConfiguration("Skill text", "skill-a", "Skill A")));
        assertNotNull(SkillData.getSkillData("skill-a"));
        assertEquals(1, SkillData.getAllSkills().size());

        link.setSkills(List.of());
        assertNull(SkillData.getSkillData("skill-a"));
        assertEquals(0, SkillData.getAllSkills().size());
    }

    @Test
    public void toolsAreLookedUpFromTheExtensionList(JenkinsRule j) {
        assertInstanceOf(EchoTool.class, ToolRegistry.getTool("echo"));
        assertEquals(
                1,
                ToolRegistry.getAllTools().stream()
                        .filter(tool -> tool.getName().equals("echo"))
                        .count());
        assertNull(ToolRegistry.getTool("missing"));
    }

    public static class TestModelConfiguration extends ModelConfiguration {
        private final double maxTemperature;

        public TestModelConfiguration(String modelName, String modelDisplayName, double maxTemperature)
                throws Descriptor.FormException {
            super(modelName, modelDisplayName);
            this.maxTemperature = maxTemperature;
        }

        @Override
        public String getProviderType() {
            return "test";
        }

        public double getMaxTemperature() {
            return maxTemperature;
        }
    }

    public static class UnsupportedModelConfiguration extends ModelConfiguration {
        public UnsupportedModelConfiguration(String modelName, String modelDisplayName)
                throws Descriptor.FormException {
            super(modelName, modelDisplayName);
        }

        @Override
        public String getProviderType() {
            return "unsupported";
        }
    }

    @TestExtension
    public static class TestModelDataRetriever extends ModelDataRetriever<TestModelConfiguration> {
        public TestModelDataRetriever() {
            super(TestModelConfiguration.class);
        }

        @Override
        public ModelData retrieveFromConfiguration(TestModelConfiguration configuration) {
            ModelData data = new ModelData(configuration);
            data.setMaxTemperature(configuration.getMaxTemperature());
            return data;
        }
    }

    @TestExtension("toolsAreLookedUpFromTheExtensionList")
    public static class EchoTool extends Tool {
        public EchoTool() throws NoSuchMethodException {
            super();
        }

        @Override
        public String getName() {
            return "echo";
        }

        @Override
        public String getDescription() {
            return "Returns a fixed reply";
        }

        @Override
        public List<ToolArgumentDescription> getArguments() {
            return List.of();
        }

        public String echo() {
            return "echo";
        }
    }
}
