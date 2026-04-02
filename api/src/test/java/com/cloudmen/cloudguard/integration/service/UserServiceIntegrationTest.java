package com.cloudmen.cloudguard.integration.service;

import com.cloudmen.cloudguard.domain.model.User;
import com.cloudmen.cloudguard.dto.users.UserDto;
import com.cloudmen.cloudguard.repository.UserRepository;
import com.cloudmen.cloudguard.service.UserService;
import jakarta.validation.constraints.Email;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
public class UserServiceIntegrationTest {
    @Autowired
    private UserService userService;

    @MockitoBean
    private UserRepository userRepository;

    private User testUser;
    private static final String EMAIL = "test@cloudmen.com";

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setEmail(EMAIL);
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setPictureUrl("https://image.url");
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setLanguage("en");
    }

    @Test
    void convertToDto_mapsFieldsCorrectly() {
        UserDto dto = userService.convertToDto(testUser);

        assertNotNull(dto);
        assertEquals(testUser.getEmail(), dto.getEmail());
        assertEquals(testUser.getFirstName(), dto.getFirstName());
        assertEquals(testUser.getLastName(), dto.getLastName());
        assertEquals(testUser.getPictureUrl(), dto.getPictureUrl());
        assertEquals(testUser.getCreatedAt(), dto.getCreatedAt());
    }

    @Test
    void findByEmail_returnsUserFromRepository() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(testUser));

        Optional<User> result = userService.findByEmail(EMAIL);

        assertTrue(result.isPresent());
        assertEquals(EMAIL, result.get().getEmail());
    }

    @Test
    void save_callsRepositorySave() {
        when(userRepository.save(testUser)).thenReturn(testUser);

        User savedUser = userService.save(testUser);

        assertNotNull(savedUser);
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void getAllEmails_returnsListOfEmails() {
        User user2 = new User();
        user2.setEmail("another@cloudmen.com");

        when(userRepository.findAll()).thenReturn(List.of(testUser, user2));

        List<String> emails = userService.getAllEmails();

        assertEquals(2, emails.size());
        assertTrue(emails.contains(EMAIL));
        assertTrue(emails.contains("another@cloudmen.com"));
    }

    @Test
    void getLanguage_userExistsWithLanguage_returnsLanguage() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(testUser));

        String lang = userService.getLanguage(EMAIL);

        assertEquals("en", lang);
    }

    @Test
    void getLanguage_userExistsWithoutLanguage_returnsDefaultNl() {
        testUser.setLanguage(null);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(testUser));

        String lang = userService.getLanguage(EMAIL);

        assertEquals("nl", lang);
    }

    @Test
    void getLanguage_userNotFound_returnsDefaultNl() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        String lang = userService.getLanguage(EMAIL);

        assertEquals("nl", lang);
    }

    @Test
    void updateLanguage_userExists_updatesAndSaves() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(testUser));

        userService.updateLanguage(EMAIL, "fr");

        assertEquals("fr", testUser.getLanguage());
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void updateLanguage_userNotFound_doesNotSave() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        userService.updateLanguage(EMAIL, "fr");

        verify(userRepository, never()).save(any(User.class));
    }
}
