package com.cloudmen.cloudguard.controller;

import com.cloudmen.cloudguard.dto.dns.DnsRecordResponseDto;
import com.cloudmen.cloudguard.service.JwtService;
import com.cloudmen.cloudguard.service.dns.DnsRecordsService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/google/dns-records")
public class DnsRecordsController {
    private final DnsRecordsService dnsRecordsService;
    private final JwtService jwtService;

    public DnsRecordsController(DnsRecordsService dnsRecordsService, JwtService jwtService) {
        this.dnsRecordsService = dnsRecordsService;
        this.jwtService = jwtService;
    }

    @GetMapping("/records")
    public DnsRecordResponseDto records(
            @CookieValue(name = "AuthToken", required = false) String token,
            @RequestParam String domain,
            @RequestParam(defaultValue = "google") String dkimSelector
    ) {
        jwtService.validateInternalToken(token);
        return dnsRecordsService.getImportantRecords(domain, dkimSelector);
    }
}
