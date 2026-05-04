package com.cloudmen.cloudguard.service.user;

import com.cloudmen.cloudguard.domain.model.User;
import com.cloudmen.cloudguard.dto.users.UserDto;
import com.cloudmen.cloudguard.repository.OrganizationRepository;
import com.cloudmen.cloudguard.repository.UserRepository;
import com.cloudmen.cloudguard.service.CloudguardStaffService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

/**
 * Component responsible for mapping the internal User database entity to the frontend-ready UserDto, resolving
 * organization names and staff roles in the process.
 */
@Component
public class UserMapper {
    private static final String ORGANIZATION_FALLBACK_DISPLAY_MESSAGE_KEY =
            "api.organization.fallback_display_name";

    private static final String ORGANIZATION_FALLBACK_DISPLAY_DEFAULT =
            "Organization (workspace customer could not be resolved)";
    private final OrganizationRepository organizationRepository;
    private final MessageSource messageSource;
    private final CloudguardStaffService cloudguardStaffService;

    public UserMapper(OrganizationRepository organizationRepository, @Qualifier("messageSource") MessageSource messageSource, CloudguardStaffService cloudguardStaffService ) {
        this.organizationRepository = organizationRepository;
        this.messageSource = messageSource;
        this.cloudguardStaffService = cloudguardStaffService;
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
                user.isActive(),
                user.isRoleRequested(),
                user.isOrganizationRequested(),
                user.isAccessRequested(),
                user.isAccessAccepted(),
                user.isAccessDenied(),
                orgId,
                organizationName,
                cloudguardStaffService.isCloudmenAdmin(user.getEmail())

        );
    }
}
