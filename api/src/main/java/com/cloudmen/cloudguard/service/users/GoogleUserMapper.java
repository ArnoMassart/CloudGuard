package com.cloudmen.cloudguard.service.users;

import com.cloudmen.cloudguard.dto.users.UserOrgDetail;
import com.google.api.client.util.DateTime;
import org.springframework.stereotype.Component;
import com.cloudmen.cloudguard.utility.DateTimeConverter;
import com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods;
import com.google.api.services.admin.directory.model.User;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Component responsible for translating raw Google Workspace User objects into frontend-ready Data Transfer Objects
 * (DTOs), including role resolution and basic security status evaluation.
 */
@Component
public class GoogleUserMapper {
    /**
     * @param cloudguardPictureFallback stored OAuth/JWT picture URL for this email when Directory has no thumbnail
     */
    public UserOrgDetail mapToOrgDetail(
            User user,
            Map<String, Long> roleAssignments,
            Map<Long, String> roleDictionary,
            String cloudguardPictureFallback) {
        Long roleId = roleAssignments.get(user.getId());
        String roleName = (roleId != null) ? roleDictionary.getOrDefault(roleId, "Unknown Role") : "Regular User";

        boolean isActive = !Boolean.TRUE.equals(user.getSuspended());
        boolean twoFAEnabled = Boolean.TRUE.equals(user.getIsEnrolledIn2Sv());

        var security = GoogleServiceHelperMethods.evaluateUserSecurity(isActive, user.getLastLoginTime(), twoFAEnabled);

        String pictureUrl = firstNonBlank(user.getThumbnailPhotoUrl(), cloudguardPictureFallback);

        return new UserOrgDetail(
                user.getName().getFullName(),
                user.getPrimaryEmail(),
                pictureUrl,
                GoogleServiceHelperMethods.translateRoleName(roleName),
                isActive,
                lastLoginConverter(user.getLastLoginTime()),
                twoFAEnabled,
                security.conform(),
                security.violationCodes()
        );
    }

    private static String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        if (fallback != null && !fallback.isBlank()) {
            return fallback;
        }
        return null;
    }

    private String lastLoginConverter(DateTime lastLogin) {
        return lastLogin != null && lastLogin.getValue() != 0 ? DateTimeConverter.convertToTimeAgo(lastLogin) : "Nooit";
    }
}
