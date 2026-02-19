package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.dto.UserOrgDetail;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.admin.directory.Directory;
import com.google.api.services.admin.directory.DirectoryScopes;
import com.google.api.services.admin.directory.model.*;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.ServiceAccountCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GoogleAdminService {

    @Value("${google.api.client-email}")
    private String clientEmail;

    @Value("${google.api.private-key}")
    private String privateKey;

    private Directory getDirectoryService(String scope, String loggedInEmail) throws Exception {
        String pk = privateKey.replace("\\n", "\n");

        ServiceAccountCredentials credentials = ServiceAccountCredentials.newBuilder()
                .setClientEmail(clientEmail)
                .setPrivateKey(decodePrivateKey(pk))
                .setServiceAccountUser(loggedInEmail)
                .setScopes(Collections.singleton(scope))
                .build();

        return new Directory.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName("CloudGuard")
                .build();
    }

    private java.security.PrivateKey decodePrivateKey(String pem) throws Exception {
        String privateKeyPEM = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] encoded = java.util.Base64.getDecoder().decode(privateKeyPEM);
        java.security.spec.PKCS8EncodedKeySpec keySpec = new java.security.spec.PKCS8EncodedKeySpec(encoded);
        java.security.KeyFactory kf = java.security.KeyFactory.getInstance("RSA");
        return kf.generatePrivate(keySpec);
    }

    public List<String> getUserRoles(String email) {
        try {
            Directory service = getDirectoryService(DirectoryScopes.ADMIN_DIRECTORY_ROLEMANAGEMENT_READONLY, email);

            RoleAssignments assignments = service.roleAssignments().list("my_customer")
                    .setUserKey(email)
                    .execute();
            List<RoleAssignment> items = assignments.getItems();

            if (items == null || items.isEmpty()) {
                return Collections.emptyList();
            }

            List<com.google.api.services.admin.directory.model.Role> allRoles =
                    service.roles().list("my_customer").execute().getItems();

            return items.stream()
                    .map(assignment -> allRoles.stream()
                            .filter(role -> role.getRoleId().equals(assignment.getRoleId()))
                            .findFirst()
                            .map(Role::getRoleName)
                            .orElse("Unknown Role (" + assignment.getRoleId() + ")"))
                    .distinct()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch roles from Google: " + e.getMessage());
        }
    }

    public List<UserOrgDetail> getAllWorkspaceUsers(String loggedInEmail) {
        try {
            Directory userDirectory = getDirectoryService(DirectoryScopes.ADMIN_DIRECTORY_USER_READONLY, loggedInEmail);
            Directory roleDirectory = getDirectoryService(DirectoryScopes.ADMIN_DIRECTORY_ROLEMANAGEMENT_READONLY, loggedInEmail);

            Map<Long, String> roleDictionary = roleDirectory.roles().list("my_customer")
                    .execute()
                    .getItems()
                    .stream()
                    .collect(Collectors.toMap(
                            Role::getRoleId,
                            Role::getRoleName,
                            (existing, replacement) -> existing
                    ));

            List<User> googleUsers = fetchAllOrgUsers(userDirectory);

            return googleUsers.stream().map(user -> {
                String firstRole = getFirstRoleForUser(roleDirectory, user.getPrimaryEmail(), roleDictionary);

                return new UserOrgDetail(
                        user.getName().getFullName(),
                        user.getPrimaryEmail(),
                        translateRoleName(firstRole),
                        !Boolean.TRUE.equals(user.getSuspended()),
                        user.getLastLoginTime() != null ? user.getLastLoginTime().toString() : "Never",
                        Boolean.TRUE.equals(user.getIsEnrolledIn2Sv())
                );
            }).toList();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch users from Google: " + e.getMessage());
        }
    }

    private List<User> fetchAllOrgUsers(Directory service) throws IOException {
        List<User> googleUsers = new ArrayList<>();
        String pageToken = null;

        do {
            Users result = service.users().list()
                    .setCustomer("my_customer")
                    .setProjection("full")
                    .setMaxResults(100)
                    .setPageToken(pageToken)
                    .setOrderBy("email")
                    .execute();

            if (result.getUsers() != null) {
                googleUsers.addAll(result.getUsers());
            }

            pageToken = result.getNextPageToken();
        } while (pageToken != null);

        return googleUsers;
    }

    private String getFirstRoleForUser(Directory service, String email, Map<Long, String> roleDictionary) {
        try {
            // Haal de rol-koppelingen op voor deze specifieke gebruiker
            RoleAssignments assignments = service.roleAssignments()
                    .list("my_customer")
                    .setUserKey(email)
                    .execute();

            if (assignments.getItems() == null || assignments.getItems().isEmpty()) {
                return "Regular User";
            }

            // Pak de RoleId van de eerste assignment
            Long firstRoleId = assignments.getItems().get(0).getRoleId();

            // Zoek de naam op in ons woordenboek (geen API call nodig!)
            String name = roleDictionary.getOrDefault(firstRoleId, "Unknown Role");

            // Optioneel: vertaal systeemrollen naar mooie namen
            return translateRoleName(name);

        } catch (IOException e) {
            return "Error fetching role";
        }
    }

    private String translateRoleName(String role) {
        if (role == null) return "User";
        return switch (role) {
            case "_SEED_ADMIN_ROLE" -> "Super Admin";
            case "_READ_ONLY_ADMIN_ROLE" -> "Read Only Admin";
            default -> role;
        };
    }
}
