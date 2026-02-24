package com.cloudmen.cloudguard.service.policy;

import com.cloudmen.cloudguard.dto.OrgUnitPolicyDto;
import com.cloudmen.cloudguard.service.GoogleDirectoryFactory;
import com.google.api.services.admin.directory.Directory;
import com.google.api.services.admin.directory.model.User;
import com.google.api.services.admin.directory.model.Users;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Set;

@Order(1)
@Component
public class TSVPolicyProvider implements OrgUnitPolicyProvider {
    private static final String DIRECTORY_USER_SECURITY_SCOPE = "https://www.googleapis.com/auth/admin.directory.user.security";
    private static final String DIRECTORY_USER_READONLY_SCOPE = "https://www.googleapis.com/auth/admin.directory.user.readonly";
    private static final String SETTINGS_LINK_TEXT = "Klik hier om deze instellingen aan te passen";

    private final GoogleDirectoryFactory directoryFactory;

    public TSVPolicyProvider(GoogleDirectoryFactory directoryFactory) {
        this.directoryFactory = directoryFactory;
    }

    @Override public String key() {return "2sv_adoption";}
    @Override
    public OrgUnitPolicyDto fetch(String loggedInEmail, String orgUnitPath) throws Exception {
        Set<String> scopes = Set.of(DIRECTORY_USER_READONLY_SCOPE, DIRECTORY_USER_SECURITY_SCOPE);
        Directory directory = directoryFactory.getDirectoryService(scopes, loggedInEmail);

        int total = 0;
        int without2sv = 0;

        String pageToken = null;
        do {
            Directory.Users.List req = directory.users().list()
                    .setCustomer("my_customer")
                    .setQuery("orgUnitPath='" + orgUnitPath.replace("'", "\\'") + "'")
                    .setMaxResults(200);

            if (pageToken != null) req.setPageToken(pageToken);

            Users users = req.execute();
            if (users.getUsers() != null) {
                for (User u : users.getUsers()) {
                    total++;
                    Boolean enrolled = u.getIsEnrolledIn2Sv();
                    if (enrolled == null || !enrolled) without2sv++;
                }
            }
            pageToken = users.getNextPageToken();
        } while (pageToken != null && !pageToken.isBlank());

        String status;
        String css;

        if (total == 0) {
            status = "Geen gebruikers";
            css = "bg-slate-100 text-slate-700";
        } else if (without2sv == 0) {
            status = "OK (" + total + "/" + total + ")";
            css = "bg-green-100 text-green-800";
        } else {
            status = "Risico (" + without2sv + "/" + total + " zonder 2SV)";
            css = "bg-amber-100 text-amber-800";
        }

        String baseExplanation = "Deze beleidsregel toont de adoptie van tweestapsverificatie (2SV) onder gebruikers in deze organisatie-eenheid. Het gaat om compliance op basis van user-data.";

        return new OrgUnitPolicyDto(
                key(),
                "Tweestapsverificatie (2SV) adoptie",
                "Aantal gebruikers met 2SV in deze OU",
                status,
                css,
                baseExplanation,
                null,
                false,
                SETTINGS_LINK_TEXT,
                "https://admin.google.com/u/1/ac/security/2sv"
        );
    }
}
