package io.ohmvir.plugins.jenkinscr.utils;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;

import java.util.Collections;

public class SecretsUtils {
    public static String getSecretText(String credentialsId, Item context){
        if(credentialsId == null || credentialsId.isBlank()){
            return null;
        }

        StringCredentials credential = CredentialsMatchers.firstOrNull(
                CredentialsProvider.lookupCredentialsInItem(
                        StringCredentials.class,
                        context,
                        ACL.SYSTEM2
                ),
                CredentialsMatchers.withId(credentialsId)
        );
        if(credential == null){
            return null;
        }
        return credential.getSecret().getPlainText();
    }
}
