package dev.ohmvir.plugins.jenkinsaisynapse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.ohmvir.plugins.jenkinsaisynapse.api.input.ToolCallResponseContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.output.ToolCallContent;
import dev.ohmvir.plugins.jenkinsaisynapse.api.skills.ReadSkillFileTool;
import dev.ohmvir.plugins.jenkinsaisynapse.api.skills.SkillData;
import dev.ohmvir.plugins.jenkinsaisynapse.api.tools.ToolRegistry;
import dev.ohmvir.plugins.jenkinsaisynapse.configuration.skills.SkillConfiguration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
public class ReadSkillFileToolTest {
    private static final SkillData SKILL = new SkillData(
            "read-skill-file-test",
            "A skill used to test the read_skill_file tool",
            null,
            null,
            null,
            null,
            "Follow references/steps.md",
            Map.of("references/steps.md", "Step one"));

    private static final SkillData OTHER_SKILL = new SkillData(
            "read-skill-file-other", "A skill that is not attached", null, null, null, null, "Other text", Map.of());

    private static void registerSkills() {
        SkillData.reloadSkills(List.of(new SkillConfiguration() {
            @Override
            public List<SkillData> generateSkills() {
                return List.of(SKILL, OTHER_SKILL);
            }
        }));
    }

    @Test
    public void toolIsRegistered(JenkinsRule j) {
        assertInstanceOf(ReadSkillFileTool.class, ToolRegistry.getTool(ReadSkillFileTool.NAME));
    }

    @Test
    public void readsSkillFiles(JenkinsRule j) {
        registerSkills();
        ReadSkillFileTool tool = ReadSkillFileTool.forSkills(List.of(SKILL));
        String skillMd = tool.read_skill_file(SKILL.getSkillName(), ReadSkillFileTool.SKILL_FILE);
        assertTrue(skillMd.startsWith("Follow references/steps.md"));
        assertTrue(skillMd.contains("<skill_files>\nreferences/steps.md\n</skill_files>"));
        assertEquals("Step one", tool.read_skill_file(SKILL.getSkillName(), "./references/steps.md"));
        assertTrue(tool.read_skill_file(SKILL.getSkillName(), "missing.md").startsWith("No file"));
        assertTrue(tool.read_skill_file("no-such-skill", ReadSkillFileTool.SKILL_FILE)
                .startsWith("No skill"));
    }

    @Test
    public void refusesConfiguredSkillsThatAreNotAttached(JenkinsRule j) {
        registerSkills();
        ReadSkillFileTool tool = ReadSkillFileTool.forSkills(List.of(SKILL));
        String refused = tool.read_skill_file(OTHER_SKILL.getSkillName(), ReadSkillFileTool.SKILL_FILE);
        assertTrue(refused.startsWith("No skill"));
        assertFalse(refused.contains("Other text"));
        // Only the attached skills are reported, not every configured skill
        assertTrue(refused.endsWith("Available skills: " + SKILL.getSkillName()));
    }

    @Test
    public void registeredToolServesNoSkills(JenkinsRule j) {
        registerSkills();
        ReadSkillFileTool tool = (ReadSkillFileTool) ToolRegistry.getTool(ReadSkillFileTool.NAME);
        assertTrue(tool.read_skill_file(SKILL.getSkillName(), ReadSkillFileTool.SKILL_FILE)
                .startsWith("No skills are available"));
    }

    @Test
    public void callsToolFromModelArguments(JenkinsRule j) {
        registerSkills();
        ObjectNode arguments = new ObjectMapper().createObjectNode();
        arguments.put("skill_name", SKILL.getSkillName());
        arguments.put("file_path", "references/steps.md");
        ToolCallResponseContent response = ReadSkillFileTool.forSkills(List.of(SKILL))
                .callTool(new ToolCallContent("call-1", arguments, ReadSkillFileTool.NAME));
        assertTrue(response.isSuccessful());
        assertEquals("Step one", response.getResponseContent());
    }

    @Test
    public void listsSkillsInSystemPrompt(JenkinsRule j) {
        String prompt = ReadSkillFileTool.buildSkillsSystemPrompt(List.of(SKILL));
        assertTrue(prompt.contains("<name>" + SKILL.getSkillName() + "</name>"));
        assertTrue(prompt.contains(SKILL.getSkillDescription()));
        assertTrue(prompt.contains(ReadSkillFileTool.NAME));
    }
}
