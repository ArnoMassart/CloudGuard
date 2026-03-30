package com.cloudmen.cloudguard.service.dns;

import com.cloudmen.cloudguard.domain.model.DnsRecordImportance;
import com.cloudmen.cloudguard.domain.model.DnsRecordStatus;
import com.cloudmen.cloudguard.dto.dns.DnsLookupResult;
import com.cloudmen.cloudguard.dto.password.SecurityScoreFactorDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DnsRecordsServiceTest {

    private static final String DOMAIN = "example.com";
    private static final String DKIM_SEL = "google";

    @Mock
    DnsLookupService dnsLookupService;

    private ResourceBundleMessageSource messageSource;
    private DnsRecordsService service;

    @BeforeEach
    void setUp() {
        messageSource = new ResourceBundleMessageSource();
        messageSource.setBasenames("messages");
        messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
        messageSource.setFallbackToSystemLocale(false);
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        service = new DnsRecordsService(dnsLookupService, messageSource);
    }

    @AfterEach
    void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void getImportantRecords_happyPath_allRequiredRecommendedValid_weightedScore80() {
        stubHappyLookups();

        var response = service.getImportantRecords(DOMAIN, DKIM_SEL);

        assertEquals(DOMAIN, response.domain());
        assertEquals(8, response.rows().size());

        var spf = response.rows().stream().filter(r -> "SPF".equals(r.type())).findFirst().orElseThrow();
        assertEquals(DnsRecordStatus.VALID, spf.status());
        assertEquals(DnsRecordImportance.REQUIRED, spf.importance());

        assertEquals(80, response.securityScore());
        assertEquals("good", response.securityScoreBreakdown().status());
        assertEquals(8, response.securityScoreBreakdown().factors().size());
    }

    @Test
    void getImportantRecords_allImportanceOptional_securityScore100() {
        when(dnsLookupService.lookupTxt(eq(DOMAIN))).thenReturn(
                DnsLookupResult.success(List.of()),
                DnsLookupResult.success(List.of()));
        when(dnsLookupService.lookupTxt(eq(DKIM_SEL + "._domainkey." + DOMAIN)))
                .thenReturn(DnsLookupResult.success(List.of()));
        when(dnsLookupService.lookupTxt(eq("_dmarc." + DOMAIN)))
                .thenReturn(DnsLookupResult.success(List.of()));
        when(dnsLookupService.lookupMx(eq(DOMAIN)))
                .thenReturn(DnsLookupResult.success(List.of()));
        when(dnsLookupService.lookupDnsKey(eq(DOMAIN)))
                .thenReturn(DnsLookupResult.success(List.of()));
        when(dnsLookupService.lookupCaa(eq(DOMAIN)))
                .thenReturn(DnsLookupResult.success(List.of()));
        when(dnsLookupService.lookupCname(eq("mail." + DOMAIN)))
                .thenReturn(DnsLookupResult.success(List.of()));

        Map<String, DnsRecordImportance> allOptional = new HashMap<>();
        for (var type : List.of("SPF", "DKIM", "DMARC", "MX", "DNSSEC", "CAA", "TXT", "CNAME")) {
            allOptional.put(type, DnsRecordImportance.OPTIONAL);
        }

        var response = service.getImportantRecords(DOMAIN, DKIM_SEL, allOptional);

        assertEquals(100, response.securityScore());
        assertTrue(response.securityScoreBreakdown().factors().stream().allMatch(SecurityScoreFactorDto::muted));
    }

    @Test
    void getImportantRecords_spfTxtLookupFails_spfErrorAndScoreReduced() {
        when(dnsLookupService.lookupTxt(eq(DOMAIN))).thenReturn(
                DnsLookupResult.failure("timeout"),
                DnsLookupResult.success(List.of()));
        stubRemainingAfterSpfFailure();

        var response = service.getImportantRecords(DOMAIN, DKIM_SEL);

        var spf = response.rows().stream().filter(r -> "SPF".equals(r.type())).findFirst().orElseThrow();
        assertEquals(DnsRecordStatus.ERROR, spf.status());
        assertNotNull(spf.message());
        assertTrue(response.securityScore() < 80);
    }

    @Test
    void getImportantRecords_noSpfWhenRequired_actionRequired() {
        when(dnsLookupService.lookupTxt(eq(DOMAIN))).thenReturn(
                DnsLookupResult.success(List.of("some other txt")),
                DnsLookupResult.success(List.of()));
        stubDkimDmarcMxDnssecCaaCname();

        var response = service.getImportantRecords(DOMAIN, DKIM_SEL);

        var spf = response.rows().stream().filter(r -> "SPF".equals(r.type())).findFirst().orElseThrow();
        assertEquals(DnsRecordStatus.ACTION_REQUIRED, spf.status());
        assertTrue(spf.values().isEmpty());
    }

    @Test
    void getImportantRecords_spfWithoutGoogleWhenRequired_attention() {
        when(dnsLookupService.lookupTxt(eq(DOMAIN))).thenReturn(
                DnsLookupResult.success(List.of("v=spf1 ~all")),
                DnsLookupResult.success(List.of()));
        stubDkimDmarcMxDnssecCaaCname();

        var response = service.getImportantRecords(DOMAIN, DKIM_SEL);

        var spf = response.rows().stream().filter(r -> "SPF".equals(r.type())).findFirst().orElseThrow();
        assertEquals(DnsRecordStatus.ATTENTION, spf.status());
        assertFalse(spf.values().isEmpty());
    }

    @Test
    void getImportantRecords_nullImportanceOverrides_treatedAsEmptyMap() {
        stubHappyLookups();

        var response = service.getImportantRecords(DOMAIN, DKIM_SEL, null);

        assertEquals(80, response.securityScore());
    }

    @Test
    void getImportantRecords_delegatesLookupsToDnsService() {
        stubHappyLookups();

        service.getImportantRecords(DOMAIN, DKIM_SEL);

        verify(dnsLookupService, times(2)).lookupTxt(DOMAIN);
        verify(dnsLookupService).lookupTxt(DKIM_SEL + "._domainkey." + DOMAIN);
        verify(dnsLookupService).lookupTxt("_dmarc." + DOMAIN);
        verify(dnsLookupService).lookupMx(DOMAIN);
        verify(dnsLookupService).lookupDnsKey(DOMAIN);
        verify(dnsLookupService).lookupCaa(DOMAIN);
        verify(dnsLookupService).lookupCname("mail." + DOMAIN);
    }

    private void stubHappyLookups() {
        when(dnsLookupService.lookupTxt(eq(DOMAIN))).thenReturn(
                DnsLookupResult.success(List.of("v=spf1 include:_spf.google.com ~all")),
                DnsLookupResult.success(List.of("google-site-verification=abc")));
        stubDkimDmarcMxDnssecCaaCname();
    }

    private void stubDkimDmarcMxDnssecCaaCname() {
        when(dnsLookupService.lookupTxt(eq(DKIM_SEL + "._domainkey." + DOMAIN)))
                .thenReturn(DnsLookupResult.success(List.of("v=DKIM1; p=xx")));
        when(dnsLookupService.lookupTxt(eq("_dmarc." + DOMAIN)))
                .thenReturn(DnsLookupResult.success(List.of("v=DMARC1; p=none")));
        when(dnsLookupService.lookupMx(eq(DOMAIN)))
                .thenReturn(DnsLookupResult.success(List.of("1 aspmx.l.google.com.")));
        when(dnsLookupService.lookupDnsKey(eq(DOMAIN)))
                .thenReturn(DnsLookupResult.success(List.of("257 3 8 Kw==")));
        when(dnsLookupService.lookupCaa(eq(DOMAIN)))
                .thenReturn(DnsLookupResult.success(List.of("0 issue \"letsencrypt.org\"")));
        when(dnsLookupService.lookupCname(eq("mail." + DOMAIN)))
                .thenReturn(DnsLookupResult.success(List.of("ghs.googlehosted.com.")));
    }

    private void stubRemainingAfterSpfFailure() {
        when(dnsLookupService.lookupTxt(eq(DKIM_SEL + "._domainkey." + DOMAIN)))
                .thenReturn(DnsLookupResult.success(List.of("v=DKIM1; p=xx")));
        when(dnsLookupService.lookupTxt(eq("_dmarc." + DOMAIN)))
                .thenReturn(DnsLookupResult.success(List.of("v=DMARC1; p=none")));
        when(dnsLookupService.lookupMx(eq(DOMAIN)))
                .thenReturn(DnsLookupResult.success(List.of("1 aspmx.l.google.com.")));
        when(dnsLookupService.lookupDnsKey(eq(DOMAIN)))
                .thenReturn(DnsLookupResult.success(List.of("257 3 8 Kw==")));
        when(dnsLookupService.lookupCaa(eq(DOMAIN)))
                .thenReturn(DnsLookupResult.success(List.of("0 issue \"letsencrypt.org\"")));
        when(dnsLookupService.lookupCname(eq("mail." + DOMAIN)))
                .thenReturn(DnsLookupResult.success(List.of("ghs.googlehosted.com.")));
    }
}
