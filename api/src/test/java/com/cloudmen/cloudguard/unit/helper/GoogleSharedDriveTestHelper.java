package com.cloudmen.cloudguard.unit.helper;

import com.cloudmen.cloudguard.dto.drives.SharedDriveBasicDetail;
import com.cloudmen.cloudguard.dto.drives.SharedDriveCacheEntry;
import com.cloudmen.cloudguard.service.cache.GoogleSharedDriveCacheService;
import com.google.api.client.util.DateTime;

import java.util.List;

import static org.mockito.Mockito.*;

public class GoogleSharedDriveTestHelper {
    public static SharedDriveBasicDetail createDrive(
            String id,
            String name,
            int totalMembers,
            int externalMembers,
            int totalOrganizers,
            boolean onlyDomainUsersAllowed,
            boolean onlyMembersCanAccess,
            String risk,
            DateTime createdTime) {

        return new SharedDriveBasicDetail(
                id,
                name,
                totalMembers,
                externalMembers,
                totalOrganizers,
                createdTime,
                null, // timeAgo wordt in de service berekend
                onlyDomainUsersAllowed,
                onlyMembersCanAccess,
                risk
        );
    }

    public static void mockCacheEntry(GoogleSharedDriveCacheService cacheService, List<SharedDriveBasicDetail> drives) {
        SharedDriveCacheEntry mockEntry = mock(SharedDriveCacheEntry.class);

        lenient().when(mockEntry.allDrives()).thenReturn(drives);

        when(cacheService.getOrFetchDriveData(GlobalTestHelper.ADMIN)).thenReturn(mockEntry);
    }
}
