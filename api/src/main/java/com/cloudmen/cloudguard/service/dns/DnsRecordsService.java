package com.cloudmen.cloudguard.service.dns;

import com.cloudmen.cloudguard.dto.dns.DnsLookupResult;
import com.cloudmen.cloudguard.dto.dns.DnsRecordDto;
import com.cloudmen.cloudguard.domain.model.DnsRecordImportance;
import com.cloudmen.cloudguard.dto.dns.DnsRecordResponseDto;
import com.cloudmen.cloudguard.domain.model.DnsRecordStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DnsRecordsService {
    private final DnsLookupService dns;

    public DnsRecordsService(DnsLookupService dns) {
        this.dns = dns;
    }

    private DnsRecordDto errorRecord(String type, String name, DnsRecordImportance importance, DnsLookupResult result) {
        return new DnsRecordDto(type, name, List.of(), DnsRecordStatus.ERROR, importance, result.errorMessage());
    }

    private DnsRecordStatus resolveStatus(DnsRecordImportance importance, boolean found, boolean optimal) {
        if (!found) {
            return switch (importance) {
                case REQUIRED -> DnsRecordStatus.ACTION_REQUIRED;
                case RECOMMENDED -> DnsRecordStatus.ATTENTION;
                case OPTIONAL -> DnsRecordStatus.OK;
            };
        }
        if (!optimal) {
            return switch (importance) {
                case REQUIRED, RECOMMENDED -> DnsRecordStatus.ATTENTION;
                case OPTIONAL -> DnsRecordStatus.OK;
            };
        }
        return DnsRecordStatus.VALID;
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

        // DNSSEC (DNSKEY at root) - recommended
        rows.add(buildDnssec(domain));

        // CAA (at root) - recommended
        rows.add(buildCaa(domain));

        // TXT (root) - include google-site-verification if present (optional)
        rows.add(buildSiteVerification(domain));

        // Example CNAME (optional)
        String cnameName = "mail." + domain;
        rows.add(buildCname(cnameName));

        int securityScore = calculateSecurityScore(rows);

        return new DnsRecordResponseDto(domain, rows, securityScore);
    }

    private int calculateSecurityScore(List<DnsRecordDto> rows) {
        if (rows == null || rows.isEmpty()) return 0;

        double score = 0;
        for (DnsRecordDto row: rows) {
            int weight = switch (row.importance()){
                case REQUIRED -> 15;
                case RECOMMENDED -> 10;
                case OPTIONAL -> 5;
            };

            double mult = switch (row.status()) {
                case VALID -> 1.0;
                case OK,ATTENTION -> 0.5;
                case ACTION_REQUIRED, ERROR -> 0.0;
            };

            score += weight * mult;
        }

        return (int) Math.round(Math.min(100.0, score));
    }

    private DnsRecordDto buildSpf(String domain) {
        DnsRecordImportance importance = DnsRecordImportance.REQUIRED;
        DnsLookupResult result = dns.lookupTxt(domain);
        if (!result.isSuccess()) {
            return errorRecord("SPF", domain, importance, result);
        }
        List<String> spf = result.values().stream().filter(v -> v.toLowerCase().startsWith("v=spf1")).toList();
        boolean found = !spf.isEmpty();
        boolean optimal = found && spf.stream().anyMatch(v -> v.contains("include:_spf.google.com"));
        String message = found ? (optimal ? "SPF includes Google." : "SPF does not include _spf.google.com.")
                : "No SPF TXT record found.";
        return new DnsRecordDto("SPF", domain, spf, resolveStatus(importance, found, optimal), importance, message);
    }

    private DnsRecordDto buildDkim(String name) {
        DnsRecordImportance importance = DnsRecordImportance.REQUIRED;
        DnsLookupResult result = dns.lookupTxt(name);
        if (!result.isSuccess()) {
            return errorRecord("DKIM", name, importance, result);
        }
        List<String> txt = result.values();
        boolean found = !txt.isEmpty();
        boolean optimal = found && txt.stream().anyMatch(v -> v.toUpperCase().contains("V=DKIM1"));
        String message = found ? (optimal ? "DKIM record present." : "TXT found but does not look like DKIM1.")
                : "No DKIM TXT record found.";
        return new DnsRecordDto("DKIM", name, txt, resolveStatus(importance, found, optimal), importance, message);
    }

    private DnsRecordDto buildDmarc(String name) {
        DnsRecordImportance importance = DnsRecordImportance.REQUIRED;
        DnsLookupResult result = dns.lookupTxt(name);
        if (!result.isSuccess()) {
            return errorRecord("DMARC", name, importance, result);
        }
        List<String> txt = result.values();
        boolean found = !txt.isEmpty();
        boolean optimal = found && txt.stream().anyMatch(v -> v.toUpperCase().startsWith("V=DMARC1"));
        String message = found ? (optimal ? "DMARC record present." : "TXT found but does not start with v=DMARC1.")
                : "No DMARC TXT record found.";
        return new DnsRecordDto("DMARC", name, txt, resolveStatus(importance, found, optimal), importance, message);
    }

    private DnsRecordDto buildMx(String domain) {
        DnsRecordImportance importance = DnsRecordImportance.REQUIRED;
        DnsLookupResult result = dns.lookupMx(domain);
        if (!result.isSuccess()) {
            return errorRecord("MX", domain, importance, result);
        }
        List<String> mx = result.values();
        boolean found = !mx.isEmpty();
        boolean optimal = found && mx.stream().anyMatch(v -> v.contains("google.com") || v.contains("googlemail.com"));
        String message = found ? (optimal ? "MX points to Google." : "MX does not appear to point to Google.")
                : "No MX records found.";
        return new DnsRecordDto("MX", domain, mx, resolveStatus(importance, found, optimal), importance, message);
    }

    private DnsRecordDto buildDnssec(String domain) {
        DnsRecordImportance importance = DnsRecordImportance.RECOMMENDED;
        DnsLookupResult result = dns.lookupDnsKey(domain);
        if (!result.isSuccess()) {
            return errorRecord("DNSSEC", domain, importance, result);
        }
        List<String> keys = result.values();
        boolean found = !keys.isEmpty();
        String message = found ? "DNSKEY records present – DNSSEC configured." : "No DNSKEY records found.";
        return new DnsRecordDto("DNSSEC", domain, keys, resolveStatus(importance, found, found), importance, message);
    }

    private DnsRecordDto buildCaa(String domain) {
        DnsRecordImportance importance = DnsRecordImportance.RECOMMENDED;
        DnsLookupResult result = dns.lookupCaa(domain);
        if (!result.isSuccess()) {
            return errorRecord("CAA", domain, importance, result);
        }
        List<String> caa = result.values();
        boolean found = !caa.isEmpty();
        String message = found ? "CAA records present." : "No CAA records found.";
        return new DnsRecordDto("CAA", domain, caa, resolveStatus(importance, found, found), importance, message);
    }

    private DnsRecordDto buildSiteVerification(String domain) {
        DnsRecordImportance importance = DnsRecordImportance.OPTIONAL;
        DnsLookupResult result = dns.lookupTxt(domain);
        if (!result.isSuccess()) {
            return errorRecord("TXT", domain, importance, result);
        }
        List<String> ver = result.values().stream().filter(v -> v.startsWith("google-site-verification=")).toList();
        boolean found = !ver.isEmpty();
        String message = found ? "Google site verification present." : "No google-site-verification TXT found (optional).";
        return new DnsRecordDto("TXT", domain, ver, resolveStatus(importance, found, found), importance, message);
    }

    private DnsRecordDto buildCname(String name) {
        DnsRecordImportance importance = DnsRecordImportance.OPTIONAL;
        DnsLookupResult result = dns.lookupCname(name);
        if (!result.isSuccess()) {
            return errorRecord("CNAME", name, importance, result);
        }
        List<String> c = result.values();
        boolean found = !c.isEmpty();
        String message = found ? "CNAME present." : "No CNAME found (optional).";
        return new DnsRecordDto("CNAME", name, c, resolveStatus(importance, found, found), importance, message);
    }
}
