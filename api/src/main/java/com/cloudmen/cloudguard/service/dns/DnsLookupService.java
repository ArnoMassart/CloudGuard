package com.cloudmen.cloudguard.service.dns;

import com.cloudmen.cloudguard.dto.dns.DnsLookupResult;
import org.springframework.stereotype.Service;
import org.xbill.DNS.Record;
import org.xbill.DNS.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Thin wrapper around dnsjava ({@link Lookup}) for synchronous TXT/MX/CNAME/DNSKEY/CAA queries used by {@link DnsRecordsService}.
 */
@Service
public class DnsLookupService {

    /** TXT at {@code name} (apex or subdomain). */
    public DnsLookupResult lookupTxt(String name) {
        return lookup(name, Type.TXT);
    }

    /** MX records at {@code name}. */
    public DnsLookupResult lookupMx(String name) {
        return lookup(name, Type.MX);
    }

    /** CNAME at {@code name} (e.g. {@code mail.example.com}). */
    public DnsLookupResult lookupCname(String name) {
        return lookup(name, Type.CNAME);
    }

    /** DNSKEY at {@code name} (used as DNSSEC signal at zone apex). */
    public DnsLookupResult lookupDnsKey(String name) {
        return lookup(name, Type.DNSKEY);
    }

    /** CAA records at {@code name}. */
    public DnsLookupResult lookupCaa(String name) {
        return lookup(name, Type.CAA);
    }

    /**
     * Executes {@link Lookup#run()} and normalizes {@link Record} payloads into strings (TXT joined, MX with priority, etc.).
     */
    private DnsLookupResult lookup(String name, int type) {
        try {
            Name n = Name.fromString(ensureDot(name));
            Record[] records = new Lookup(n, type).run();

            List<String> values = new ArrayList<>();
            if (records != null) {
                for (Record r : records) {
                    if (r instanceof TXTRecord txt) {
                        values.add(String.join("", txt.getStrings()));
                    } else if (r instanceof MXRecord mx) {
                        values.add(mx.getPriority() + " " + mx.getTarget().toString(true));
                    } else if (r instanceof CNAMERecord c) {
                        values.add(c.getTarget().toString(true));
                    } else {
                        values.add(r.rdataToString());
                    }
                }
            }
            return DnsLookupResult.success(values);
        } catch (Exception e) {
            return DnsLookupResult.failure(e.getMessage());
        }
    }

    /** dnsjava expects absolute names with a trailing dot. */
    private String ensureDot(String name) {
        return name.endsWith(".") ? name : name + ".";
    }
}
