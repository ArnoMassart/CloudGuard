package com.cloudmen.cloudguard.service.dns;

import java.util.List;

/**
 * Result of a DNS lookup. Distinguishes between successful lookup (possibly empty)
 * and lookup failure (network error, timeout, etc.).
 */
public record DnsLookupResult(List<String> values, String errorMessage) {

    public static DnsLookupResult success(List<String> values) {
        return new DnsLookupResult(values != null ? values : List.of(), null);
    }

    public static DnsLookupResult failure(String errorMessage) {
        return new DnsLookupResult(List.of(), errorMessage);
    }

    public boolean isSuccess() {
        return errorMessage == null;
    }
}
