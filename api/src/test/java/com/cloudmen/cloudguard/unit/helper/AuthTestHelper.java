package com.cloudmen.cloudguard.unit.helper;

import com.cloudmen.cloudguard.domain.model.User;
import com.cloudmen.cloudguard.dto.users.UserDto;
import com.cloudmen.cloudguard.utility.GoogleApiFactory;
import com.google.api.services.admin.directory.Directory;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

public class AuthTestHelper {
    public static Jwt mockGoogleJwt(String email, String givenName, String familyName, String picture) {
        Jwt jwt = mock(Jwt.class);
        lenient().when(jwt.getClaimAsString("email")).thenReturn(email);
        lenient().when(jwt.getClaimAsString("given_name")).thenReturn(givenName);
        lenient().when(jwt.getClaimAsString("family_name")).thenReturn(familyName);
        lenient().when(jwt.getClaimAsString("picture")).thenReturn(picture);
        return jwt;
    }

    public static User createDbUser(String email, String pictureUrl){
        User user = new User();
        user.setEmail(email);
        user.setFirstName("First");
        user.setLastName("Last");
        user.setPictureUrl(pictureUrl);
        user.setCreatedAt(LocalDateTime.now());

        return user;
    }

    public static UserDto mockUserDto(){
        return mock(UserDto.class);
    }

    public static void mockDwdCheck(String userEmail, String adminEmail, boolean isGoogleAdmin, GoogleApiFactory googleApiFactory) throws Exception {
        Directory dirMock = mock(Directory.class);
        Directory.Users usersMock = mock(Directory.Users.class);
        Directory.Users.Get getMock = mock(Directory.Users.Get.class);

        com.google.api.services.admin.directory.model.User googleUser =
                new com.google.api.services.admin.directory.model.User();
        googleUser.setIsAdmin(isGoogleAdmin);

        String effectiveEmail = (adminEmail == null || adminEmail.isBlank()) ? userEmail : adminEmail;

        lenient().when(googleApiFactory.getDirectoryService(anyString(), eq(effectiveEmail))).thenReturn(dirMock);
        lenient().when(dirMock.users()).thenReturn(usersMock);
        lenient().when(usersMock.get(userEmail)).thenReturn(getMock);
        lenient().when(getMock.execute()).thenReturn(googleUser);
    }
}
