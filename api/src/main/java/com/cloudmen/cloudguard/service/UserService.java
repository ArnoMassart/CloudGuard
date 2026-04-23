package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.domain.model.User;
import com.cloudmen.cloudguard.domain.model.UserRole;
import com.cloudmen.cloudguard.dto.users.DatabaseUsersResponse;
import com.cloudmen.cloudguard.dto.users.UserDto;
import com.cloudmen.cloudguard.exception.UserNotFoundException;
import com.cloudmen.cloudguard.repository.OrganizationRepository;
import com.cloudmen.cloudguard.repository.UserRepository;
import com.cloudmen.cloudguard.utility.GoogleApiFactory;
import com.google.api.services.admin.directory.DirectoryScopes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;

    private static final String ORGANIZATION_FALLBACK_DISPLAY_MESSAGE_KEY =
            "api.organization.fallback_display_name";

    private static final String ORGANIZATION_FALLBACK_DISPLAY_DEFAULT =
            "Organization (workspace customer could not be resolved)";

    private final OrganizationRepository organizationRepository;
    private final MessageSource messageSource;
    private final AccessRequestEmailService accessRequestEmailService;
    private final OrganizationService organizationService;
    private final GoogleApiFactory googleApiFactory;

    public UserService(
            UserRepository userRepository,
            OrganizationRepository organizationRepository,
            @Qualifier("messageSource") MessageSource messageSource, AccessRequestEmailService accessRequestEmailService, OrganizationService organizationService, GoogleApiFactory googleApiFactory) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.messageSource = messageSource;
        this.accessRequestEmailService = accessRequestEmailService;
        this.organizationService = organizationService;
        this.googleApiFactory = googleApiFactory;
    }

    public UserDto convertToDto(User user) {
        String organizationName = "";
        Long orgId = user.getOrganizationId();

        if (orgId != null) {
            organizationName = organizationRepository.findById(orgId)
                    .map(org -> {
                        if (org.getCustomerId() == null || org.getCustomerId().isBlank()) {
                            return messageSource.getMessage(
                                    ORGANIZATION_FALLBACK_DISPLAY_MESSAGE_KEY,
                                    null,
                                    ORGANIZATION_FALLBACK_DISPLAY_DEFAULT,
                                    LocaleContextHolder.getLocale());
                        } else {
                            return org.getName();
                        }
                    })
                    .orElse("");
        }

        return new UserDto(
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPictureUrl(),
                user.getRoles(),
                user.getCreatedAt(),
                user.isRoleRequested(),
                user.isOrganizationRequested(),
                orgId,
                organizationName

        );
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

    public boolean getRoleRequested(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);

        return userOptional.map(User::isRoleRequested).orElse(false);
    }

    @Transactional
    public void updateRequestAccess(String email) {
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

    public boolean hasOrganization(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            return user.getOrganizationId() != null && user.getOrganizationId() != 0;
        }

        return false;
    }

    public DatabaseUsersResponse getAll(String pageToken, int size, String query, String orgIdFilter) {
        return fetchUsersFromDb(pageToken, size, query, orgIdFilter, true);
    }

    public DatabaseUsersResponse getAllWithRequestedRoleAndOrganization(String pageToken, int size, String query) {
        return fetchUsersFromDb(pageToken, size, query, "", false);
    }

    private DatabaseUsersResponse fetchUsersFromDb(String pageToken, int size, String query, String orgIdFilter, boolean withoutRequests) {
        int page = (pageToken == null || pageToken.isEmpty()) ? 0 : Integer.parseInt(pageToken);

        Pageable pageable = PageRequest.of(page, size, Sort.by("firstName").ascending());

        Page<User> userPage;

        if (withoutRequests) {
            // Safely parse the String to a Long. If it's empty/null, keep it null.
            Long parsedOrgId = null;
            if (orgIdFilter != null && !orgIdFilter.trim().isEmpty() && !orgIdFilter.equals("all")) {
                parsedOrgId = Long.parseLong(orgIdFilter);
            }

            userPage = userRepository.findAllWithoutRequested(parsedOrgId, query, pageable);
        } else {
            userPage = userRepository.findAllByRoleRequestedWithSearch(query, pageable);
        }

        String nextPageToken = userPage.hasNext() ? String.valueOf(page + 1) : null;

        List<UserDto> filteredList = userPage.getContent().stream().map(this::convertToDto).toList();

        return new DatabaseUsersResponse(filteredList, nextPageToken);
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

    public boolean hasRole(String userEmail, UserRole role) {
        User user = findByEmail(userEmail);

        return user.getRoles().contains(role);
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
}
