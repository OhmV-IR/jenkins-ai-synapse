package dev.ohmvir.plugins.jenkinsaisynapse.utils;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import hudson.model.Run;
import java.util.Map;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Credential lookups on behalf of a build. Lookups are scoped to the credentials the build can see
 * (using the build's authentication) and their use is tracked against the build.
 */
public class SecretsUtils {
    /**
     * Helper method to get a secret value (eg a api key) in plaintext.
     * @param credentialsId The credentials id for the value
     * @param run The build the value is being retrieved for
     * @return null if the credential is not found or not available to the build, otherwise its plaintext value.
     */
    public static @Nullable String getSecretText(String credentialsId, @NonNull Run<?, ?> run) {
        if (credentialsId == null || credentialsId.isBlank()) {
            return null;
        }

        StringCredentials credential =
                CredentialsProvider.findCredentialById(credentialsId, StringCredentials.class, run);
        if (credential == null) {
            return null;
        }
        return credential.getSecret().getPlainText();
    }

    /**
     * Helper method to get the username and password associated with a credential id in plaintext
     * @param credentialsId The credentials id
     * @param run The build the credential is being retrieved for
     * @return null if the credential is not found or not available to the build.
     */
    public static Map.Entry<String, String> getSecretUsernamePassword(String credentialsId, @NonNull Run<?, ?> run) {
        if (credentialsId == null || credentialsId.isBlank()) {
            return null;
        }

        StandardUsernamePasswordCredentials credential =
                CredentialsProvider.findCredentialById(credentialsId, StandardUsernamePasswordCredentials.class, run);
        if (credential == null) {
            return null;
        }
        return Map.entry(credential.getUsername(), credential.getPassword().getPlainText());
    }
}
