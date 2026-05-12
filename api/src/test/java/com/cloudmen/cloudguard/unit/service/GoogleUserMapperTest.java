package com.cloudmen.cloudguard.unit.service;

import com.cloudmen.cloudguard.dto.users.UserOrgDetail;
import com.cloudmen.cloudguard.service.users.GoogleUserMapper;
import com.google.api.services.admin.directory.model.User;
import com.google.api.services.admin.directory.model.UserName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.cloudmen.cloudguard.unit.helper.GlobalTestHelper.daysAgo;
import static org.assertj.core.api.Assertions.assertThat;

/** Unit tests for {@link GoogleUserMapper} (picture fallback, roles, active flag). */
class GoogleUserMapperTest {

    private final GoogleUserMapper mapper = new GoogleUserMapper();

    @Test
    void mapToOrgDetail_usesThumbnail_whenPresent_overFallback() {
        User u = baseUser("u1@x.com", "Pat Example");
        u.setThumbnailPhotoUrl("https://lh3.googleusercontent.com/thumb");
        UserOrgDetail dto =
                mapper.mapToOrgDetail(u, Map.of(), Map.of(), "https://fallback/oauth.jpg");

        assertThat(dto.pictureUrl()).isEqualTo("https://lh3.googleusercontent.com/thumb");
    }

    @Test
    void mapToOrgDetail_usesCloudguardFallback_whenThumbnailMissing() {
        User u = baseUser("u2@x.com", "No Thumb");
        u.setThumbnailPhotoUrl(null);
        UserOrgDetail dto =
                mapper.mapToOrgDetail(u, Map.of(), Map.of(), "https://fallback/oauth.jpg");

        assertThat(dto.pictureUrl()).isEqualTo("https://fallback/oauth.jpg");
    }

    @Test
    void mapToOrgDetail_pictureUrlNull_whenThumbnailAndFallbackBlank() {
        User u = baseUser("u3@x.com", "Both Blank");
        u.setThumbnailPhotoUrl("   ");
        UserOrgDetail dto = mapper.mapToOrgDetail(u, Map.of(), Map.of(), null);

        assertThat(dto.pictureUrl()).isNull();
    }

    @Test
    void mapToOrgDetail_resolvesRole_fromAssignmentsAndDictionary() {
        User u = baseUser("admin@x.com", "Admin Person");
        u.setId("admin-google-id");

        Map<String, Long> assignments = Map.of("admin-google-id", 700L);
        Map<Long, String> dictionary = Map.of(700L, "_SEED_ADMIN_ROLE");

        UserOrgDetail dto = mapper.mapToOrgDetail(u, assignments, dictionary, null);

        assertThat(dto.role()).isEqualTo("Super Admin");
    }

    @Test
    void mapToOrgDetail_regularUser_whenNoRoleAssignment() {
        User u = baseUser("plain@x.com", "Plain");
        u.setId("plain-id");

        UserOrgDetail dto = mapper.mapToOrgDetail(u, Map.of(), Map.of(), null);

        assertThat(dto.role()).isEqualTo("Regular User");
    }

    @Test
    void mapToOrgDetail_suspended_userMarkedInactive() {
        User u = baseUser("gone@x.com", "Suspended");
        u.setSuspended(true);
        u.setLastLoginTime(daysAgo(5));

        UserOrgDetail dto = mapper.mapToOrgDetail(u, Map.of(), Map.of(), null);

        assertThat(dto.isActive()).isFalse();
    }

    private static User baseUser(String email, String fullName) {
        User u = new User();
        u.setId(email);
        u.setPrimaryEmail(email);
        UserName name = new UserName();
        name.setFullName(fullName);
        u.setName(name);
        u.setSuspended(false);
        u.setIsEnrolledIn2Sv(true);
        u.setLastLoginTime(daysAgo(5));
        return u;
    }
}
