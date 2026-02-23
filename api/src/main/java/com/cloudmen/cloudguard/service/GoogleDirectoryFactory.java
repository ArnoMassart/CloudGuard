package com.cloudmen.cloudguard.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.admin.directory.Directory;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.ServiceAccountCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;

import static com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods.decodePrivateKey;

@Component
public class GoogleDirectoryFactory {

    @Value("${google.api.client-email}")
    private String clientEmail;

    @Value("${google.api.private-key}")
    private String privateKey;

    public Directory getDirectoryService(String scope, String loggedInEmail) throws Exception {
        return getDirectoryService(Collections.singleton(scope), loggedInEmail);
    }

    public Directory getDirectoryService(Set<String> scopes, String loggedInEmail) throws Exception {
        String pk = privateKey.replace("\\n", "\n");

        ServiceAccountCredentials credentials = ServiceAccountCredentials.newBuilder()
                .setClientEmail(clientEmail)
                .setPrivateKey(decodePrivateKey(pk))
                .setServiceAccountUser(loggedInEmail)
                .setScopes(scopes)
                .build();

        return new Directory.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName("CloudGuard")
                .build();
    }
}
