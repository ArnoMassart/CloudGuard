package com.cloudmen.cloudguard.integration.google;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.admin.directory.Directory;
import com.google.api.services.admin.directory.model.Role;
import com.google.api.services.admin.directory.model.RoleAssignment;
import com.google.api.services.admin.directory.model.Roles;
import com.google.api.services.admin.directory.model.RoleAssignments;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract tests for Directory endpoints used alongside {@code users().list()} in
 * {@link com.cloudmen.cloudguard.service.cache.GoogleUsersCacheService#fetchFromGoogle}:
 * {@code roles().list(customer)} and {@code roleAssignments().list(customer)}.
 */
class GoogleDirectoryRolesWireMockIT {

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
    @DisplayName("roles.list JSON fixture parses to Role objects (role dictionary for RBAC)")
    void rolesList_fixture_parses() throws Exception {
        wireMockServer.stubFor(
                get(urlMatching("/admin/directory/v1/customer/my_customer/roles.*"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json; charset=UTF-8")
                                        .withBody(readFixture("contracts/google-directory-roles-list.json"))));

        Roles roles = directoryClient().roles().list("my_customer").execute();

        assertThat(roles.getItems()).hasSize(2);
        Map<Long, String> dictionary =
                roles.getItems().stream()
                        .collect(Collectors.toMap(Role::getRoleId, Role::getRoleName, (a, b) -> a));

        assertThat(dictionary.get(70000000000000001L)).isEqualTo("_SUPER_ADMIN");
        assertThat(dictionary.get(70000000000000002L)).isEqualTo("_HELP_DESK_ADMIN");
    }

    @Test
    @DisplayName("roleAssignments.list JSON fixture parses (user id -> role id map source)")
    void roleAssignmentsList_fixture_parses() throws Exception {
        wireMockServer.stubFor(
                get(urlMatching("/admin/directory/v1/customer/my_customer/roleassignments.*"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json; charset=UTF-8")
                                        .withBody(readFixture("contracts/google-directory-role-assignments-list.json"))));

        RoleAssignments assignments =
                directoryClient().roleAssignments().list("my_customer").execute();

        assertThat(assignments.getItems()).hasSize(1);
        RoleAssignment ra = assignments.getItems().get(0);
        assertThat(ra.getAssignedTo()).isEqualTo("contract.user@example.com");
        assertThat(ra.getRoleId()).isEqualTo(70000000000000001L);

        Map<String, Long> byUser =
                assignments.getItems().stream()
                        .collect(Collectors.toMap(RoleAssignment::getAssignedTo, RoleAssignment::getRoleId, (e, r) -> e));

        assertThat(byUser.get("contract.user@example.com")).isEqualTo(70000000000000001L);
    }

    private Directory directoryClient() throws Exception {
        return new Directory.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                request -> {})
                .setApplicationName("CloudGuard-contract-test")
                .setRootUrl(wireMockServer.baseUrl())
                .build();
    }

    private static String readFixture(String classpathRelative) throws Exception {
        try (var in = GoogleDirectoryRolesWireMockIT.class.getClassLoader().getResourceAsStream(classpathRelative)) {
            assertThat(in).as("classpath resource %s", classpathRelative).isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
