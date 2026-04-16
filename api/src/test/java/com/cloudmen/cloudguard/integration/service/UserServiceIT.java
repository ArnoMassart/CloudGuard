package com.cloudmen.cloudguard.integration.service;

import com.cloudmen.cloudguard.domain.model.Organization;
import com.cloudmen.cloudguard.domain.model.User;
import com.cloudmen.cloudguard.domain.model.UserRole;
import com.cloudmen.cloudguard.dto.users.DatabaseUsersResponse;
import com.cloudmen.cloudguard.dto.users.UserDto;
import com.cloudmen.cloudguard.repository.OrganizationRepository;
import com.cloudmen.cloudguard.repository.UserRepository;
import com.cloudmen.cloudguard.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {UserService.class})
public class UserServiceIT {

    @Autowired
    private UserService userService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private OrganizationRepository organizationRepository;

    @MockitoBean
    private MessageSource messageSource;

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
        testUser.setRoles(new ArrayList<>());
        testUser.setRoleRequested(false);
    }

    // ==========================================
    // DTO & BASIC CRUD TESTS
    // ==========================================

    @Test
    void convertToDto_mapsFieldsCorrectly_withoutOrganization() {
        UserDto dto = userService.convertToDto(testUser);

        assertNotNull(dto);
        assertEquals(testUser.getEmail(), dto.email());
        assertEquals(testUser.getFirstName(), dto.firstName());
        assertEquals(testUser.getLastName(), dto.lastName());
        assertEquals(testUser.getPictureUrl(), dto.pictureUrl());
        assertEquals(testUser.getCreatedAt(), dto.createdAt());
        assertEquals("", dto.organizationName());
    }

    @Test
    void convertToDto_mapsOrganizationCorrectly() {
        testUser.setOrganizationId(1L);
        Organization org = new Organization();
        org.setId(1L);
        org.setCustomerId("C-123");
        org.setName("Acme Corp");

        when(organizationRepository.findById(1L)).thenReturn(Optional.of(org));

        UserDto dto = userService.convertToDto(testUser);

        assertEquals("Acme Corp", dto.organizationName());
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

    // ==========================================
    // PREFERENCE & STATUS TESTS
    // ==========================================

    @Test
    void getLanguage_userExistsWithLanguage_returnsLanguage() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(testUser));
        assertEquals("en", userService.getLanguage(EMAIL));
    }

    @Test
    void getLanguage_userExistsWithoutLanguage_returnsDefaultNl() {
        testUser.setLanguage(null);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(testUser));
        assertEquals("nl", userService.getLanguage(EMAIL));
    }

    @Test
    void getLanguage_userNotFound_returnsDefaultNl() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        assertEquals("nl", userService.getLanguage(EMAIL));
    }

    @Test
    void updateLanguage_userExists_updatesAndSaves() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(testUser));
        userService.updateLanguage(EMAIL, "fr");
        assertEquals("fr", testUser.getLanguage());
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void getRoleRequested_userExists_returnsCorrectStatus() {
        testUser.setRoleRequested(true);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(testUser));
        assertTrue(userService.getRoleRequested(EMAIL));
    }

    @Test
    void updateRequestAccess_userExists_setsRoleRequestedToTrue() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(testUser));
        userService.updateRequestAccess(EMAIL);
        assertTrue(testUser.isRoleRequested());
        verify(userRepository).save(testUser);
    }

    // ==========================================
    // ROLES TESTS
    // ==========================================

    @Test
    void hasValidRole_withAssignedRoles_returnsTrue() {
        testUser.setRoles(List.of(UserRole.SUPER_ADMIN));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(testUser));
        assertTrue(userService.hasValidRole(EMAIL));
    }

    @Test
    void hasValidRole_withUnassignedRoleOnly_returnsFalse() {
        testUser.setRoles(List.of(UserRole.UNASSIGNED));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(testUser));
        assertFalse(userService.hasValidRole(EMAIL));
    }

    @Test
    void hasValidRole_withEmptyRoles_returnsFalse() {
        testUser.setRoles(new ArrayList<>());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(testUser));
        assertFalse(userService.hasValidRole(EMAIL));
    }

    @Test
    void updateRoles_withValidRoles_updatesRoles() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(testUser));

        userService.updateRoles(EMAIL, new ArrayList<>(List.of(UserRole.USERS_GROUPS_VIEWER)));

        assertTrue(testUser.getRoles().contains(UserRole.USERS_GROUPS_VIEWER));
        verify(userRepository).save(testUser);
    }

    @Test
    void updateRoles_withEmptyList_setsToUnassigned() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(testUser));

        userService.updateRoles(EMAIL, new ArrayList<>()); // Empty list

        assertTrue(testUser.getRoles().contains(UserRole.UNASSIGNED));
        assertEquals(1, testUser.getRoles().size());
        verify(userRepository).save(testUser);
    }

    @Test
    void updateRolesAndUpdateRequestedStatus_updatesRolesAndResetsRequestedFlag() {
        testUser.setRoleRequested(true);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(testUser));

        userService.updateRolesAndUpdateRequestedStatus(EMAIL, new ArrayList<>(List.of(UserRole.SUPER_ADMIN)));

        assertTrue(testUser.getRoles().contains(UserRole.SUPER_ADMIN));
        assertFalse(testUser.isRoleRequested());
        verify(userRepository).save(testUser);
    }

    // ==========================================
    // PAGINATION & FETCHING TESTS
    // ==========================================

    @Test
    void getAll_returnsMappedDatabaseResponse() {
        Page<User> mockPage = new PageImpl<>(List.of(testUser));
        when(userRepository.findAllByRoleRequestedWithSearch(eq("query"), any(Pageable.class)))
                .thenReturn(mockPage);

        DatabaseUsersResponse response = userService.getAll("0", 10, "query");

        assertEquals(1, response.users().size());
        assertNull(response.nextPageToken());
    }

    @Test
    void getAllWithRequestedRole_AndOrganization_returnsMappedDatabaseResponse() {
        Page<User> mockPage = new PageImpl<>(List.of(testUser));
        when(userRepository.findAllByRoleRequestedWithSearch(eq(""), any(Pageable.class)))
                .thenReturn(mockPage);

        DatabaseUsersResponse response = userService.getAllWithRequestedRoleAndOrganization("0", 10, "");

        assertEquals(1, response.users().size());
        assertNull(response.nextPageToken()); // Geen hasNext() in kleine list
    }

    @Test
    void getAllRequestedCount_returnsDirectCount() {
        when(userRepository.countByRoleRequestedTrueAndOrganizationRequestedTrue()).thenReturn(15L);
        long count = userService.getAllRequestedCount();
        assertEquals(15L, count);
    }
}