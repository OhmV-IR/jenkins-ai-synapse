package io.ohmvir.plugins.jenkinsaisynapse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.BulkChange;
import hudson.ExtensionList;
import hudson.model.ManagementLink;
import io.ohmvir.plugins.jenkinsaisynapse.configuration.ConversationHistoryManagementLink;
import io.ohmvir.plugins.jenkinsaisynapse.configuration.PromptsConfiguration;
import io.ohmvir.plugins.jenkinsaisynapse.configuration.SkillsConfiguration;
import io.ohmvir.plugins.jenkinsaisynapse.configuration.prompts.PromptConfiguration;
import io.ohmvir.plugins.jenkinsaisynapse.configuration.prompts.SimplePrompt;
import io.ohmvir.plugins.jenkinsaisynapse.configuration.skills.BasicSkillConfiguration;
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
                <io.ohmvir.plugins.jenkinsaisynapse.configuration.BasePromptsManagementLink>
                  <prompts>
                    <entry>
                      <string>greet</string>
                      <io.ohmvir.plugins.jenkinsaisynapse.configuration.prompts.SimplePrompt>
                        <promptId>greet</promptId>
                        <promptText>Say hello</promptText>
                      </io.ohmvir.plugins.jenkinsaisynapse.configuration.prompts.SimplePrompt>
                    </entry>
                  </prompts>
                </io.ohmvir.plugins.jenkinsaisynapse.configuration.BasePromptsManagementLink>
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
                <io.ohmvir.plugins.jenkinsaisynapse.configuration.SkillsManagementLink>
                  <skillConfigurations>
                    <io.ohmvir.plugins.jenkinsaisynapse.configuration.skills.BasicSkillConfiguration>
                      <skillText>Review the code</skillText>
                      <skillName>reviewer</skillName>
                      <skillDescription>Reviews code</skillDescription>
                    </io.ohmvir.plugins.jenkinsaisynapse.configuration.skills.BasicSkillConfiguration>
                  </skillConfigurations>
                </io.ohmvir.plugins.jenkinsaisynapse.configuration.SkillsManagementLink>
                """;
        Files.writeString(new File(root, "skills.xml").toPath(), legacyXml);

        SkillsConfiguration migrated = new SkillsConfiguration();

        assertEquals(1, migrated.getSkills().size());
        assertEquals("reviewer", ((BasicSkillConfiguration) migrated.getSkills().get(0)).skillName);
        assertTrue(new File(root, "skills.xml.migrated").exists());
    }

    @Test
    public void conversationHistoryPageIsRegistered(JenkinsRule j) throws Exception {
        assertTrue(ExtensionList.lookup(ManagementLink.class).stream()
                .anyMatch(ConversationHistoryManagementLink.class::isInstance));
        try (JenkinsRule.WebClient wc = j.createWebClient()) {
            wc.goTo("manage/conversations/");
        }
    }

    @Test
    public void systemConfigurationPageStillSaves(JenkinsRule j) throws Exception {
        j.configRoundtrip();
    }

    private static List<String> promptIds(PromptsConfiguration config) {
        return config.getPrompts().stream()
                .map(PromptConfiguration::getPromptId)
                .toList();
    }
}
