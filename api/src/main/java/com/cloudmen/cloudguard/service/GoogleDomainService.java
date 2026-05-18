package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.dto.domain.DomainDto;
import com.cloudmen.cloudguard.service.cache.GoogleDomainCacheService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Application facade over {@link GoogleDomainCacheService} for workspace domain listing and manual refresh.
 *
 * @see com.cloudmen.cloudguard.controller.GoogleDomainController
 */
@Service
public class GoogleDomainService {

    private final GoogleDomainCacheService domainCacheService;

    public GoogleDomainService(GoogleDomainCacheService domainCacheService) {
        this.domainCacheService = domainCacheService;
    }

    /** Invalidates and refetches domain data on next read for {@code loggedInEmail}’s tenant. */
    public void forceRefreshCache(String loggedInEmail) {
        domainCacheService.forceRefreshCache(loggedInEmail);
    }

    /**
     * All domains and aliases visible to the delegated admin for this login’s organization.
     *
     * @param loggedInEmail authenticated CloudGuard user (maps to workspace admin via org settings)
     */
    public List<DomainDto> getAllDomains(String loggedInEmail) {
        return domainCacheService.getOrFetchData(loggedInEmail).domains();
    }
}
