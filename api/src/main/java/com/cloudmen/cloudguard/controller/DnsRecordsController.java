package com.cloudmen.cloudguard.controller;

import com.cloudmen.cloudguard.dto.dns.DnsRecordResponseDto;
import com.cloudmen.cloudguard.service.JwtService;
import com.cloudmen.cloudguard.service.dns.DnsLookupService;
import com.cloudmen.cloudguard.service.dns.DnsRecordsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/google/dns-records")
public class DnsRecordsController {
    private final DnsRecordsService dnsRecordsService;

    public DnsRecordsController(DnsRecordsService dnsRecordsService) {
        this.dnsRecordsService = dnsRecordsService;
    }

    @GetMapping("/records")
    public DnsRecordResponseDto records(
            @RequestParam String domain,
            @RequestParam(defaultValue = "google") String dkimSelector
    ) {
        return dnsRecordsService.getImportantRecords(domain, dkimSelector);
    }
}
