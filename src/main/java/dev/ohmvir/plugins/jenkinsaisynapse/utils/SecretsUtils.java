package dev.ohmvir.plugins.jenkinsaisynapse.utils;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.Run;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Credential lookups on behalf of a build. Lookups are scoped to the credentials the build can see
 * (using the build's authentication) and their use is tracked against the build.
 */
public class SecretsUtils {
    /** Names of environment variables that are assumed to hold secrets, such as the ones bound by withCredentials. */
    private static final Pattern SENSITIVE_NAME =
            Pattern.compile("PASSWORD|PASSWD|SECRET|TOKEN|API_?KEY|CREDENTIAL|PRIVATE_?KEY", Pattern.CASE_INSENSITIVE);

    /**
     * Finds the secret values a build is using, to keep them out of the recorded conversations. These are the values
     * of password parameters and sensitive build variables, and of environment variables whose name suggests a secret
     * (for example {@code API_TOKEN}). A credential bound by withCredentials to a variable with an unrelated name is
     * not found.
     * @param run The build, if any
     * @param env The environment of the step, if any
     * @return The non-blank secret values
     */
    public static Set<String> getBuildSecrets(@Nullable Run<?, ?> run, @Nullable EnvVars env) {
        Set<String> secrets = new LinkedHashSet<>();
        if (run != null) {
            ParametersAction parameters = run.getAction(ParametersAction.class);
            if (parameters != null) {
                for (ParameterValue parameter : parameters.getAllParameters()) {
                    if (parameter.isSensitive()) {
                        EnvVars values = new EnvVars();
                        parameter.buildEnvironment(run, values);
                        secrets.addAll(values.values());
                    }
                }
            }
            if (run instanceof AbstractBuild<?, ?> build && env != null) {
                for (String name : build.getSensitiveBuildVariables()) {
                    secrets.add(env.get(name));
                }
            }
        }
        if (env != null) {
            for (Map.Entry<String, String> variable : env.entrySet()) {
                if (SENSITIVE_NAME.matcher(variable.getKey()).find()) {
                    secrets.add(variable.getValue());
                }
            }
        }
        secrets.removeIf(value -> value == null || value.isBlank());
        return secrets;
    }

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
