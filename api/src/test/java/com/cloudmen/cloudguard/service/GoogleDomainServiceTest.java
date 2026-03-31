package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.dto.domain.DomainCacheEntry;
import com.cloudmen.cloudguard.dto.domain.DomainDto;
import com.cloudmen.cloudguard.service.cache.GoogleDomainCacheService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.cloudmen.cloudguard.utility.GlobalTestHelper.ADMIN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleDomainServiceTest {

    @Mock
    GoogleDomainCacheService domainCacheService;

    @InjectMocks
    GoogleDomainService service;

    @Test
    void forceRefreshCache_delegatesToCacheService() {
        service.forceRefreshCache(ADMIN);
        verify(domainCacheService).forceRefreshCache(ADMIN);
    }

    @Test
    void getAllDomains_returnsDomainsFromCacheEntry() {
        List<DomainDto> domains = List.of(
                new DomainDto("primary.com", "PRIMARY", true, 42),
                new DomainDto("alias.com", "ALIAS", false, 0));
        when(domainCacheService.getOrFetchDomainData(ADMIN))
                .thenReturn(new DomainCacheEntry(domains, 1L));

        List<DomainDto> result = service.getAllDomains(ADMIN);

        assertEquals(2, result.size());
        assertSame(domains, result);
        assertEquals("primary.com", result.get(0).domainName());
        assertEquals(true, result.get(0).isVerified());
    }
}
