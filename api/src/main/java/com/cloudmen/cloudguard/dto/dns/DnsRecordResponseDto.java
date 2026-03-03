package com.cloudmen.cloudguard.dto.dns;

import java.util.List;

public record DnsRecordResponseDto(
        String domain,
        List<DnsRecordDto> rows
) {
}
