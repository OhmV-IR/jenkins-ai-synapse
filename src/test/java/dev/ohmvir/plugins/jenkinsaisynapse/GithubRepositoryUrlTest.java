package dev.ohmvir.plugins.jenkinsaisynapse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import dev.ohmvir.plugins.jenkinsaisynapse.configuration.skills.GithubFolderSkillConfiguration;
import org.junit.jupiter.api.Test;

public class GithubRepositoryUrlTest {
    private static String parse(String url) {
        return GithubFolderSkillConfiguration.extractRepoName(url);
    }

    @Test
    public void commonUrlFormsAreAccepted() {
        assertEquals("owner/repo", parse("https://github.com/owner/repo"));
        assertEquals("owner/repo", parse("https://github.com/owner/repo.git"));
        assertEquals("owner/repo", parse("https://github.com/owner/repo/"));
        assertEquals("owner/repo", parse("https://github.com/owner/repo.git/"));
        assertEquals("owner/repo", parse("http://github.com/owner/repo"));
        assertEquals("owner/repo", parse("https://www.github.com/owner/repo"));
        assertEquals("owner/repo", parse("  https://github.com/owner/repo  "));
        assertEquals("owner/repo", parse("git@github.com:owner/repo.git"));
        assertEquals("owner/repo", parse("ssh://git@github.com/owner/repo.git"));
    }

    @Test
    public void onlyTrailingDotGitIsStripped() {
        assertEquals("owner/owner.github.io", parse("https://github.com/owner/owner.github.io"));
        assertEquals("owner/my.gitops", parse("https://github.com/owner/my.gitops"));
        assertEquals("owner/my.git.repo", parse("https://github.com/owner/my.git.repo.git"));
    }

    @Test
    public void invalidUrlsAreRejected() {
        assertNull(parse(null));
        assertNull(parse(""));
        assertNull(parse("owner/repo"));
        assertNull(parse("https://github.com/owner"));
        assertNull(parse("https://github.com/owner/repo/tree/main"));
        assertNull(parse("https://github.example.com/owner/repo"));
        assertNull(parse("https://notgithub.com/owner/repo"));
        assertNull(parse("https://github.com/owner/.."));
        assertNull(parse("ftp://github.com/owner/repo"));
    }
}
