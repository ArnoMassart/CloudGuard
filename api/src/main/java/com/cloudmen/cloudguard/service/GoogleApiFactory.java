package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.admin.directory.Directory;
import com.google.api.services.drive.Drive;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.ServiceAccountCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;

@Component
public class GoogleApiFactory {

    @Value("${google.api.client-email}")
    private String clientEmail;

    @Value("${google.api.private-key}")
    private String privateKey;

    private ServiceAccountCredentials getCredentials(Set<String> scopes, String loggedInEmail) throws Exception {
        String pk = privateKey.replace("\\n", "\n");

        return ServiceAccountCredentials.newBuilder()
                .setClientEmail(clientEmail)
                .setPrivateKey(GoogleServiceHelperMethods.decodePrivateKey(pk))
                .setServiceAccountUser(loggedInEmail)
                .setScopes(scopes)
                .build();
    }

    public Directory getDirectoryService(String scope, String loggedInEmail) throws Exception {
        return getDirectoryService(Collections.singleton(scope), loggedInEmail);
    }

    public Directory getDirectoryService(Set<String> scopes, String loggedInEmail) throws Exception {
        ServiceAccountCredentials credentials = getCredentials(scopes, loggedInEmail);

        return new Directory.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName("CloudGuard")
                .build();
    }

    public Drive getDriveService(String scope, String loggedInEmail) throws Exception {
        return getDriveService(Collections.singleton(scope), loggedInEmail);
    }

    public Drive getDriveService(Set<String> scopes, String loggedInEmail) throws Exception {
        ServiceAccountCredentials credentials = getCredentials(scopes, loggedInEmail);

        return new Drive.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName("CloudGuard")
                .build();
    }
}
