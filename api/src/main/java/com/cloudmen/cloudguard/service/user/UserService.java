package com.cloudmen.cloudguard.service.user;

import com.cloudmen.cloudguard.domain.model.User;
import com.cloudmen.cloudguard.domain.model.UserRole;
import com.cloudmen.cloudguard.dto.users.*;
import com.cloudmen.cloudguard.exception.UserNotFoundException;
import com.cloudmen.cloudguard.repository.UserRepository;
import com.cloudmen.cloudguard.service.AccessRequestEmailService;
import com.cloudmen.cloudguard.service.OrganizationService;
import com.cloudmen.cloudguard.utility.DateTimeConverter;
import com.cloudmen.cloudguard.utility.GoogleApiFactory;
import com.google.api.services.admin.directory.DirectoryScopes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * The core orchestration service for managing local application users. <p>
 *
 * This service handles standard CRUD operations, language preferences, role assignments, and onboarding workflows
 * (such as organization and role requests). It also interfaces with the Google Workspace API to verify Domain-Wide
 * Delegation (DWD) setups for administrative users.
 */
@Service
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final AccessRequestEmailService accessRequestEmailService;
    private final OrganizationService organizationService;
    private final GoogleApiFactory googleApiFactory;
    private final UserMapper mapper;

    public UserService(
            UserRepository userRepository, AccessRequestEmailService accessRequestEmailService, OrganizationService organizationService, GoogleApiFactory googleApiFactory, UserMapper mapper) {
        this.userRepository = userRepository;
        this.accessRequestEmailService = accessRequestEmailService;
        this.organizationService = organizationService;
        this.googleApiFactory = googleApiFactory;
        this.mapper = mapper;
    }

    public UserDto convertToDto(User user) {
        return mapper.convertToDto(user);
    }

    public Optional<User> findByEmailOptional(String email) {
        return userRepository.findByEmail(email);
    }

    public User findByEmail(String email) {
        return findByEmailOptional(email).orElseThrow(() -> new UserNotFoundException("User met email: " + email + " niet gevonden"));
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    public List<String> getAllEmails() {
        return userRepository.findAll().stream().map(User::getEmail).toList();
    }

    public String getLanguage(String email) {
        Optional<User> user = userRepository.findByEmail(email);

        if (user.isPresent()) {
            String lang = user.get().getLanguage();

            if (lang == null) return "nl";

            return lang;
        }

        return "nl";
    }

    @Transactional
    public void updateLanguage(String email, String newLanguage) {
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            user.setLanguage(newLanguage);
            userRepository.save(user);
        }
    }


    public boolean getAccessRequested(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);

        return userOptional.map(User::isAccessRequested).orElse(false);
    }

    @Transactional
    public void updateRequestAccess(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            if (!user.isAccessRequested()) {
                user.setAccessRequested(true);
                user.setAccessRequestedAt(LocalDateTime.now());
                userRepository.save(user);

                accessRequestEmailService.notifyAccessRequest(email);
                accessRequestEmailService.sendAccessRequestConfirmationEmailToUser(email, localeForUser(user));
            }
        }
    }

    public boolean getRoleRequested(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);

        return userOptional.map(User::isRoleRequested).orElse(false);
    }

    @Transactional
    public void updateRoleRequestAccess(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            if (!user.isRoleRequested()) {
                user.setRoleRequested(true);
                userRepository.save(user);

                accessRequestEmailService.notifyRoleRequest(email);
            }
        }
    }

    public boolean getNoOrganizationRequested(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);

        return userOptional.map(User::isOrganizationRequested).orElse(false);
    }

    @Transactional
    public void updateRequestNoOrganization(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            if (!user.isOrganizationRequested()) {
                user.setOrganizationRequested(true);
                userRepository.save(user);

                accessRequestEmailService.notifyOrganizationRequest(email);
            }
        }
    }

    public boolean hasValidRole(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            if (user.getRoles() == null || user.getRoles().isEmpty()) {
                return false;
            }

            if (user.getRoles().contains(UserRole.UNASSIGNED)) {
                return false;
            }

            return true;
        }

        return false;
    }

    public boolean hasRole(String email, UserRole role) {
        if (role == null) {
            return false;
        }
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            return false;
        }
        User user = userOptional.get();
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            return false;
        }
        return user.getRoles().contains(role);
    }

    public boolean hasOrganization(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            return user.getOrganizationId() != null && user.getOrganizationId() != 0;
        }

        return false;
    }

    public DatabaseUsersResponse<UserDto> getAll(String pageToken, int size, String query, String orgIdFilter, String statusParam) {
        return fetchUsersFromDb(pageToken, size, query, orgIdFilter, statusParam, true);
    }

    public DatabaseUsersResponse<UserDto> getAllRequested(String pageToken, int size, String query) {
        return fetchUsersFromDb(pageToken, size, query, "", null, false);
    }

    public DatabaseUsersResponse<DeniedUser> getAllDenied(String pageToken, int size, String query) {
        return fetchDeniedUsersFromDb(pageToken, size, query);
    }

    @Transactional
    public void updateRoles(String email, List<UserRole> roles) {
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            if (roles.isEmpty()) {
                user.getRoles().clear();
                user.getRoles().add(UserRole.UNASSIGNED);
            } else {
                user.setRoles(roles);
            }

            user.setRoleRequested(false);

            userRepository.save(user);
        }
    }

    @Transactional
    public void updateRolesAndUpdateRequestedStatus(String email, List<UserRole> roles) {
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            user.setRoles(roles);
            user.setRoleRequested(false);
            userRepository.save(user);
        }
    }

    public long getAllRequestedCount() {
        return userRepository.countByRoleRequestedTrueOrOrganizationRequestedTrue();
    }

    @Transactional
    public void updateUsersOrg(String email, Long orgId) {
        userRepository.findByEmail(email).ifPresent(user -> {
            user.setOrganizationId(orgId);
            user.setOrganizationRequested(false);
            userRepository.save(user);
        });
    }

    public boolean isOrganizationSetup(String email) {
        return userRepository.findByEmail(email)
                .map(User::getOrganizationId)
                .flatMap(organizationService::findById)
                .map(org -> {
                    String adminEmail = org.getAdminEmail();
                    if (adminEmail == null || adminEmail.isBlank()) {
                        return false; // Geen admin e-mail ingesteld [cite: 18]
                    }
                    // Controleer of de Domain-Wide Delegation effectief werkt [cite: 7, 16]
                    return verifyDwdStatus(adminEmail);
                })
                .orElse(false);
    }

    public long getRequestedAccessCount() {
        return userRepository.countByAccessRequestedTrue();
    }

    public long getDeniedCount() {
        return userRepository.countByAccessDeniedTrue();
    }

    public boolean isAccepted(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            return user.isAccessAccepted();
        }

        return false;
    }

    public boolean isDenied(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            return user.isAccessDenied();
        }

        return false;
    }

    public boolean isActive(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            return user.isActive();
        }

        return false;
    }

    public void acceptUser(UserDecisionRequestDto request) {
        User user = findByEmail(request.userEmail());

        user.setAccessAccepted(true);
        user.setAccessDenied(false);

        user.setAccessRequested(false);
        user.setAccessRequestedAt(null);

        user.setOrganizationRequested(false);
        user.setRoleRequested(false);

        user.setAccessDeniedReason(null);
        user.setAccessDeniedAt(null);

        user.setOrganizationId(Long.valueOf(request.organizationId()));
        user.setRoles(request.roles());

        save(user);

        accessRequestEmailService.sendRequestAcceptedEmailToUser(user.getEmail(), localeForUser(user));
    }

    private static Locale localeForUser(User user) {
        String userLang = user.getLanguage() != null ? user.getLanguage() : "nl";
        return Locale.forLanguageTag(userLang.replace('_', '-'));
    }

    public void denyUser(UserDenyRequest request) {
        User user = findByEmail(request.userEmail());

        user.setAccessDenied(true);
        user.setAccessAccepted(false);

        user.setAccessRequested(false);
        user.setAccessRequestedAt(null);

        user.setAccessDeniedReason(request.denyReason());

        user.setAccessDeniedAt(LocalDateTime.now());

        save(user);
    }

    public void reacceptUser(String email) {
        User user = findByEmail(email);
        
        user.setAccessDenied(false);
        user.setAccessAccepted(false);

        user.setAccessRequested(false);
        user.setAccessRequestedAt(null);

        user.setAccessDeniedReason(null);
        user.setAccessDeniedAt(null);

        save(user);
    }

    public void switchUserStatus(String email) {
        User user = findByEmail(email);

        user.setActive(!user.isActive());

        save(user);
    }

    private DatabaseUsersResponse<UserDto> fetchUsersFromDb(String pageToken, int size, String query, String orgIdFilter, String statusParam, boolean withoutRequests) {
        int page = (pageToken == null || pageToken.isEmpty()) ? 0 : Integer.parseInt(pageToken);

        Pageable pageable = PageRequest.of(page, size, Sort.by("firstName").ascending());

        Page<User> userPage;

        if (withoutRequests) {
            // Safely parse the String to a Long. If it's empty/null, keep it null.
            Long parsedOrgId = null;
            if (orgIdFilter != null && !orgIdFilter.trim().isEmpty() && !orgIdFilter.equals("all")) {
                parsedOrgId = Long.parseLong(orgIdFilter);
            }

            Boolean isActive = null;

            if ("active".equalsIgnoreCase(statusParam)) {
                isActive = true;
            } else if ("inactive".equalsIgnoreCase(statusParam)) {
                isActive = false;
            }

            userPage = userRepository.findAllAccepted(parsedOrgId, isActive, query, pageable);
        }else {
            userPage = userRepository.findAllByAccessRequested(query, pageable);
        }

        String nextPageToken = userPage.hasNext() ? String.valueOf(page + 1) : null;

        List<UserDto> filteredList = userPage.getContent().stream().map(this::convertToDto).toList();

        return new DatabaseUsersResponse<>(filteredList, nextPageToken);
    }

    private DatabaseUsersResponse<DeniedUser> fetchDeniedUsersFromDb(String pageToken, int size, String query) {
        int page = (pageToken == null || pageToken.isEmpty()) ? 0 : Integer.parseInt(pageToken);

        Pageable pageable = PageRequest.of(page, size, Sort.by("firstName").ascending());

        Page<User> userPage = userRepository.findAllDenied(query, pageable);

        String nextPageToken = userPage.hasNext() ? String.valueOf(page + 1) : null;

        List<DeniedUser> filteredList = userPage.getContent().stream().map(this::convertToDeniedUser).toList();

        return new DatabaseUsersResponse<>(filteredList, nextPageToken);
    }

    private boolean verifyDwdStatus(String adminEmail) {
        try {
            var directory = googleApiFactory.getDirectoryService(
                    DirectoryScopes.ADMIN_DIRECTORY_USER_READONLY,
                    adminEmail
            );
            // Test call: Haal 1 gebruiker op. Slaagt dit, dan staat DWD aan.
            directory.users().list().setCustomer("my_customer").setMaxResults(1).execute();
            return true;
        } catch (Exception e) {
            // DWD is mogelijk ingetrokken of onjuist geconfigureerd
            return false;
        }
    }

    private DeniedUser convertToDeniedUser(User user) {
        return new DeniedUser(
                getFullName(user),
                user.getEmail(),
                user.getAccessDeniedReason(),
                DateTimeConverter.parseWithPattern(user.getAccessDeniedAt(), "dd-MM-yyyy HH:mm"),
                user.getPictureUrl()
        );
    }

    private String getFullName(User user) {
        return user.getFirstName() + " " + user.getLastName();
    }
}
