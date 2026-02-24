package com.cloudmen.cloudguard.service.policy;

import com.cloudmen.cloudguard.dto.OrgUnitPolicyDto;
import com.cloudmen.cloudguard.service.GoogleDirectoryFactory;
import com.google.api.services.admin.directory.Directory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.*;

@Order(3)
@Component
public class ExternalGroupMembersProvider implements OrgUnitPolicyProvider {
    private static final String GROUP_READONLY_SCOPE = "https://www.googleapis.com/auth/admin.directory.group.readonly";
    private static final String CUSTOMER_READONLY_SCOPE = "https://www.googleapis.com/auth/admin.directory.customer.readonly";
    private static final String SETTINGS_LINK_TEXT = "Klik hier om deze instellingen aan te passen";

    private final GoogleDirectoryFactory directoryFactory;

    public ExternalGroupMembersProvider(GoogleDirectoryFactory directoryFactory) {
        this.directoryFactory = directoryFactory;
    }

    @Override public String key() { return "external_group_members"; }

    @Override
    public OrgUnitPolicyDto fetch(String adminEmail, String orgUnitPath) throws Exception {
        Directory directory = directoryFactory.getDirectoryService(
                Set.of(GROUP_READONLY_SCOPE, CUSTOMER_READONLY_SCOPE), adminEmail);

        String customerDomain = directory.customers().get("my_customer").execute().getCustomerDomain();
        if (customerDomain == null || customerDomain.isBlank()) {
            return new OrgUnitPolicyDto(key(),
                    "Externe leden in groepen",
                    "Detectie van externe members",
                    "Kon niet ophalen",
                    "bg-amber-100 text-amber-800",
                    "Kon klantdomein niet ophalen (Directory API).",
                    false,
                    "Directory API",
                    SETTINGS_LINK_TEXT,
                    "groups");
        }
        final String domainSuffix = "@" + customerDomain.toLowerCase();

        List<String> riskyGroups = new ArrayList<>();

        String pageToken = null;
        do {
            var req = directory.groups().list()
                    .setCustomer("my_customer")
                    .setMaxResults(200);
            if (pageToken != null) req.setPageToken(pageToken);

            var groups = req.execute();
            if (groups.getGroups() != null) {
                for (var g : groups.getGroups()) {
                    var memReq = directory.members().list(g.getId()).setMaxResults(200);
                    var members = memReq.execute();

                    if (members.getMembers() != null) {
                        boolean hasExternal = members.getMembers().stream()
                                .map(m -> m.getEmail() == null ? "" : m.getEmail().toLowerCase())
                                .anyMatch(email -> !domainSuffix.isEmpty() && !email.endsWith(domainSuffix));

                        if (hasExternal) riskyGroups.add(g.getEmail());
                    }
                }
            }

            pageToken = groups.getNextPageToken();
        } while (pageToken != null && !pageToken.isBlank());

        String status;
        String css;
        if (riskyGroups.isEmpty()) {
            status = "Geen externe leden";
            css = "bg-green-100 text-green-800";
        } else {
            status = "Externe leden (" + riskyGroups.size() + ")";
            css = "bg-amber-100 text-amber-800";
        }

        return new OrgUnitPolicyDto(
                key(),
                "Externe leden in groepen",
                "Detectie van externe members",
                status,
                css,
                "Dit is gedetecteerd via group membership (Directory API).",
                false,
                "Directory API",
                SETTINGS_LINK_TEXT,
                "groups"
        );
    }
}

