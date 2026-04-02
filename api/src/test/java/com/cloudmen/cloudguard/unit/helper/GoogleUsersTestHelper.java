package com.cloudmen.cloudguard.unit.helper;

import com.cloudmen.cloudguard.dto.users.UserCacheEntry;
import com.cloudmen.cloudguard.service.cache.GoogleUsersCacheService;
import com.google.api.client.util.DateTime;
import com.google.api.services.admin.directory.model.User;
import com.google.api.services.admin.directory.model.UserName;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

public class GoogleUsersTestHelper {
    public static User createUser(String email, String fullName, Boolean suspended,Boolean enrolledIn2Sv, Boolean isAdmin, DateTime lastLogin) {
        User u = new User();
        u.setId(email);
        u.setPrimaryEmail(email);

        UserName name = new UserName();
        name.setFullName(fullName);
        u.setName(name);

        u.setSuspended(suspended);
        u.setIsEnrolledIn2Sv(enrolledIn2Sv);
        u.setIsAdmin(isAdmin);
        u.setLastLoginTime(lastLogin);

        return u;
    }

    public static void mockCacheEntry(GoogleUsersCacheService usersCacheService, List<User> users, Map<String, Long> roleAssignments, Map<Long, String> roleDict) {
        UserCacheEntry mockEntry = mock(UserCacheEntry.class);

        lenient().when(mockEntry.allUsers()).thenReturn(users);
        lenient().when(mockEntry.userRoleAssignments()).thenReturn(roleAssignments);
        lenient().when(mockEntry.roleDictionary()).thenReturn(roleDict);

        when(usersCacheService.getOrFetchUsersData(GlobalTestHelper.ADMIN)).thenReturn(mockEntry);
    }
}
