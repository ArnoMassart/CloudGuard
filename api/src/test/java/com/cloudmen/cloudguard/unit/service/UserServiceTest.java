package com.cloudmen.cloudguard.unit.service;

import com.cloudmen.cloudguard.domain.model.Organization;
import com.cloudmen.cloudguard.domain.model.User;
import com.cloudmen.cloudguard.dto.users.UserDto;
import com.cloudmen.cloudguard.repository.OrganizationRepository;
import com.cloudmen.cloudguard.repository.UserRepository;
import com.cloudmen.cloudguard.service.AccessRequestEmailService;
import com.cloudmen.cloudguard.service.CloudguardStaffService;
import com.cloudmen.cloudguard.service.OrganizationService;
import com.cloudmen.cloudguard.service.UserService;
import com.cloudmen.cloudguard.utility.GoogleApiFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

import java.util.*;

import static com.cloudmen.cloudguard.unit.helper.GlobalTestHelper.*;
import static com.cloudmen.cloudguard.unit.helper.UserTestHelper.createDbUser;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private MessageSource messageSource;

    @Mock
    private UserService userService;

    @Mock
    private AccessRequestEmailService accessRequestEmailService;

    @Mock
    private GoogleApiFactory googleApiFactory;

    @Mock
    private OrganizationService organizationService;

    @Mock
    private CloudguardStaffService cloudguardStaffService;

    @BeforeEach
    void setUp() {
        lenient().when(cloudguardStaffService.isCloudmenAdmin(anyString())).thenReturn(false);
        userService = new UserService(userRepository, organizationRepository, messageSource, accessRequestEmailService, organizationService, googleApiFactory, cloudguardStaffService);
    }

    @Test
    void convertToDto_mapsAllFieldsCorrectly() {
        User user = createDbUser(ADMIN, "John", "Doe", "en");

        UserDto dto = userService.convertToDto(user);

        // AANGEPAST: Record accessors (zonder "get")
        assertEquals(ADMIN, dto.email());
        assertEquals("John", dto.firstName());
        assertEquals("Doe", dto.lastName());
        assertEquals("https://example.com/avatar.jpg", dto.pictureUrl());
        assertEquals(user.getCreatedAt(), dto.createdAt());
        assertEquals(user.isRoleRequested(), dto.roleRequested()); // Toegevoegd voor compleetheid
        assertEquals("", dto.organizationName());
        assertEquals(false, dto.isCloudmenStaff());
    }

    @Test
    void convertToDto_setsIsCloudmenStaffFromAllowlist() {
        User user = createDbUser(ADMIN, "John", "Doe", "en");
        when(cloudguardStaffService.isCloudmenAdmin(ADMIN)).thenReturn(true);

        UserDto dto = userService.convertToDto(user);

        assertEquals(true, dto.isCloudmenStaff());
    }

    @Test
    void convertToDto_resolvesOrganizationNameWhenLinked() {
        User user = createDbUser(ADMIN, "John", "Doe", "en");
        user.setOrganizationId(42L);
        Organization org = new Organization();
        org.setId(42L);
        org.setCustomerId("C-acme");
        org.setName("Acme Workspace");
        when(organizationRepository.findById(42L)).thenReturn(Optional.of(org));

        UserDto dto = userService.convertToDto(user);

        // AANGEPAST: Record accessors
        assertEquals("Acme Workspace", dto.organizationName());
        verifyNoInteractions(messageSource);
    }

    @Test
    void convertToDto_fallbackOrganization_usesMessageBundle() {
        User user = createDbUser(ADMIN, "John", "Doe", "en");
        user.setOrganizationId(42L);
        Organization org = new Organization();
        org.setId(42L);
        org.setCustomerId(null);
        org.setName("Organization (workspace customer id unavailable)");
        when(organizationRepository.findById(42L)).thenReturn(Optional.of(org));
        when(messageSource.getMessage(
                eq("api.organization.fallback_display_name"),
                isNull(),
                eq("Organization (workspace customer could not be resolved)"),
                any(Locale.class)))
                .thenReturn("Vertaalde fallback");

        UserDto dto = userService.convertToDto(user);

        // AANGEPAST: Record accessors
        assertEquals("Vertaalde fallback", dto.organizationName());
    }

    @Test
    void findByEmail_Optional_delegatesToRepository() {
        User user = createDbUser(ADMIN, "John", "Doe", "nl");
        when(userRepository.findByEmail(ADMIN)).thenReturn(Optional.of(user));

        Optional<User> result = userService.findByEmailOptional(ADMIN);

        assertTrue(result.isPresent());
        assertEquals(ADMIN, result.get().getEmail());
        verify(userRepository).findByEmail(ADMIN);
    }

    @Test
    void save_delegatesToRepository() {
        User user = createDbUser(ADMIN, "John", "Doe", "nl");
        when(userRepository.save(user)).thenReturn(user);

        User savedUser = userService.save(user);

        assertEquals(ADMIN, savedUser.getEmail());
        verify(userRepository).save(user);
    }

    @Test
    void getAllEmails_mapsUsersToEmailList() {
        User user1 = createDbUser("user1@example.com", "A", "B", "nl");
        User user2 = createDbUser("user2@example.com", "C", "D", "en");

        when(userRepository.findAll()).thenReturn(List.of(user1, user2));

        List<String> emails = userService.getAllEmails();

        assertEquals(2, emails.size());
        assertTrue(emails.contains("user1@example.com"));
        assertTrue(emails.contains("user2@example.com"));
    }

    @Test
    void getAllEmails_emptyRepository_returnsEmptyList() {
        when(userRepository.findAll()).thenReturn(List.of());

        List<String> emails = userService.getAllEmails();

        assertTrue(emails.isEmpty());
    }

    @Test
    void getLanguage_userExistsWithLanguage_returnsLanguage() {
        User user = createDbUser(ADMIN, "John", "Doe", "fr");
        when(userRepository.findByEmail(ADMIN)).thenReturn(Optional.of(user));

        String language = userService.getLanguage(ADMIN);

        assertEquals("fr", language);
    }

    @Test
    void getLanguage_userExistsWithNullLanguage_returnsDefaultNl() {
        User user = createDbUser(ADMIN, "John", "Doe", null);
        when(userRepository.findByEmail(ADMIN)).thenReturn(Optional.of(user));

        String language = userService.getLanguage(ADMIN);

        assertEquals("nl", language);
    }

    @Test
    void getLanguage_userDoesNotExist_returnsDefaultNl() {
        String email = "unknown@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        String language = userService.getLanguage(email);

        assertEquals("nl", language);
    }

    @Test
    void updateLanguage_userExists_updatesAndSaves() {
        User user = createDbUser(ADMIN, "John", "Doe", "nl");
        when(userRepository.findByEmail(ADMIN)).thenReturn(Optional.of(user));

        userService.updateLanguage(ADMIN, "es");

        ArgumentCaptor<User> userArgumentCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userArgumentCaptor.capture());

        User savedUser = userArgumentCaptor.getValue();
        assertEquals("es", savedUser.getLanguage());
    }

    @Test
    void updateLanguage_userDoesNotExist_doesNothing() {
        String email = "unknown@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        userService.updateLanguage(email, "en");

        verify(userRepository, never()).save(any(User.class));
    }
}