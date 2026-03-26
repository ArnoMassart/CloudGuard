package com.cloudmen.cloudguard.service.policy;

import com.cloudmen.cloudguard.dto.organization.OrgUnitPolicyDto;
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

@Service
public class OrgUnitPolicyAggregator {
    private static final Logger log = LoggerFactory.getLogger(OrgUnitPolicyAggregator.class);

    private final List<OrgUnitPolicyProvider> providers;
    private final MessageSource messageSource;

    public OrgUnitPolicyAggregator(List<OrgUnitPolicyProvider> providers, @Qualifier("messageSource") MessageSource messageSource) {
        this.providers = providers.stream()
                .sorted(Comparator.comparingInt(p -> {
                    Order o = p.getClass().getAnnotation(Order.class);
                    return o != null ? o.value() : Integer.MAX_VALUE;
                }))
                .toList();
        this.messageSource = messageSource;
    }

    public List<OrgUnitPolicyDto> getPolicies(String loggedInEmail, String orgUnitPath) {
        Locale locale = LocaleContextHolder.getLocale();

        String path = (orgUnitPath == null || orgUnitPath.isBlank()) ? "/" : orgUnitPath.trim();
        List<OrgUnitPolicyDto> result = new ArrayList<>();
        for (OrgUnitPolicyProvider provider : providers) {
            try {
                result.add(provider.fetch(loggedInEmail, path));
            } catch (Exception e) {
                log.warn("Policy provider {} failed for OU {}: {}", provider.key(), path, e.getMessage());
                result.add(new OrgUnitPolicyDto(
                        provider.key(),
                        messageSource.getMessage("orgUnits.policy.title.not_found", null, locale),
                        messageSource.getMessage("orgUnits.policy.description.not_found", null, locale),
                        messageSource.getMessage("orgUnits.policy.status.not_found", null, locale),
                        "bg-amber-100 text-amber-800",
                        messageSource.getMessage("orgUnits.policy.base_explanation.not_found", null, locale),
                        messageSource.getMessage("orgUnits.policy.inheritance_explanation.not_found", null, locale),
                        false,
                        null,
                        null
                ));
            }
        }
        return result;
    }
}
