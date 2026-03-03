package com.cloudmen.cloudguard.service.dns;

import com.cloudmen.cloudguard.dto.dns.DnsLookupResult;
import com.cloudmen.cloudguard.dto.dns.DnsRecordDto;
import com.cloudmen.cloudguard.dto.dns.DnsRecordResponseDto;
import com.cloudmen.cloudguard.dto.dns.DnsRecordStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DnsRecordsService {
    private final DnsLookupService dns;

    public DnsRecordsService(DnsLookupService dns) {
        this.dns = dns;
    }

    private DnsRecordDto errorRecord(String type, String name, DnsLookupResult result) {
        return new DnsRecordDto(type, name, List.of(), DnsRecordStatus.ERROR, result.errorMessage());
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
            return errorRecord("SPF", domain, result);
        }
        List<String> spf = result.values().stream().filter(v -> v.toLowerCase().startsWith("v=spf1")).toList();

        if (spf.isEmpty()) {
            return new DnsRecordDto("SPF", domain, List.of(), DnsRecordStatus.MISSING, "No SPF TXT record found.");
        }

        boolean hasGoogle = spf.stream().anyMatch(v -> v.contains("include:_spf.google.com"));
        return new DnsRecordDto(
                "SPF",
                domain,
                spf,
                hasGoogle ? DnsRecordStatus.VALID : DnsRecordStatus.ATTENTION,
                hasGoogle ? "SPF includes Google." : "SPF does not include _spf.google.com."
        );
    }

    private DnsRecordDto buildDkim(String name) {
        DnsLookupResult result = dns.lookupTxt(name);
        if (!result.isSuccess()) {
            return errorRecord("DKIM", name, result);
        }
        List<String> txt = result.values();
        boolean ok = txt.stream().anyMatch(v -> v.toUpperCase().contains("V=DKIM1"));

        if (txt.isEmpty()) {
            return new DnsRecordDto("DKIM", name, List.of(), DnsRecordStatus.MISSING, "No DKIM TXT record found.");
        }
        return new DnsRecordDto("DKIM", name, txt, ok ? DnsRecordStatus.VALID : DnsRecordStatus.ATTENTION,
                ok ? "DKIM record present." : "TXT found but does not look like DKIM1.");
    }

    private DnsRecordDto buildDmarc(String name) {
        DnsLookupResult result = dns.lookupTxt(name);
        if (!result.isSuccess()) {
            return errorRecord("DMARC", name, result);
        }
        List<String> txt = result.values();
        boolean ok = txt.stream().anyMatch(v -> v.toUpperCase().startsWith("V=DMARC1"));

        if (txt.isEmpty()) {
            return new DnsRecordDto("DMARC", name, List.of(), DnsRecordStatus.MISSING, "No DMARC TXT record found.");
        }
        return new DnsRecordDto("DMARC", name, txt, ok ? DnsRecordStatus.VALID : DnsRecordStatus.ATTENTION,
                ok ? "DMARC record present." : "TXT found but does not start with v=DMARC1.");
    }

    private DnsRecordDto buildMx(String domain) {
        DnsLookupResult result = dns.lookupMx(domain);
        if (!result.isSuccess()) {
            return errorRecord("MX", domain, result);
        }
        List<String> mx = result.values();

        if (mx.isEmpty()) {
            return new DnsRecordDto("MX", domain, List.of(), DnsRecordStatus.MISSING, "No MX records found.");
        }

        boolean googleMx = mx.stream().anyMatch(v -> v.contains("google.com") || v.contains("googlemail.com"));
        return new DnsRecordDto("MX", domain, mx, googleMx ? DnsRecordStatus.VALID : DnsRecordStatus.ATTENTION,
                googleMx ? "MX points to Google." : "MX does not appear to point to Google.");
    }

    private DnsRecordDto buildSiteVerification(String domain) {
        DnsLookupResult result = dns.lookupTxt(domain);
        if (!result.isSuccess()) {
            return errorRecord("TXT", domain, result);
        }
        List<String> ver = result.values().stream().filter(v -> v.startsWith("google-site-verification=")).toList();

        if (ver.isEmpty()) {
            return new DnsRecordDto("TXT", domain, List.of(), DnsRecordStatus.OK, "No google-site-verification TXT found (optional).");
        }
        return new DnsRecordDto("TXT", domain, ver, DnsRecordStatus.VALID, "Google site verification present.");
    }

    private DnsRecordDto buildCname(String name) {
        DnsLookupResult result = dns.lookupCname(name);
        if (!result.isSuccess()) {
            return errorRecord("CNAME", name, result);
        }
        List<String> c = result.values();

        if (c.isEmpty()) {
            return new DnsRecordDto("CNAME", name, List.of(), DnsRecordStatus.OK, "No CNAME found (optional).");
        }
        return new DnsRecordDto("CNAME", name, c, DnsRecordStatus.VALID, "CNAME present.");
    }
}
