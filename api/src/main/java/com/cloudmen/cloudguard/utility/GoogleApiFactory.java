package com.cloudmen.cloudguard.utility;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.admin.directory.Directory;
import com.google.api.services.cloudidentity.v1.CloudIdentity;
import com.google.api.services.drive.Drive;
import com.google.api.services.licensing.Licensing;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.ServiceAccountCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;

import static com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods.decodePrivateKey;

/**
 * Factory component for initializing various Google API Client services. <p>
 *
 * This factory utilizes a Google Service Account to interact with the Google Workspace APIs. It implements
 * <b>Domain-Wide Delegation</b>, allowing the service account to impersonate an administrative user (the
 * {@code organizationAdminEmail} to perform tasks across the entire domain.
 */
@Component
public class GoogleApiFactory {

    @Value("${google.api.client-email}")
    private String clientEmail;

    @Value("${google.api.private-key}")
    private String privateKey;

    /**
     * Constructs the ServiceAccountCredentials required for Google API authentication.
     *
     * @param scopes                    the permissions (OAuth scopes) requested for the service
     * @param organizationAdminEmail    the email of the admin user to impersonate (Domain-Wide Delegation)
     * @return authorized {@link ServiceAccountCredentials}
     * @throws Exception if the private key is invalid or credentials cannot be built
     */
    public ServiceAccountCredentials getCredentials(Set<String> scopes, String organizationAdminEmail) throws Exception {
        String pk = privateKey.replace("\\n", "\n");

        return ServiceAccountCredentials.newBuilder()
                .setClientEmail(clientEmail)
                .setPrivateKey(decodePrivateKey(pk))
                .setServiceAccountUser(organizationAdminEmail)
                .setScopes(scopes)
                .build();
    }

    /**
     * Initializes a Directory service for managing users, groups, and devices.
     *
     * @param scopes        set of OAuth scopes
     * @param loggedInEmail the admin email to impersonate
     */
    public Directory getDirectoryService(Set<String> scopes, String loggedInEmail) throws Exception {
        ServiceAccountCredentials credentials = getCredentials(scopes, loggedInEmail);

        return new Directory.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName("CloudGuard")
                .build();
    }

    public Directory getDirectoryService(String scope, String loggedInEmail) throws Exception {
        return getDirectoryService(Collections.singleton(scope), loggedInEmail);
    }

    /**
     * Initializes a Drive service for analyzing Shared Drives and file permissions.
     */
    public Drive getDriveService(Set<String> scopes, String loggedInEmail) throws Exception {
        ServiceAccountCredentials credentials = getCredentials(scopes, loggedInEmail);

        return new Drive.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName("CloudGuard")
                .build();
    }

    public Drive getDriveService(String scope, String loggedInEmail) throws Exception {
        return getDriveService(Collections.singleton(scope), loggedInEmail);
    }

    /**
     * Initializes a Licensing service for auditing Google Workspace license usage.
     */
    public Licensing getLicensingService(Set<String> scopes, String loggedInEmail) throws Exception {
        ServiceAccountCredentials credentials = getCredentials(scopes, loggedInEmail);

        return new Licensing.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName("CloudGuard")
                .build();

    }

    public Licensing getLicensingService(String scope, String loggedInEmail) throws Exception {
        return getLicensingService(Collections.singleton(scope), loggedInEmail);
    }

    /**
     * Initializes a CloudIdentity service for managing security groups and identity settings.
     */
    public CloudIdentity getCloudIdentityService(Set<String> scopes, String loggedInEmail) throws Exception {
        ServiceAccountCredentials credentials = getCredentials(scopes, loggedInEmail);

        return new CloudIdentity.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName("CloudGuard")
                .build();

    }

    public CloudIdentity getCloudIdentityService(String scope, String loggedInEmail) throws Exception {
        return getCloudIdentityService(Collections.singleton(scope), loggedInEmail);
    }
}
