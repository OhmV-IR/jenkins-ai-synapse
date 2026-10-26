package dev.ohmvir.plugins.jenkinsaisynapse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.ohmvir.plugins.jenkinsaisynapse.api.skills.ReadSkillFileTool;
import dev.ohmvir.plugins.jenkinsaisynapse.api.skills.SkillData;
import dev.ohmvir.plugins.jenkinsaisynapse.api.tools.ToolRegistry;
import dev.ohmvir.plugins.jenkinsaisynapse.configuration.skills.SkillConfiguration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    private static void registerSkill() {
        SkillData.initializeSkillDataForConfig(new SkillConfiguration() {
            @Override
            public List<SkillData> generateSkills() {
                return List.of(SKILL);
            }
        });
    }

    @Test
    public void toolIsRegistered(JenkinsRule j) {
        assertInstanceOf(ReadSkillFileTool.class, ToolRegistry.getTool(ReadSkillFileTool.NAME));
    }

    @Test
    public void readsSkillFiles(JenkinsRule j) {
        registerSkill();
        ReadSkillFileTool tool = ReadSkillFileTool.get();
        String skillMd = tool.read_skill_file(SKILL.getSkillName(), ReadSkillFileTool.SKILL_FILE);
        assertTrue(skillMd.startsWith("Follow references/steps.md"));
        assertTrue(skillMd.contains("<skill_files>\nreferences/steps.md\n</skill_files>"));
        assertEquals("Step one", tool.read_skill_file(SKILL.getSkillName(), "./references/steps.md"));
        assertTrue(tool.read_skill_file(SKILL.getSkillName(), "missing.md").startsWith("No file"));
        assertTrue(tool.read_skill_file("no-such-skill", ReadSkillFileTool.SKILL_FILE)
                .startsWith("No skill"));
    }

    @Test
    public void callsToolFromModelArguments(JenkinsRule j) {
        registerSkill();
        ObjectNode arguments = new ObjectMapper().createObjectNode();
        arguments.put("skill_name", SKILL.getSkillName());
        arguments.put("file_path", "references/steps.md");
        assertEquals(
                Optional.of("Step one"),
                ToolRegistry.getTool(ReadSkillFileTool.NAME).callTool(arguments));
    }

    @Test
    public void listsSkillsInSystemPrompt(JenkinsRule j) {
        String prompt = ReadSkillFileTool.buildSkillsSystemPrompt(List.of(SKILL));
        assertTrue(prompt.contains("<name>" + SKILL.getSkillName() + "</name>"));
        assertTrue(prompt.contains(SKILL.getSkillDescription()));
        assertTrue(prompt.contains(ReadSkillFileTool.NAME));
    }
}
