package com.cloudmen.cloudguard.unit.service.preference;

import com.cloudmen.cloudguard.domain.model.DnsRecordImportance;
import com.cloudmen.cloudguard.domain.model.preference.UserSecurityPreference;
import com.cloudmen.cloudguard.service.preference.DnsImportancePreferenceSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DnsImportancePreferenceSupportTest {

    @Test
    void systemDefaultImportance_knownTypes() {
        Assertions.assertEquals(DnsRecordImportance.REQUIRED, DnsImportancePreferenceSupport.systemDefaultImportance("SPF"));
        assertEquals(DnsRecordImportance.RECOMMENDED, DnsImportancePreferenceSupport.systemDefaultImportance("DNSSEC"));
        assertEquals(DnsRecordImportance.OPTIONAL, DnsImportancePreferenceSupport.systemDefaultImportance("TXT"));
    }

    @Test
    void systemDefaultImportance_unknownTypeIsOptional() {
        assertEquals(DnsRecordImportance.OPTIONAL, DnsImportancePreferenceSupport.systemDefaultImportance("UNKNOWN"));
    }

    @Test
    void preferenceKeyForType_roundTrip() {
        assertEquals("impSpf", DnsImportancePreferenceSupport.preferenceKeyForType("SPF"));
        assertEquals("impCname", DnsImportancePreferenceSupport.preferenceKeyForType("CNAME"));
    }

    @Test
    void isDnsImportancePreferenceKey() {
        assertTrue(DnsImportancePreferenceSupport.isDnsImportancePreferenceKey("impSpf"));
        assertFalse(DnsImportancePreferenceSupport.isDnsImportancePreferenceKey("2fa"));
        assertFalse(DnsImportancePreferenceSupport.isDnsImportancePreferenceKey(null));
    }

    @Test
    void parseOverridesFromDbRows_filtersSectionAndInvalidValues() {
        UserSecurityPreference good = pref("domain-dns", "impSpf", "OPTIONAL");
        UserSecurityPreference wrongSection = pref("other", "impSpf", "REQUIRED");
        UserSecurityPreference badEnum = pref("domain-dns", "impMx", "not-an-enum");
        UserSecurityPreference blank = pref("domain-dns", "impDkim", "  ");
        UserSecurityPreference nonImp = pref("domain-dns", "toggle", "REQUIRED");

        var out = DnsImportancePreferenceSupport.parseOverridesFromDbRows(
                List.of(good, wrongSection, badEnum, blank, nonImp));

        assertEquals(1, out.size());
        assertEquals(DnsRecordImportance.OPTIONAL, out.get("SPF"));
    }

    @Test
    void dnsTypeForPreferenceKey() {
        assertEquals("DMARC", DnsImportancePreferenceSupport.dnsTypeForPreferenceKey("impDmarc"));
        assertNull(DnsImportancePreferenceSupport.dnsTypeForPreferenceKey("impUnknown"));
    }

    @Test
    void preferenceKeyForType_unknownType_returnsNull() {
        assertNull(DnsImportancePreferenceSupport.preferenceKeyForType("NOT_A_DNS_TYPE"));
    }

    @Test
    void systemDefaultsMap_containsExpectedEntriesAndIsCopy() {
        var map = DnsImportancePreferenceSupport.systemDefaultsMap();
        map.put("SPF", DnsRecordImportance.OPTIONAL);
        assertEquals(DnsRecordImportance.REQUIRED, DnsImportancePreferenceSupport.systemDefaultImportance("SPF"));
    }

    private static UserSecurityPreference pref(String section, String key, String value) {
        UserSecurityPreference p = new UserSecurityPreference();
        p.setSection(section);
        p.setPreferenceKey(key);
        p.setPreferenceValue(value);
        return p;
    }
}
