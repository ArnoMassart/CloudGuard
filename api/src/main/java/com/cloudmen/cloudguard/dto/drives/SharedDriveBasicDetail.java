package com.cloudmen.cloudguard.dto.drives;

import com.google.api.client.util.DateTime;

public record SharedDriveBasicDetail(
        String id,
        String name,
        int totalMembers,
        int externalMembers,
        int totalOrganizers,
        DateTime createdTime,
        String parsedTime,
        boolean onlyDomainUsersAllowed,
        boolean onlyMembersCanAccess,
        String risk
) {
}
