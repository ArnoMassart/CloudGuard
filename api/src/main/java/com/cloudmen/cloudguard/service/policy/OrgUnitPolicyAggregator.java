package com.cloudmen.cloudguard.service.policy;

import com.cloudmen.cloudguard.dto.OrgUnitPolicyDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class OrgUnitPolicyAggregator {
    private static final String SETTINGS_LINK_TEXT = "Klik hier om deze instellingen aan te passen";

    private final List<OrgUnitPolicyProvider> providers;

    public OrgUnitPolicyAggregator(List<OrgUnitPolicyProvider> providers) {
        this.providers = providers;
    }

    public List<OrgUnitPolicyDto> getPolicies(String loggedInEmail, String orgUnitPath) {
        List<OrgUnitPolicyDto> result = new ArrayList<>();
        for (OrgUnitPolicyProvider provider : providers) {
            try {
                result.add(provider.fetch(loggedInEmail, orgUnitPath));
            } catch (Exception e) {
                result.add(new OrgUnitPolicyDto(
                        provider.key(),
                        "Title niet opgehaald",
                        "Beschrijving kon niet opgehaald worden",
                        "Kon niet ophalen",
                        "bg-amber-100 text-amber-800",
                        "Beleidsregel kon niet opgehaald worden.",
                        false,
                        "N/A",
                        SETTINGS_LINK_TEXT,
                        null
                ));
            }
        }
        return result;
    }
}
