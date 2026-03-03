package com.cloudmen.cloudguard.service.dns;

import com.cloudmen.cloudguard.dto.dns.DnsRecordDto;
import com.cloudmen.cloudguard.dto.dns.DnsRecordResponseDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DnsRecordsService {
    private final DnsLookupService dns;

    public DnsRecordsService(DnsLookupService dns) {
        this.dns = dns;
    }

    public DnsRecordResponseDto getImportantRecords(String domain, String dkimSelector) {
        List<DnsRecordDto> rows = new ArrayList<>();

        // SPF (TXT at root)
        rows.add(buildSpf(domain));

        // DKIM (TXT at selector._domainkey.domain)
        String dkimName = dkimSelector + "._domainkey." + domain;
        rows.add(buildDkim(dkimName));

        // DMARC (TXT at _dmarc.domain)
        String dmarcName = "_dmarc." + domain;
        rows.add(buildDmarc(dmarcName));

        // MX (root)
        rows.add(buildMx(domain));

        // TXT (root) - include google-site-verification if present
        rows.add(buildSiteVerification(domain));

        // Example CNAME (optional)
        String cnameName = "mail." + domain;
        rows.add(buildCname(cnameName));

        return new DnsRecordResponseDto(domain, rows);
    }

    private DnsRecordDto buildSpf(String domain) {
        DnsLookupResult result = dns.lookupTxt(domain);
        if (!result.isSuccess()) {
            return new DnsRecordDto("SPF", domain, List.of(), "ERROR", result.errorMessage());
        }
        List<String> spf = result.values().stream().filter(v -> v.toLowerCase().startsWith("v=spf1")).toList();

        if (spf.isEmpty()) {
            return new DnsRecordDto("SPF", domain, List.of(), "MISSING", "No SPF TXT record found.");
        }

        boolean hasGoogle = spf.stream().anyMatch(v -> v.contains("include:_spf.google.com"));
        return new DnsRecordDto(
                "SPF",
                domain,
                spf,
                hasGoogle ? "VALID" : "ATTENTION",
                hasGoogle ? "SPF includes Google." : "SPF does not include _spf.google.com."
        );
    }

    private DnsRecordDto buildDkim(String name) {
        DnsLookupResult result = dns.lookupTxt(name);
        if (!result.isSuccess()) {
            return new DnsRecordDto("DKIM", name, List.of(), "ERROR", result.errorMessage());
        }
        List<String> txt = result.values();
        boolean ok = txt.stream().anyMatch(v -> v.toUpperCase().contains("V=DKIM1"));

        if (txt.isEmpty()) {
            return new DnsRecordDto("DKIM", name, List.of(), "MISSING", "No DKIM TXT record found.");
        }
        return new DnsRecordDto("DKIM", name, txt, ok ? "VALID" : "ATTENTION",
                ok ? "DKIM record present." : "TXT found but does not look like DKIM1.");
    }

    private DnsRecordDto buildDmarc(String name) {
        DnsLookupResult result = dns.lookupTxt(name);
        if (!result.isSuccess()) {
            return new DnsRecordDto("DMARC", name, List.of(), "ERROR", result.errorMessage());
        }
        List<String> txt = result.values();
        boolean ok = txt.stream().anyMatch(v -> v.toUpperCase().startsWith("V=DMARC1"));

        if (txt.isEmpty()) {
            return new DnsRecordDto("DMARC", name, List.of(), "MISSING", "No DMARC TXT record found.");
        }
        return new DnsRecordDto("DMARC", name, txt, ok ? "VALID" : "ATTENTION",
                ok ? "DMARC record present." : "TXT found but does not start with v=DMARC1.");
    }

    private DnsRecordDto buildMx(String domain) {
        DnsLookupResult result = dns.lookupMx(domain);
        if (!result.isSuccess()) {
            return new DnsRecordDto("MX", domain, List.of(), "ERROR", result.errorMessage());
        }
        List<String> mx = result.values();

        if (mx.isEmpty()) {
            return new DnsRecordDto("MX", domain, List.of(), "MISSING", "No MX records found.");
        }

        boolean googleMx = mx.stream().anyMatch(v -> v.contains("google.com") || v.contains("googlemail.com"));
        return new DnsRecordDto("MX", domain, mx, googleMx ? "VALID" : "ATTENTION",
                googleMx ? "MX points to Google." : "MX does not appear to point to Google.");
    }

    private DnsRecordDto buildSiteVerification(String domain) {
        DnsLookupResult result = dns.lookupTxt(domain);
        if (!result.isSuccess()) {
            return new DnsRecordDto("TXT", domain, List.of(), "ERROR", result.errorMessage());
        }
        List<String> ver = result.values().stream().filter(v -> v.startsWith("google-site-verification=")).toList();

        if (ver.isEmpty()) {
            return new DnsRecordDto("TXT", domain, List.of(), "OK", "No google-site-verification TXT found (optional).");
        }
        return new DnsRecordDto("TXT", domain, ver, "VALID", "Google site verification present.");
    }

    private DnsRecordDto buildCname(String name) {
        DnsLookupResult result = dns.lookupCname(name);
        if (!result.isSuccess()) {
            return new DnsRecordDto("CNAME", name, List.of(), "ERROR", result.errorMessage());
        }
        List<String> c = result.values();

        if (c.isEmpty()) {
            return new DnsRecordDto("CNAME", name, List.of(), "OK", "No CNAME found (optional).");
        }
        return new DnsRecordDto("CNAME", name, c, "VALID", "CNAME present.");
    }
}
