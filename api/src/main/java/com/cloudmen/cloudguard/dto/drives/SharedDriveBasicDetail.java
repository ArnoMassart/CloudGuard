package com.cloudmen.cloudguard.dto.drives;

public record SharedDriveBasicDetail(
        String id,
        String name,
        int totalMembers,
        int externalMembers,
        int totalOrganizers,
        String createdTime,
        boolean onlyDomainUsersAllowed,
        boolean onlyMembersCanAccess,
        String risk
) {
}
