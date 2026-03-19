package com.cloudmen.cloudguard.dto.dns;

import com.cloudmen.cloudguard.dto.password.SecurityScoreBreakdownDto;

import java.util.List;

public record DnsRecordResponseDto(
        String domain,
        List<DnsRecordDto> rows,
        int securityScore,
        SecurityScoreBreakdownDto securityScoreBreakdown
) {
}
