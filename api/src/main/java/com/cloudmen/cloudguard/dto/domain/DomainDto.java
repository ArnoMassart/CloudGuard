package com.cloudmen.cloudguard.dto.domain;

/**
 * One Google Workspace domain or alias as returned by the Admin SDK domains API.
 *
 * @param domainName   FQDN (primary, secondary, or alias hostname)
 * @param domainType   UI label such as {@code Primary Domain}, {@code Secondary Domain}, or {@code Domain alias}
 * @param isVerified   {@link com.google.api.services.admin.directory.model.Domains#getVerified()} mirror
 * @param totalUsers   approximate Directory user count for that domain ({@code 0} for aliases in current mapping)
 */
public record DomainDto(
        String domainName,
        String domainType,
        Boolean isVerified,
        Integer totalUsers
) {
}
