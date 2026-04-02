package com.cloudmen.cloudguard.unit.helper;

import com.cloudmen.cloudguard.domain.model.User;

import java.time.LocalDateTime;

public class UserTestHelper {
    public static User createDbUser(String email, String firstName, String lastName, String language) {
        User user = new User();
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPictureUrl("https://example.com/avatar.jpg");
        user.setCreatedAt(LocalDateTime.now().minusDays(1)); // Gisteren aangemaakt
        user.setLanguage(language);
        return user;
    }
}
