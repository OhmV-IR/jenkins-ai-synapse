package dev.ohmvir.plugins.jenkinsaisynapse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import dev.ohmvir.plugins.jenkinsaisynapse.api.skills.SkillData;
import dev.ohmvir.plugins.jenkinsaisynapse.configuration.skills.GithubFolderSkillConfiguration;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class GithubFolderSkillLoadingTest {
    private static Map<String, String> skillFiles(String name) {
        return Map.of("SKILL.md", "---\nname: " + name + "\ndescription: A skill\n---\nBody of " + name);
    }

    @Test
    public void badSkillFoldersDoNotDiscardTheOthers() {
        Map<String, Map<String, String>> folders = Map.of(
                "skills/first", skillFiles("first"),
                "skills/no-skill-md", Map.of("README.md", "no skill here"),
                "skills/bad-frontmatter", Map.of("SKILL.md", "---\nname: [unclosed\n---\nBody"),
                "skills/third", skillFiles("third"));
        List<String> paths = List.of(
                "skills/first", "skills/no-skill-md", "skills/unreadable", "skills/bad-frontmatter", "skills/third");

        List<SkillData> skills = GithubFolderSkillConfiguration.loadSkillFolders(paths, path -> {
            if (path.equals("skills/unreadable")) {
                throw new IOException("cannot read " + path);
            }
            return folders.get(path);
        });

        assertEquals(2, skills.size());
        assertEquals("first", skills.get(0).getSkillName());
        assertEquals("third", skills.get(1).getSkillName());
    }

    @Test
    public void validUtf8TextIsDecoded() {
        assertEquals(
                "héllo\n", GithubFolderSkillConfiguration.decodeTextFile("héllo\n".getBytes(StandardCharsets.UTF_8)));
        assertEquals("", GithubFolderSkillConfiguration.decodeTextFile(new byte[0]));
    }

    @Test
    public void binaryContentIsRejected() {
        assertNull(GithubFolderSkillConfiguration.decodeTextFile(new byte[] {'a', 0, 'b'}));
        // A PNG header: NUL-free but not valid UTF-8
        assertNull(GithubFolderSkillConfiguration.decodeTextFile(
                new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A}));
        // Invalid UTF-8 without any NUL byte
        assertNull(GithubFolderSkillConfiguration.decodeTextFile(new byte[] {'a', (byte) 0xC3, 0x28}));
    }
}
