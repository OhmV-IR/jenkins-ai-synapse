package dev.ohmvir.plugins.jenkinsaisynapse.api.skills;

import dev.ohmvir.plugins.jenkinsaisynapse.api.tools.Tool;
import dev.ohmvir.plugins.jenkinsaisynapse.api.tools.ToolArgumentDescription;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.model.Descriptor;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.jspecify.annotations.NonNull;

/**
 * Built-in tool that lets any model with tool support use skills. The attached skills are listed by name and description
 * in the system prompt (see {@link #buildSkillsSystemPrompt(Collection)}), and the model calls this tool to load a
 * skill's SKILL.md and any of its reference files on demand.
 */
@Extension
public class ReadSkillFileTool extends Tool {
    public static final String NAME = "read_skill_file";
    public static final String SKILL_FILE = "SKILL.md";

    public ReadSkillFileTool() throws NoSuchMethodException {
        super();
    }

    public static ReadSkillFileTool get() {
        return ExtensionList.lookupSingleton(ReadSkillFileTool.class);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "Reads a file from one of the available skills. Use the path " + SKILL_FILE
                + " to load a skill's instructions, or a path relative to the skill root (eg references/guide.md)"
                + " to load a file the skill refers to.";
    }

    @Override
    public List<ToolArgumentDescription> getArguments() {
        return List.of(
                new ToolArgumentDescription(
                        "skill_name", String.class, "The name of the skill, as listed in the system prompt", true),
                new ToolArgumentDescription(
                        "file_path",
                        String.class,
                        "The path of the file relative to the skill root, " + SKILL_FILE + " for the main instructions",
                        true));
    }

    public String read_skill_file(String skillName, String filePath) {
        SkillData skill = SkillData.getSkillData(skillName);
        if (skill == null) {
            return "No skill named '" + skillName + "' exists. Available skills: "
                    + SkillData.getAllSkills().stream()
                            .map(SkillData::getSkillName)
                            .sorted()
                            .collect(Collectors.joining(", "));
        }
        String path = normalizePath(filePath);
        if (path.isEmpty() || path.equals(SKILL_FILE)) {
            if (skill.getSkillReferences().isEmpty()) {
                return skill.getSkillText();
            }
            return skill.getSkillText() + "\n\n<skill_files>\n" + listFiles(skill) + "\n</skill_files>";
        }
        String content = skill.getSkillReferences().get(path);
        if (content == null) {
            return "No file '" + path + "' exists in skill '" + skillName + "'. Available files:\n" + SKILL_FILE
                    + (skill.getSkillReferences().isEmpty() ? "" : "\n" + listFiles(skill));
        }
        return content;
    }

    /**
     * @param skills The skills attached to a request
     * @return A system prompt section listing the skills and explaining how to load them with this tool
     */
    public static String buildSkillsSystemPrompt(Collection<SkillData> skills) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("<available_skills>\n")
                .append("Skills provide specialized instructions for particular tasks. When a task matches a skill's")
                .append(" description, call the ")
                .append(NAME)
                .append(" tool with the skill's name and the file path ")
                .append(SKILL_FILE)
                .append(" to load its instructions before acting, then follow them. If the instructions refer to")
                .append(" other files in the skill, load them with the same tool using their path relative to the")
                .append(" skill root.\n");
        for (SkillData skill : skills) {
            prompt.append("<skill>\n<name>")
                    .append(skill.getSkillName())
                    .append("</name>\n<description>")
                    .append(skill.getSkillDescription())
                    .append("</description>\n</skill>\n");
        }
        prompt.append("</available_skills>");
        return prompt.toString();
    }

    private static String listFiles(SkillData skill) {
        return skill.getSkillReferences().keySet().stream().sorted().collect(Collectors.joining("\n"));
    }

    private static String normalizePath(String filePath) {
        if (filePath == null) {
            return "";
        }
        String path = filePath.trim().replace('\\', '/');
        while (path.startsWith("./") || path.startsWith("/")) {
            path = path.substring(path.startsWith("./") ? 2 : 1);
        }
        return path;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<Tool> {
        @Override
        public @NonNull String getDisplayName() {
            return "Read Skill File Tool";
        }
    }
}
