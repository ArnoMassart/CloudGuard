package com.cloudmen.cloudguard.service.dns;

import com.cloudmen.cloudguard.dto.dns.DnsLookupResult;
import org.springframework.stereotype.Service;
import org.xbill.DNS.Record;
import org.xbill.DNS.*;

import java.util.ArrayList;
import java.util.List;

@Service
public class DnsLookupService {

    public DnsLookupResult lookupTxt(String name) {
        return lookup(name, Type.TXT);
    }

    public DnsLookupResult lookupMx(String name) {
        return lookup(name, Type.MX);
    }

    public DnsLookupResult lookupCname(String name) {
        return lookup(name, Type.CNAME);
    }

    public DnsLookupResult lookupDnsKey(String name) {
        return lookup(name, Type.DNSKEY);
    }

    public DnsLookupResult lookupCaa(String name) {
        return lookup(name, Type.CAA);
    }

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

    private String ensureDot(String name) {
        return name.endsWith(".") ? name : name + ".";
    }
}
