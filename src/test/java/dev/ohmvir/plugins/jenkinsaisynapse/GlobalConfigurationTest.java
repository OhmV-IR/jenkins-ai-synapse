package dev.ohmvir.plugins.jenkinsaisynapse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.ohmvir.plugins.jenkinsaisynapse.api.input.ModelRequest;
import dev.ohmvir.plugins.jenkinsaisynapse.configuration.PromptsConfiguration;
import dev.ohmvir.plugins.jenkinsaisynapse.configuration.RequestsConfiguration;
import dev.ohmvir.plugins.jenkinsaisynapse.configuration.SkillsConfiguration;
import dev.ohmvir.plugins.jenkinsaisynapse.configuration.prompts.PromptConfiguration;
import dev.ohmvir.plugins.jenkinsaisynapse.configuration.prompts.SimplePrompt;
import dev.ohmvir.plugins.jenkinsaisynapse.configuration.skills.BasicSkillConfiguration;
import hudson.BulkChange;
import hudson.util.FormValidation;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
public class GlobalConfigurationTest {

    @Test
    public void replacingPromptsRemovesOldOnes(JenkinsRule j) throws Exception {
        PromptsConfiguration config = PromptsConfiguration.get();
        config.setPrompts(List.of(new SimplePrompt("a", "first"), new SimplePrompt("b", "second")));
        config.setPrompts(List.of(new SimplePrompt("a", "first")));
        config.load();
        assertEquals(List.of("a"), promptIds(config));
    }

    @Test
    public void duplicatePromptIdsAreRejected(JenkinsRule j) {
        PromptsConfiguration config = PromptsConfiguration.get();
        assertThrows(
                IllegalArgumentException.class,
                () -> config.setPrompts(List.of(new SimplePrompt("a", "first"), new SimplePrompt("a", "second"))));
    }

    @Test
    public void bulkChangeDefersSaving(JenkinsRule j) throws Exception {
        PromptsConfiguration config = PromptsConfiguration.get();
        File file = new File(j.jenkins.getRootDir(), PromptsConfiguration.class.getName() + ".xml");
        Files.deleteIfExists(file.toPath());
        try (BulkChange change = new BulkChange(config)) {
            config.setPrompts(List.of(new SimplePrompt("a", "first")));
            assertFalse(file.exists(), "Saved during a BulkChange");
            change.commit();
        }
        assertTrue(file.exists(), "Not saved when the BulkChange was committed");
    }

    @Test
    public void managementPageSavesPrompts(JenkinsRule j) throws Exception {
        PromptsConfiguration config = PromptsConfiguration.get();
        config.setPrompts(List.of(new SimplePrompt("a", "first"), new SimplePrompt("b", "second")));
        try (JenkinsRule.WebClient wc = j.createWebClient()) {
            j.submit(wc.goTo("manage/prompts/").getFormByName("config"));
        }
        assertEquals(List.of("a", "b"), promptIds(config));

        config.setPrompts(List.of());
        try (JenkinsRule.WebClient wc = j.createWebClient()) {
            j.submit(wc.goTo("manage/prompts/").getFormByName("config"));
        }
        assertEquals(List.of(), promptIds(config));
    }

    @Test
    public void legacyPromptsFileIsMigrated(JenkinsRule j) throws Exception {
        File root = j.jenkins.getRootDir();
        Files.deleteIfExists(new File(root, PromptsConfiguration.class.getName() + ".xml").toPath());
        String legacyXml = """
                <?xml version='1.1' encoding='UTF-8'?>
                <dev.ohmvir.plugins.jenkinsaisynapse.configuration.BasePromptsManagementLink>
                  <prompts>
                    <entry>
                      <string>greet</string>
                      <dev.ohmvir.plugins.jenkinsaisynapse.configuration.prompts.SimplePrompt>
                        <promptId>greet</promptId>
                        <promptText>Say hello</promptText>
                      </dev.ohmvir.plugins.jenkinsaisynapse.configuration.prompts.SimplePrompt>
                    </entry>
                  </prompts>
                </dev.ohmvir.plugins.jenkinsaisynapse.configuration.BasePromptsManagementLink>
                """;
        Files.writeString(new File(root, "prompts.xml").toPath(), legacyXml);

        PromptsConfiguration migrated = new PromptsConfiguration();

        assertEquals(List.of("greet"), promptIds(migrated));
        assertEquals("Say hello", ((SimplePrompt) migrated.getPrompt("greet")).getPromptText());
        assertFalse(new File(root, "prompts.xml").exists());
        assertTrue(new File(root, "prompts.xml.migrated").exists());
        assertEquals(List.of("greet"), promptIds(new PromptsConfiguration()));
    }

    @Test
    public void legacySkillsFileIsMigrated(JenkinsRule j) throws Exception {
        File root = j.jenkins.getRootDir();
        Files.deleteIfExists(new File(root, SkillsConfiguration.class.getName() + ".xml").toPath());
        String legacyXml = """
                <?xml version='1.1' encoding='UTF-8'?>
                <dev.ohmvir.plugins.jenkinsaisynapse.configuration.SkillsManagementLink>
                  <skillConfigurations>
                    <dev.ohmvir.plugins.jenkinsaisynapse.configuration.skills.BasicSkillConfiguration>
                      <skillText>Review the code</skillText>
                      <skillName>reviewer</skillName>
                      <skillDescription>Reviews code</skillDescription>
                    </dev.ohmvir.plugins.jenkinsaisynapse.configuration.skills.BasicSkillConfiguration>
                  </skillConfigurations>
                </dev.ohmvir.plugins.jenkinsaisynapse.configuration.SkillsManagementLink>
                """;
        Files.writeString(new File(root, "skills.xml").toPath(), legacyXml);

        SkillsConfiguration migrated = new SkillsConfiguration();

        assertEquals(1, migrated.getSkills().size());
        assertEquals("reviewer", ((BasicSkillConfiguration) migrated.getSkills().get(0)).skillName);
        assertTrue(new File(root, "skills.xml.migrated").exists());
    }

    @Test
    public void systemConfigurationPageStillSaves(JenkinsRule j) throws Exception {
        j.configRoundtrip();
    }

    @Test
    public void maxStepsDefaultsTo25AndIsValidated(JenkinsRule j) {
        RequestsConfiguration config = RequestsConfiguration.get();
        assertEquals(25, config.getMaxSteps());
        assertThrows(IllegalArgumentException.class, () -> config.setMaxSteps(0));
        assertThrows(IllegalArgumentException.class, () -> config.setMaxSteps(-3));
        assertEquals(25, config.getMaxSteps());
        assertEquals(FormValidation.Kind.OK, config.doCheckMaxSteps("10").kind);
        assertEquals(FormValidation.Kind.ERROR, config.doCheckMaxSteps("0").kind);
        assertEquals(FormValidation.Kind.ERROR, config.doCheckMaxSteps("").kind);
        assertEquals(FormValidation.Kind.ERROR, config.doCheckMaxSteps("many").kind);
    }

    @Test
    public void maxStepsIsPersistedAndUsedByRequests(JenkinsRule j) throws Exception {
        RequestsConfiguration config = RequestsConfiguration.get();
        config.setMaxSteps(12);
        assertEquals(12, new ModelRequest().getMaxSteps());
        assertEquals(12, new RequestsConfiguration().getMaxSteps());
    }

    @Test
    public void missingOrInvalidStoredMaxStepsFallsBackTo25(JenkinsRule j) throws Exception {
        File file = new File(j.jenkins.getRootDir(), RequestsConfiguration.class.getName() + ".xml");
        Files.deleteIfExists(file.toPath());
        assertEquals(25, new RequestsConfiguration().getMaxSteps());

        Files.writeString(file.toPath(), """
                <?xml version='1.1' encoding='UTF-8'?>
                <dev.ohmvir.plugins.jenkinsaisynapse.configuration.RequestsConfiguration>
                  <maxSteps>0</maxSteps>
                </dev.ohmvir.plugins.jenkinsaisynapse.configuration.RequestsConfiguration>
                """);
        assertEquals(25, new RequestsConfiguration().getMaxSteps());
        Files.deleteIfExists(file.toPath());
    }

    private static List<String> promptIds(PromptsConfiguration config) {
        return config.getPrompts().stream()
                .map(PromptConfiguration::getPromptId)
                .toList();
    }
}
