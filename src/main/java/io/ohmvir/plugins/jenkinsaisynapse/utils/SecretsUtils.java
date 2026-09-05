package io.ohmvir.plugins.jenkinsaisynapse.utils;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import hudson.model.Item;
import hudson.security.ACL;
import java.util.Map;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;

public class SecretsUtils {
    /**
     * Helper method to get a secret value (eg a api key) in plaintext.
     * @param credentialsId The credentials id for the value
     * @param context The item this value is being retrieved from
     * @return null if the credential is not found or its plaintext value.
     */
    public static String getSecretText(String credentialsId, Item context) {
        if (credentialsId == null || credentialsId.isBlank()) {
            return null;
        }

        StringCredentials credential = CredentialsMatchers.firstOrNull(
                CredentialsProvider.lookupCredentialsInItem(StringCredentials.class, context, ACL.SYSTEM2),
                CredentialsMatchers.withId(credentialsId));
        if (credential == null) {
            return null;
        }
        return credential.getSecret().getPlainText();
    }

    /**
     * Helper method to get the username and password associated with a credential id in plaintext
     * @param credentialsId The credentials id
     * @param context The item that the credential is being retrieved from.
     * @return Null if credential not found.
     */
    public static Map.Entry<String, String> getSecretUsernamePassword(String credentialsId, Item context) {
        if (credentialsId == null || credentialsId.isBlank()) {
            return null;
        }

        UsernamePasswordCredentials credential = CredentialsMatchers.firstOrNull(
                CredentialsProvider.lookupCredentialsInItem(UsernamePasswordCredentials.class, context, ACL.SYSTEM2),
                CredentialsMatchers.withId(credentialsId));
        if (credential == null) {
            return null;
        }
        return Map.entry(credential.getUsername(), credential.getPassword().getPlainText());
    }
}
