package com.cloudmen.cloudguard.unit.helper;

import com.cloudmen.cloudguard.domain.model.User;
import com.cloudmen.cloudguard.dto.users.UserDto;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDateTime;

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
}
