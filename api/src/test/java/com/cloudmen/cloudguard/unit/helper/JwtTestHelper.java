package com.cloudmen.cloudguard.unit.helper;

import com.cloudmen.cloudguard.domain.model.User;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JwtTestHelper {
    public static User createUser(String email) {
        User user = new User();
        user.setEmail(email);
        return user;
    }

    public static Jwt mockJwtWithAdminClaim(Boolean isAdmin) {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaim("https://cloudguard.com/is_admin")).thenReturn(isAdmin);
        return jwt;
    }
}
