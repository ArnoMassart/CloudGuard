package com.cloudmen.cloudguard.integration.google;

import com.cloudmen.cloudguard.dto.users.UserOrgDetail;
import com.cloudmen.cloudguard.service.users.GoogleUserMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.admin.directory.Directory;
import com.google.api.services.admin.directory.model.User;
import com.google.api.services.admin.directory.model.Users;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract-style test: the official Google Directory Java client talks to a **real HTTP** stub (WireMock)
 * returning checked-in JSON fixtures — not Mockito-returned {@link User} objects.
 *
 * <p>This locks the JSON shape expected by {@link com.google.api.services.admin.directory.Directory}
 * parsing and by {@link GoogleUserMapper} for the Users section / thumbnail fallback behaviour described in
 * {@link com.cloudmen.cloudguard.service.cache.GoogleUsersCacheService#fetchAllOrgUsers}.
 *
 * <p>No Docker and no credentials: requests are unsigned; WireMock returns canned bodies for the same URL
 * pattern the client library generates for {@code users().list()} with {@code customer=my_customer},
 * {@code projection=full}, {@code maxResults=100}.
 */
class GoogleDirectoryUsersListWireMockIT {

    private WireMockServer wireMockServer;

    @BeforeEach
    void startWireMock() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
    }

    @AfterEach
    void stopWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @Test
    @DisplayName("Directory users.list JSON fixture parses and maps to UserOrgDetail with thumbnail from Directory")
    void usersList_fixture_with_thumbnail_maps_to_org_detail() throws Exception {
        wireMockServer.stubFor(
                get(urlMatching("/admin/directory/v1/users.*"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json; charset=UTF-8")
                                        .withBody(readFixture("contracts/google-directory-users-list-page.json"))));

        Users result = directoryClient().users().list()
                .setCustomer("my_customer")
                .setProjection("full")
                .setMaxResults(100)
                .execute();

        assertThat(result.getUsers()).hasSize(1);
        User u = result.getUsers().get(0);
        assertThat(u.getId()).isEqualTo("104994514742784813963");
        assertThat(u.getPrimaryEmail()).isEqualTo("contract.user@example.com");
        assertThat(u.getName().getFullName()).isEqualTo("Contract Test User");
        assertThat(u.getThumbnailPhotoUrl()).contains("googleusercontent.com");
        assertThat(u.getSuspended()).isNotEqualTo(Boolean.TRUE);
        assertThat(u.getIsEnrolledIn2Sv()).isTrue();

        GoogleUserMapper mapper = new GoogleUserMapper();
        UserOrgDetail dto = mapper.mapToOrgDetail(u, Map.of(), Map.of(), null);

        assertThat(dto.email()).isEqualTo("contract.user@example.com");
        assertThat(dto.fullName()).isEqualTo("Contract Test User");
        assertThat(dto.pictureUrl()).isEqualTo(u.getThumbnailPhotoUrl());
        assertThat(dto.isTwoFactorEnabled()).isTrue();
    }

    @Test
    @DisplayName("When Directory omits thumbnailPhotoUrl, mapper uses CloudGuard DB picture fallback")
    void usersList_fixture_without_thumbnail_uses_fallback_picture() throws Exception {
        wireMockServer.stubFor(
                get(urlMatching("/admin/directory/v1/users.*"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json; charset=UTF-8")
                                        .withBody(readFixture("contracts/google-directory-users-list-no-thumbnail.json"))));

        Users result = directoryClient().users().list()
                .setCustomer("my_customer")
                .setProjection("full")
                .setMaxResults(100)
                .execute();

        User u = result.getUsers().get(0);
        assertThat(u.getThumbnailPhotoUrl()).isNull();

        String fallback = "https://cloudguard.test/oauth-picture/fallback.jpg";
        UserOrgDetail dto = new GoogleUserMapper().mapToOrgDetail(u, Map.of(), Map.of(), fallback);

        assertThat(dto.pictureUrl()).isEqualTo(fallback);
    }

    @Test
    @DisplayName("users.list with empty users array parses without error (no users in domain edge case)")
    void usersList_emptyUsers_parses() throws Exception {
        wireMockServer.stubFor(
                get(urlMatching("/admin/directory/v1/users.*"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json; charset=UTF-8")
                                        .withBody(readFixture("contracts/google-directory-users-list-empty.json"))));

        Users result = directoryClient().users().list()
                .setCustomer("my_customer")
                .setProjection("full")
                .setMaxResults(100)
                .execute();

        assertThat(result.getUsers()).isEmpty();
        assertThat(result.getNextPageToken()).isNull();
    }

    private Directory directoryClient() throws Exception {
        return new Directory.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                request -> {
                    // Real calls sign with service-account JWT; WireMock does not validate auth — skip for contract test.
                })
                .setApplicationName("CloudGuard-contract-test")
                .setRootUrl(wireMockServer.baseUrl())
                .build();
    }

    private static String readFixture(String classpathRelative) throws Exception {
        try (var in = GoogleDirectoryUsersListWireMockIT.class.getClassLoader().getResourceAsStream(classpathRelative)) {
            assertThat(in).as("classpath resource %s", classpathRelative).isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
