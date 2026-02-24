package com.cloudmen.cloudguard.service.policy;

import com.cloudmen.cloudguard.dto.organization.OrgUnitPolicyDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class OrgUnitPolicyAggregator {
    private static final Logger log = LoggerFactory.getLogger(OrgUnitPolicyAggregator.class);
    private static final String SETTINGS_LINK_TEXT = "Klik hier om deze instellingen aan te passen";

    private final List<OrgUnitPolicyProvider> providers;

    public OrgUnitPolicyAggregator(List<OrgUnitPolicyProvider> providers) {
        this.providers = providers.stream()
                .sorted(Comparator.comparingInt(p -> {
                    Order o = p.getClass().getAnnotation(Order.class);
                    return o != null ? o.value() : Integer.MAX_VALUE;
                }))
                .toList();
    }

    public List<OrgUnitPolicyDto> getPolicies(String loggedInEmail, String orgUnitPath) {
        String path = (orgUnitPath == null || orgUnitPath.isBlank()) ? "/" : orgUnitPath.trim();
        List<OrgUnitPolicyDto> result = new ArrayList<>();
        for (OrgUnitPolicyProvider provider : providers) {
            try {
                result.add(provider.fetch(loggedInEmail, path));
            } catch (Exception e) {
                log.warn("Policy provider {} failed for OU {}: {}", provider.key(), path, e.getMessage());
                result.add(new OrgUnitPolicyDto(
                        provider.key(),
                        "Title niet opgehaald",
                        "Beschrijving kon niet opgehaald worden",
                        "Kon niet ophalen",
                        "bg-amber-100 text-amber-800",
                        "De beleidsregelgegevens zijn tijdelijk niet beschikbaar.",
                        "Er is een fout opgetreden bij het ophalen. Probeer later opnieuw.",
                        false,
                        SETTINGS_LINK_TEXT,
                        null
                ));
            }
        }
        return result;
    }
}
