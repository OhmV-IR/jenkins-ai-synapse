package dev.ohmvir.plugins.jenkinsaisynapse.configuration.skills;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import dev.ohmvir.plugins.jenkinsaisynapse.api.skills.SkillData;
import hudson.Extension;
import hudson.model.Descriptor;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jenkins.model.Jenkins;
import lombok.Getter;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.jspecify.annotations.NonNull;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

public class GithubFolderSkillConfiguration extends SkillConfiguration {
    private @Getter final String folderPath;
    private @Getter final String repositoryUrl;
    private @Getter final String authenticationTokenCredentialsId;
    private static final Logger LOGGER = Logger.getLogger(GithubFolderSkillConfiguration.class.getName());

    /**
     * Accepts https/http (with or without "www.") and SSH forms of a github.com repository URL, with an optional
     * trailing ".git" and slash. Group 1 is the owner and group 2 the repository name. GitHub Enterprise hosts are not
     * accepted because the client is always connected to github.com.
     */
    private static final Pattern REPOSITORY_URL_PATTERN = Pattern.compile(
            "^(?:https?://(?:www\\.)?github\\.com/|git@github\\.com:|ssh://git@github\\.com/)"
                    + "([A-Za-z0-9_.-]+)/([A-Za-z0-9_.-]+?)(?:\\.git)?/?$",
            Pattern.CASE_INSENSITIVE);

    @DataBoundConstructor
    public GithubFolderSkillConfiguration(
            String folderPath, String repositoryUrl, String authenticationTokenCredentialsId)
            throws Descriptor.FormException {
        this.folderPath = folderPath;
        this.repositoryUrl = repositoryUrl;
        this.authenticationTokenCredentialsId = authenticationTokenCredentialsId;
    }

    @Override
    public List<SkillData> generateSkills() {
        List<SkillData> discoveredSkills = new ArrayList<>();

        try {
            GitHub github = createGitHubClient(this.authenticationTokenCredentialsId);
            String repoName = extractRepoName(this.repositoryUrl);
            if (repoName == null) {
                throw new IOException("Invalid GitHub repository URL: " + this.repositoryUrl);
            }
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
    private void readDirectoryRecursively(
            GHRepository repo, String currentPath, String basePath, Map<String, String> filesMap) throws IOException {

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
        String oauthTokens = getGlobalSecretText(credentialsId);
        if (oauthTokens != null) {
            return GitHub.connectUsingOAuth(oauthTokens);
        }
        // Fallback to unauthenticated / default client
        return GitHub.connectAnonymously();
    }

    /**
     * Skill sources are global configuration, so the token is looked up among the credentials defined at the Jenkins
     * root, never on behalf of a job.
     */
    private static String getGlobalSecretText(String credentialsId) {
        if (credentialsId == null || credentialsId.isBlank()) {
            return null;
        }
        StringCredentials credential = CredentialsMatchers.firstOrNull(
                CredentialsProvider.lookupCredentialsInItemGroup(
                        StringCredentials.class, Jenkins.get(), ACL.SYSTEM2, List.of()),
                CredentialsMatchers.withId(credentialsId));
        return credential == null ? null : credential.getSecret().getPlainText();
    }

    /**
     * Extracts "owner/repo" from a github.com repository URL such as "https://github.com/owner/repo.git". Only a
     * trailing ".git" and slash are stripped, so names like "owner.github.io" are preserved.
     *
     * @return the "owner/repo" name, or null if the URL is not a supported GitHub repository URL
     */
    public static String extractRepoName(String url) {
        if (url == null) {
            return null;
        }
        Matcher matcher = REPOSITORY_URL_PATTERN.matcher(url.trim());
        if (!matcher.matches()) {
            return null;
        }
        String owner = matcher.group(1);
        String repo = matcher.group(2);
        if (owner.equals(".") || owner.equals("..") || repo.equals(".") || repo.equals("..")) {
            return null;
        }
        return owner + "/" + repo;
    }

    @Extension
    @Symbol("githubFolder")
    public static class DescriptorImpl extends SkillConfiguration.DescriptorImpl {
        @Override
        public @NonNull String getDisplayName() {
            return "Github Skill Folder";
        }

        @POST
        public FormValidation doCheckRepositoryUrl(@QueryParameter String value) {
            Jenkins.get().checkPermission(Jenkins.ADMINISTER);
            if (value == null || value.isBlank()) {
                return FormValidation.error("Repository URL is required");
            }
            if (extractRepoName(value) == null) {
                return FormValidation.error(
                        "Not a valid github.com repository URL, expected https://github.com/owner/repo");
            }
            return FormValidation.ok();
        }

        public ListBoxModel doFillAuthenticationTokenCredentialsIdItems(
                @QueryParameter String authenticationTokenCredentialsId) {
            StandardListBoxModel items = new StandardListBoxModel();
            if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
                return items.includeCurrentValue(authenticationTokenCredentialsId);
            }
            return items.includeEmptyValue()
                    .includeMatchingAs(
                            ACL.SYSTEM2,
                            Jenkins.get(),
                            StringCredentials.class,
                            List.of(),
                            CredentialsMatchers.always())
                    .includeCurrentValue(authenticationTokenCredentialsId);
        }
    }
}
