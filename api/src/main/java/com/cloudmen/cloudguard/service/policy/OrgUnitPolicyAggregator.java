package com.cloudmen.cloudguard.service.policy;

import com.cloudmen.cloudguard.dto.organization.OrgUnitPolicyDto;
import com.cloudmen.cloudguard.service.OrganizationService;
import com.cloudmen.cloudguard.service.user.UserService;
import com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Runs every Spring-registered {@link OrgUnitPolicyProvider} for a chosen OU path, resolves the workspace admin for the
 * logged-in user, and returns a merged list. Providers are invoked in {@link Order} annotation order; failures yield
 * a localized “not found” placeholder row instead of failing the whole response.
 */
@Service
public class OrgUnitPolicyAggregator {
    private static final Logger log = LoggerFactory.getLogger(OrgUnitPolicyAggregator.class);

    private final List<OrgUnitPolicyProvider> providers;
    private final MessageSource messageSource;
    private final UserService userService;
    private final OrganizationService organizationService;

    /**
     * @param providers             all {@link OrgUnitPolicyProvider} beans (sorted by {@link Order#value()})
     * @param messageSource         fallback strings when a provider throws
     * @param userService           resolves admin email from the viewer’s login
     * @param organizationService   tenant context for admin resolution
     */
    public OrgUnitPolicyAggregator(
            List<OrgUnitPolicyProvider> providers,
            @Qualifier("messageSource") MessageSource messageSource,
            UserService userService,
            OrganizationService organizationService) {
        this.providers =
                providers.stream()
                        .sorted(
                                Comparator.comparingInt(
                                        p -> {
                                            Order o = p.getClass().getAnnotation(Order.class);
                                            return o != null ? o.value() : Integer.MAX_VALUE;
                                        }))
                        .toList();
        this.messageSource = messageSource;
        this.userService = userService;
        this.organizationService = organizationService;
    }

    /**
     * Evaluates each provider against {@code orgUnitPath} for the tenant backing {@code loggedInEmail}.
     *
     * @param loggedInEmail authenticated CloudGuard user
     * @param orgUnitPath   OU path; blank defaults to {@code "/"}
     * @return one {@link com.cloudmen.cloudguard.dto.organization.OrgUnitPolicyDto} per registered provider, in {@link Order} sequence
     */
    public List<OrgUnitPolicyDto> getPolicies(String loggedInEmail, String orgUnitPath) {
        String adminEmail = GoogleServiceHelperMethods.getAdminEmailForUser(loggedInEmail, userService, organizationService);

        Locale locale = LocaleContextHolder.getLocale();

        String path = (orgUnitPath == null || orgUnitPath.isBlank()) ? "/" : orgUnitPath.trim();
        List<OrgUnitPolicyDto> result = new ArrayList<>();
        for (OrgUnitPolicyProvider provider : providers) {
            try {
                result.add(provider.fetch(adminEmail, path));
            } catch (Exception e) {
                log.warn("Policy provider {} failed for OU {}: {}", provider.key(), path, e.getMessage());
                result.add(
                        new OrgUnitPolicyDto(
                                provider.key(),
                                messageSource.getMessage("orgUnits.policy.title.not_found", null, locale),
                                messageSource.getMessage("orgUnits.policy.description.not_found", null, locale),
                                messageSource.getMessage("orgUnits.policy.status.not_found", null, locale),
                                "bg-amber-100 text-amber-800",
                                messageSource.getMessage("orgUnits.policy.base_explanation.not_found", null, locale),
                                messageSource.getMessage("orgUnits.policy.inheritance_explanation.not_found", null, locale),
                                false,
                                null,
                                null));
            }
        }
        return result;
    }
}
