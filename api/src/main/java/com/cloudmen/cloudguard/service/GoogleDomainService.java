package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.dto.domain.DomainDto;
import com.cloudmen.cloudguard.service.cache.GoogleDomainCacheService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GoogleDomainService {

    private final GoogleDomainCacheService domainCacheService;

    public GoogleDomainService(GoogleDomainCacheService domainCacheService) {
        this.domainCacheService = domainCacheService;
    }

    public void forceRefreshCache(String adminEmail) {
        domainCacheService.forceRefreshCache(adminEmail);
    }

    public List<DomainDto> getAllDomains(String adminEmail) {
        return domainCacheService.getOrFetchDomainData(adminEmail).domains();
    }
}
