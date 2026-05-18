package com.cloudmen.cloudguard.service.policy;

import com.cloudmen.cloudguard.dto.organization.OrgUnitPolicyDto;

/**
 * Supplies one category of Google Workspace policy evaluated at a specific organizational unit path (Chrome, Drive,
 * 2SV, mobile management, etc.). Implementations are collected by {@link OrgUnitPolicyAggregator} and ordered with
 * {@link org.springframework.core.annotation.Order}.
 */
public interface OrgUnitPolicyProvider {
    /** Unique key for logging and fallback rows when {@link #fetch} fails. */
    String key();

    /**
     * Loads live policy state for the OU {@code orgUnitPath} using APIs impersonating {@code adminEmail}.
     *
     * @param adminEmail   workspace admin resolved for the tenant
     * @param orgUnitPath  Directory path such as {@code /} or {@code /Engineering}
     * @return localized card payload for this provider and OU
     */
    OrgUnitPolicyDto fetch(String adminEmail, String orgUnitPath) throws Exception;
}
