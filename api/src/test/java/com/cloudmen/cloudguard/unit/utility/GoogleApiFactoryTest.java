package com.cloudmen.cloudguard.unit.utility;

import com.cloudmen.cloudguard.utility.GoogleApiFactory;
import com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods;
import com.google.api.services.admin.directory.Directory;
import com.google.api.services.cloudidentity.v1.CloudIdentity;
import com.google.api.services.drive.Drive;
import com.google.api.services.licensing.Licensing;
import com.google.auth.oauth2.ServiceAccountCredentials;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.PrivateKey;
import java.util.Set;

import static com.cloudmen.cloudguard.unit.helper.GlobalTestHelper.ADMIN;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class GoogleApiFactoryTest {

    private GoogleApiFactory googleApiFactory;

    private static final String CLIENT_EMAIL = "test-service-account@gserviceaccount.com";
    private static final String PRIVATE_KEY_STRING = "dummy-private-key\\nwith-newlines";
    private static final String DUMMY_SCOPE = "https://www.googleapis.com/auth/admin.directory.user";

    @BeforeEach
    void setUp() {
        googleApiFactory = new GoogleApiFactory();
        ReflectionTestUtils.setField(googleApiFactory, "clientEmail", CLIENT_EMAIL);
        ReflectionTestUtils.setField(googleApiFactory, "privateKey", PRIVATE_KEY_STRING);
    }

    @Test
    void getCredentials_buildsValidServiceAccountCredentials() throws Exception {
        PrivateKey mockPrivateKey = mock(PrivateKey.class);

        try (MockedStatic<GoogleServiceHelperMethods> utilities = mockStatic(GoogleServiceHelperMethods.class)) {
            utilities.when(() -> GoogleServiceHelperMethods.decodePrivateKey("dummy-private-key\nwith-newlines"))
                    .thenReturn(mockPrivateKey);

            ServiceAccountCredentials credentials = googleApiFactory.getCredentials(Set.of(DUMMY_SCOPE), ADMIN);

            assertNotNull(credentials);
            assertEquals(CLIENT_EMAIL, credentials.getClientEmail());
            assertEquals(ADMIN, credentials.getServiceAccountUser());
            assertTrue(credentials.getScopes().contains(DUMMY_SCOPE));
        }
    }

    @Test
    void getDirectoryService_returnsConfiguredInstance() throws Exception {
        PrivateKey mockPrivateKey = mock(PrivateKey.class);

        try (MockedStatic<GoogleServiceHelperMethods> utilities = mockStatic(GoogleServiceHelperMethods.class)) {
            utilities.when(() -> GoogleServiceHelperMethods.decodePrivateKey(anyString()))
                    .thenReturn(mockPrivateKey);

            Directory service = googleApiFactory.getDirectoryService(DUMMY_SCOPE, ADMIN);

            assertNotNull(service);
            assertEquals("CloudGuard", service.getApplicationName());
        }
    }

    @Test
    void getDriveService_returnsConfiguredInstance() throws Exception {
        PrivateKey mockPrivateKey = mock(PrivateKey.class);

        try (MockedStatic<GoogleServiceHelperMethods> utilities = mockStatic(GoogleServiceHelperMethods.class)) {
            utilities.when(() -> GoogleServiceHelperMethods.decodePrivateKey(anyString()))
                    .thenReturn(mockPrivateKey);

            Drive service = googleApiFactory.getDriveService(DUMMY_SCOPE, ADMIN);

            assertNotNull(service);
            assertEquals("CloudGuard", service.getApplicationName());
        }
    }

    @Test
    void getLicensingService_returnsConfiguredInstance() throws Exception {
        PrivateKey mockPrivateKey = mock(PrivateKey.class);

        try (MockedStatic<GoogleServiceHelperMethods> utilities = mockStatic(GoogleServiceHelperMethods.class)) {
            utilities.when(() -> GoogleServiceHelperMethods.decodePrivateKey(anyString()))
                    .thenReturn(mockPrivateKey);

            Licensing service = googleApiFactory.getLicensingService(DUMMY_SCOPE, ADMIN);

            assertNotNull(service);
            assertEquals("CloudGuard", service.getApplicationName());
        }
    }

    @Test
    void getCloudIdentityService_returnsConfiguredInstance() throws Exception {
        PrivateKey mockPrivateKey = mock(PrivateKey.class);

        try (MockedStatic<GoogleServiceHelperMethods> utilities = mockStatic(GoogleServiceHelperMethods.class)) {
            utilities.when(() -> GoogleServiceHelperMethods.decodePrivateKey(anyString()))
                    .thenReturn(mockPrivateKey);

            CloudIdentity service = googleApiFactory.getCloudIdentityService(DUMMY_SCOPE, ADMIN);

            assertNotNull(service);
            assertEquals("CloudGuard", service.getApplicationName());
        }
    }
}
