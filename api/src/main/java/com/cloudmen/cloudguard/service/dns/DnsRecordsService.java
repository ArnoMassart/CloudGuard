package com.cloudmen.cloudguard.service.dns;

import com.cloudmen.cloudguard.domain.model.DnsRecordImportance;
import com.cloudmen.cloudguard.domain.model.DnsRecordStatus;
import com.cloudmen.cloudguard.dto.dns.DnsLookupResult;
import com.cloudmen.cloudguard.dto.dns.DnsRecordDto;
import com.cloudmen.cloudguard.dto.dns.DnsRecordResponseDto;
import com.cloudmen.cloudguard.dto.password.SecurityScoreBreakdownDto;
import com.cloudmen.cloudguard.dto.password.SecurityScoreFactorDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods.severity;

@Service
public class DnsRecordsService {
    private final DnsLookupService dns;
    private final MessageSource messageSource;

    public DnsRecordsService(DnsLookupService dns, @Qualifier("messageSource") MessageSource messageSource) {
        this.dns = dns;
        this.messageSource = messageSource;
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
        SecurityScoreBreakdownDto breakdown = buildDnsBreakdown(rows, securityScore);

        return new DnsRecordResponseDto(domain, rows, securityScore, breakdown);
    }

    private SecurityScoreBreakdownDto buildDnsBreakdown(List<DnsRecordDto> rows, int securityScore) {
        List<SecurityScoreFactorDto> factors = new ArrayList<>();
        for (DnsRecordDto row : rows) {
            double mult = switch (row.status()) {
                case VALID -> 1.0;
                case OK, ATTENTION -> 0.5;
                case ACTION_REQUIRED, ERROR -> 0.0;
            };

            Locale locale = LocaleContextHolder.getLocale();

            int score = (int) Math.round(mult * 100);
            String title = switch (row.type()) {
                case "SPF" -> "SPF Record";
                case "DKIM" -> "DKIM Signing";
                case "DMARC" -> "DMARC Policy";
                case "MX" -> "MX Records";
                case "DNSSEC" -> "DNSSEC";
                case "CAA" -> "CAA Records";
                case "TXT" -> messageSource.getMessage("dns.score.factor.title.txt", null, locale);
                case "CNAME" -> messageSource.getMessage("dns.score.factor.title.cname", null, locale);
                default -> row.type();
            };
            factors.add(new SecurityScoreFactorDto(title, row.message(), score, 100, severity(score)));
        }
        String status = securityScore == 100 ? "perfect" : securityScore >= 75 ? "good" : securityScore > 50 ? "average" : "bad";
        return new SecurityScoreBreakdownDto(securityScore, status, factors);
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

        Locale locale = LocaleContextHolder.getLocale();

        String message = found ? (optimal ?
                messageSource.getMessage("dns.score.factor.description.spf.includes", null, locale)
                : messageSource.getMessage("dns.score.factor.description.spf.includes.not", null, locale))
                : messageSource.getMessage("dns.score.factor.description.spf.not", null, locale);
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

        Locale locale = LocaleContextHolder.getLocale();

        String message = found
                ? (optimal ? messageSource.getMessage("dns.score.factor.description.dkim.present", null, locale)
                : messageSource.getMessage("dns.score.factor.description.dkim.present.not", null, locale))
                : messageSource.getMessage("dns.score.factor.description.dkim.not", null, locale);
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

        Locale locale = LocaleContextHolder.getLocale();

        String message = found ? (optimal ? messageSource.getMessage("dns.score.factor.description.dmarc.present", null, locale)
                : messageSource.getMessage("dns.score.factor.description.dmarc.present.not", null, locale))
                : messageSource.getMessage("dns.score.factor.description.dmarc.not", null, locale);
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

        Locale locale = LocaleContextHolder.getLocale();

        String message = found ? (optimal ? messageSource.getMessage("dns.score.factor.description.mx.points_correct", null, locale)
                : messageSource.getMessage("dns.score.factor.description.mx.present.not", null, locale))
                : messageSource.getMessage("dns.score.factor.description.mx.not", null, locale);
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

        Locale locale = LocaleContextHolder.getLocale();

        String message = found
                ? messageSource.getMessage("dns.score.factor.description.dnssec.present_1", null, locale)+ "–"+ messageSource.getMessage("dns.score.factor.description.dnssec.present_2", null, locale)
                : messageSource.getMessage("dns.score.factor.description.dnssec.not", null, locale);
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

        Locale locale = LocaleContextHolder.getLocale();

        String message = found
                ? messageSource.getMessage("dns.score.factor.description.caa.present", null, locale)
                : messageSource.getMessage("dns.score.factor.description.caa.not", null, locale);
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

        Locale locale = LocaleContextHolder.getLocale();

        String message = found ? messageSource.getMessage("dns.score.factor.description.txt.present", null, locale)
                : messageSource.getMessage("dns.score.factor.description.txt.not", null, locale);
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

        Locale locale = LocaleContextHolder.getLocale();

        String message = found ? messageSource.getMessage("dns.score.factor.description.cname.present", null, locale)
                : messageSource.getMessage("dns.score.factor.description.cname.not", null, locale);
        return new DnsRecordDto("CNAME", name, c, resolveStatus(importance, found, found), importance, message);
    }
}
