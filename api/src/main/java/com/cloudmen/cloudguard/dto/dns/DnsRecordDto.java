package com.cloudmen.cloudguard.dto.dns;

import java.util.List;

public record DnsRecordDto(
        String type,
        String name,
        List<String> values,
        DnsRecordStatus status,
        DnsRecordImportance importance,
        String message
) {
}
