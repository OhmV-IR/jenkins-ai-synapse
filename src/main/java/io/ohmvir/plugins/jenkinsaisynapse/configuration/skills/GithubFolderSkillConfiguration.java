package io.ohmvir.plugins.jenkinsaisynapse.configuration.skills;

import hudson.model.Descriptor;
import io.ohmvir.plugins.jenkinsaisynapse.api.skills.SkillData;
import io.ohmvir.plugins.jenkinsaisynapse.utils.SecretsUtils;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GithubFolderSkillConfiguration extends SkillConfiguration {
    private @Getter final String folderPath;
    private @Getter final String repositoryUrl;
    private @Getter final String authenticationTokenCredentialsId;
    private static final Logger LOGGER = Logger.getLogger(GithubFolderSkillConfiguration.class.getName());

    public GithubFolderSkillConfiguration(String configurationId, String folderPath, String repositoryUrl, String authenticationTokenCredentialsId) throws Descriptor.FormException {
        super(configurationId);
        this.folderPath = folderPath;
        this.repositoryUrl = repositoryUrl;
        this.authenticationTokenCredentialsId = authenticationTokenCredentialsId;
    }

    @Override
    protected List<SkillData> generateSkills() {
        List<SkillData> discoveredSkills = new ArrayList<>();

        try {
            GitHub github = createGitHubClient(this.authenticationTokenCredentialsId);
            String repoName = extractRepoName(this.repositoryUrl);
            GHRepository repository = github.getRepository(repoName);

            // Fetch top-level items in the base folder path
            List<GHContent> topLevelItems = repository.getDirectoryContent(this.folderPath);

            for (GHContent item : topLevelItems) {
                if (item.isDirectory()) {
                    String skillFolderBasePath = item.getPath();
                    Map<String, String> filesMap = new HashMap<>();
                    readDirectoryRecursively(repository, skillFolderBasePath, skillFolderBasePath, filesMap);
                    SkillData skill = new SkillData(filesMap);
                    discoveredSkills.add(skill);
                }
            }
        } catch (IOException | Descriptor.FormException e) {
            LOGGER.log(Level.SEVERE, "Failed to generate skills from GitHub folder: " + this.folderPath, e);
        }

        return discoveredSkills;
    }

    /**
     * Recursively walks subdirectories to collect file contents into a Map.
     *
     * @param repo The target GHRepository instance
     * @param currentPath Current folder path in GitHub
     * @param basePath Base directory path used to strip absolute prefix for relative keys
     * @param filesMap Output accumulator for relative-path -> file-content
     */
    private void readDirectoryRecursively(GHRepository repo, String currentPath, String basePath, Map<String, String> filesMap)
            throws IOException {

        List<GHContent> contents = repo.getDirectoryContent(currentPath);

        for (GHContent content : contents) {
            if (content.isDirectory()) {
                // Recurse down sub-folder
                readDirectoryRecursively(repo, content.getPath(), basePath, filesMap);
            } else if (content.isFile()) {
                // Compute relative path (e.g., "references/myfile.md" or "SKILL.md")
                String relativePath = content.getPath();
                if (relativePath.startsWith(basePath + "/")) {
                    relativePath = relativePath.substring(basePath.length() + 1);
                } else if (relativePath.equals(basePath)) {
                    relativePath = content.getName();
                }

                // Read file input stream into String
                try (InputStream is = content.read()) {
                    String textContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    filesMap.put(relativePath, textContent);
                }
            }
        }
    }

    private GitHub createGitHubClient(String credentialsId) throws IOException {
        String oauthTokens = SecretsUtils.getSecretText(credentialsId, null);
        if (oauthTokens != null) {
            return GitHub.connectUsingOAuth(oauthTokens);
        }
        // Fallback to unauthenticated / default client
        return GitHub.connectAnonymously();
    }

    /**
     * Helper to extract "owner/repo" from full URLs like "https://github.com/owner/repo.git"
     */
    private String extractRepoName(String url) {
        String cleaned = url.replace("https://github.com/", "").replace(".git", "");
        if (cleaned.endsWith("/")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        return cleaned;
    }

    public static class DescriptorImpl extends SkillConfiguration.DescriptorImpl {
        @Override
        public @NonNull String getDisplayName() {
            return "Github Skill Folder";
        }
    }
}
