package com.cloudmen.cloudguard.controller;

import com.cloudmen.cloudguard.dto.dns.DnsRecordResponseDto;
import com.cloudmen.cloudguard.service.JwtService;
import com.cloudmen.cloudguard.service.dns.DnsRecordsService;
import com.cloudmen.cloudguard.service.preference.UserSecurityPreferenceService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/google/dns-records")
public class DnsRecordsController {
    private final DnsRecordsService dnsRecordsService;
    private final JwtService jwtService;
    private final UserSecurityPreferenceService preferenceService;

    public DnsRecordsController(DnsRecordsService dnsRecordsService, JwtService jwtService, UserSecurityPreferenceService preferenceService) {
        this.dnsRecordsService = dnsRecordsService;
        this.jwtService = jwtService;
        this.preferenceService = preferenceService;
    }

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
