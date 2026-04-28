package com.cloudmen.cloudguard.service.users;

import com.cloudmen.cloudguard.dto.users.UserOrgDetail;
import org.springframework.stereotype.Component;
import com.cloudmen.cloudguard.utility.DateTimeConverter;
import com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods;
import com.google.api.services.admin.directory.model.User;

import java.util.*;

/**
 * Component responsible for translating raw Google Workspace User objects into frontend-ready Data Transfer Objects
 * (DTOs), including role resolution and basic security status evaluation.
 */
@Component
public class GoogleUserMapper {
    public UserOrgDetail mapToOrgDetail(User user, Map<String, Long> roleAssignments, Map<Long, String> roleDictionary) {
        Long roleId = roleAssignments.get(user.getId());
        String roleName = (roleId != null) ? roleDictionary.getOrDefault(roleId, "Unknown Role") : "Regular User";

        boolean isActive = !Boolean.TRUE.equals(user.getSuspended());
        boolean twoFAEnabled = Boolean.TRUE.equals(user.getIsEnrolledIn2Sv());

        var security = GoogleServiceHelperMethods.evaluateUserSecurity(isActive, user.getLastLoginTime(), twoFAEnabled);

        return new UserOrgDetail(
                user.getName().getFullName(),
                user.getPrimaryEmail(),
                GoogleServiceHelperMethods.translateRoleName(roleName),
                isActive,
                user.getLastLoginTime() != null ? DateTimeConverter.convertToTimeAgo(user.getLastLoginTime()) : "Nooit",
                twoFAEnabled,
                security.conform(),
                security.violationCodes()
        );
    }
}
