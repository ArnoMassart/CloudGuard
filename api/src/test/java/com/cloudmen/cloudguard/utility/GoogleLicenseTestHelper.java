package com.cloudmen.cloudguard.utility;

import com.cloudmen.cloudguard.dto.licenses.InactiveUser;
import com.cloudmen.cloudguard.dto.licenses.LicenseCacheEntry;
import com.cloudmen.cloudguard.dto.licenses.LicenseType;
import com.cloudmen.cloudguard.dto.users.UserCacheEntry;
import com.cloudmen.cloudguard.service.cache.GoogleLicenseCacheService;
import com.cloudmen.cloudguard.service.cache.GoogleUsersCacheService;
import com.google.api.services.admin.directory.model.User;

import java.util.List;

import static com.cloudmen.cloudguard.utility.GlobalTestHelper.ADMIN;
import static org.mockito.Mockito.*;

public class GoogleLicenseTestHelper {
    public static LicenseType createLicenseType(String skuId, String skuName, int totalAssigned) {
        return new LicenseType(skuId, skuName, totalAssigned);
    }

    public static InactiveUser createInactiveUser(String email) {
        return new InactiveUser(email, "30 days ago", "Google Workspace Basic", false, 30L);
    }

    public static User createUser(String email, Boolean suspended) {
        User u = new User();
        u.setPrimaryEmail(email);
        u.setSuspended(suspended);
        return u;
    }

    public static void mockLicenseCacheEntry(GoogleLicenseCacheService cacheService, List<LicenseType> types, List<InactiveUser> inactiveUsers) {
        LicenseCacheEntry mockEntry = mock(LicenseCacheEntry.class);

        lenient().when(mockEntry.licenseTypes()).thenReturn(types);
        lenient().when(mockEntry.inactiveUsers()).thenReturn(inactiveUsers);

        when(cacheService.getOrFetchLicenseData(ADMIN)).thenReturn(mockEntry);
    }

    public static void mockUsersCacheEntry(GoogleUsersCacheService cacheService, List<User> users) {
        UserCacheEntry mockEntry = mock(UserCacheEntry.class);

        lenient().when(mockEntry.allUsers()).thenReturn(users);

        lenient().when(cacheService.getOrFetchUsersData(ADMIN)).thenReturn(mockEntry);
    }
}
