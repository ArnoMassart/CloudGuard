package com.cloudmen.cloudguard.dto.drives;

public record SharedDriveSecurityData(
        String id,
        String name,
        boolean domainUsersOnly,
        boolean driveMembersOnly,
        int externalMemberCount,
        int organizerCount,
        String risk
) {
}
