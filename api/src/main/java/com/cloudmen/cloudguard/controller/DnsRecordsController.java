package com.cloudmen.cloudguard.controller;

import com.cloudmen.cloudguard.dto.dns.DnsRecordResponseDto;
import com.cloudmen.cloudguard.service.JwtService;
import com.cloudmen.cloudguard.service.dns.DnsRecordsService;
import com.cloudmen.cloudguard.service.preference.UserSecurityPreferenceService;
import org.springframework.web.bind.annotation.*;

/**
 * REST surface for live DNS checks (SPF, DKIM, DMARC, MX, DNSSEC, CAA, site verification, mail CNAME) scoped to a domain.
 * Applies per-user importance overrides from security preferences ({@code domain-dns} section).
 *
 * @see DnsRecordsService
 */
@RestController
@RequestMapping("/google/dns-records")
public class DnsRecordsController {
    private final DnsRecordsService dnsRecordsService;
    private final JwtService jwtService;
    private final UserSecurityPreferenceService preferenceService;

    /**
     * @param dnsRecordsService  orchestrates {@link com.cloudmen.cloudguard.service.dns.DnsLookupService} and scoring
     * @param jwtService         validates {@code AuthToken} into internal user id/email key used for preferences
     * @param preferenceService  supplies DNS importance overrides for {@code userId}
     */
    public DnsRecordsController(DnsRecordsService dnsRecordsService, JwtService jwtService, UserSecurityPreferenceService preferenceService) {
        this.dnsRecordsService = dnsRecordsService;
        this.jwtService = jwtService;
        this.preferenceService = preferenceService;
    }

    /**
     * Runs the standard checklist against {@code domain}. The DKIM TXT name is
     * {@code dkimSelector + "._domainkey." + domain}.
     */
    @GetMapping("/records")
    public DnsRecordResponseDto records(
            @CookieValue(name = "AuthToken", required = false) String token,
            @RequestParam String domain,
            @RequestParam(defaultValue = "google") String dkimSelector
    ) {
        String userId = jwtService.validateInternalToken(token);
        var overrides = preferenceService.getDnsImportanceOverrides(userId);
        return dnsRecordsService.getImportantRecords(domain, dkimSelector, overrides);
    }
}
