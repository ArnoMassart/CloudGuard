package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.domain.model.Organization;
import com.cloudmen.cloudguard.domain.model.User;
import com.cloudmen.cloudguard.dto.users.UserDto;
import com.cloudmen.cloudguard.repository.OrganizationRepository;
import com.cloudmen.cloudguard.repository.UserRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private static final String ORGANIZATION_FALLBACK_DISPLAY_MESSAGE_KEY =
            "api.organization.fallback_display_name";

    private static final String ORGANIZATION_FALLBACK_DISPLAY_DEFAULT =
            "Organization (workspace customer could not be resolved)";

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final MessageSource messageSource;

    public UserService(
            UserRepository userRepository,
            OrganizationRepository organizationRepository,
            @Qualifier("messageSource") MessageSource messageSource) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.messageSource = messageSource;
    }

    public UserDto convertToDto(User user) {
        UserDto dto = new UserDto(
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPictureUrl(),
                user.getCreatedAt()
        );
        Long orgId = user.getOrganizationId();
        if (orgId != null) {
            organizationRepository
                    .findById(orgId)
                    .ifPresent(
                            org -> {
                                String displayName;
                                if (org.getCustomerId() == null || org.getCustomerId().isBlank()) {
                                    displayName =
                                            messageSource.getMessage(
                                                    ORGANIZATION_FALLBACK_DISPLAY_MESSAGE_KEY,
                                                    null,
                                                    ORGANIZATION_FALLBACK_DISPLAY_DEFAULT,
                                                    LocaleContextHolder.getLocale());
                                } else {
                                    displayName = org.getName();
                                }
                                dto.setOrganizationName(displayName);
                            });
        }
        return dto;
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
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
}
