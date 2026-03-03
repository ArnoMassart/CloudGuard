package com.cloudmen.cloudguard.dto.domain;


public record DomainDto(
        String domainName,
        String domainType,
        Boolean isVerified,
        Integer totalUsers
) {
}
