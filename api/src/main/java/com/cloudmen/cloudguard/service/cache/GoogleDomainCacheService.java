package com.cloudmen.cloudguard.service.cache;

import com.cloudmen.cloudguard.dto.DomainCacheEntry;
import com.cloudmen.cloudguard.dto.DomainDto;
import com.cloudmen.cloudguard.utility.GoogleApiFactory;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.api.services.admin.directory.Directory;
import com.google.api.services.admin.directory.DirectoryScopes;
import com.google.api.services.admin.directory.model.DomainAlias;
import com.google.api.services.admin.directory.model.DomainAliases;
import com.google.api.services.admin.directory.model.Domains;
import com.google.api.services.admin.directory.model.Domains2;
import com.google.api.services.admin.directory.model.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class GoogleDomainCacheService {
    private static final Logger log = LoggerFactory.getLogger(GoogleDomainCacheService.class);

    private final GoogleApiFactory apiFactory;

    private final Cache<String, DomainCacheEntry> cache = Caffeine.newBuilder()
            .expireAfterWrite(4, TimeUnit.HOURS)
            .maximumSize(100)
            .build();

    public GoogleDomainCacheService(GoogleApiFactory apiFactory) {
        this.apiFactory = apiFactory;
    }

    public void forceRefreshCache(String adminEmail) {
        cache.asMap().compute(adminEmail, this::fetchFromGoogle);
    }

    public DomainCacheEntry getOrFetchDomainData(String adminEmail) {
        return cache.get(adminEmail, email -> fetchFromGoogle(email, null));
    }

    private DomainCacheEntry fetchFromGoogle(String adminEmail, DomainCacheEntry fallbackEntry) {
        try {
            log.info("Ophalen LIVE domain data van Google voor: {}", adminEmail);
            Directory directory = apiFactory.getDirectoryService(
                    Set.of(
                            DirectoryScopes.ADMIN_DIRECTORY_DOMAIN_READONLY,
                            DirectoryScopes.ADMIN_DIRECTORY_USER_READONLY
                    ), adminEmail);

            List<DomainDto> result = new ArrayList<>();
            Domains2 response = directory.domains().list("my_customer").execute();

            if (response.getDomains() != null) {
                for (Domains domain : response.getDomains()) {
                    boolean primary = Boolean.TRUE.equals(domain.getIsPrimary());
                    String domainType = primary ? "Primary Domain" : "Secondary Domain";
                    int userCount = countUsersForDomain(directory, domain.getDomainName());
                    result.add(new DomainDto(
                            domain.getDomainName(),
                            domainType,
                            Boolean.TRUE.equals(domain.getVerified()),
                            userCount
                    ));

                    List<DomainAlias> aliases = domain.getDomainAliases();
                    if (aliases == null || aliases.isEmpty()) {
                        try {
                            DomainAliases aliasResponse = directory.domainAliases()
                                    .list("my_customer")
                                    .setParentDomainName(domain.getDomainName())
                                    .execute();
                            if (aliasResponse.getDomainAliases() != null) {
                                aliases = aliasResponse.getDomainAliases();
                            }
                        } catch (Exception ex) {
                            log.debug("Geen domain aliases voor {}: {}", domain.getDomainName(), ex.getMessage());
                        }
                    }
                    if (aliases != null) {
                        for (DomainAlias alias : aliases) {
                            result.add(new DomainDto(
                                    alias.getDomainAliasName(),
                                    "Domain alias",
                                    Boolean.TRUE.equals(alias.getVerified()),
                                    0
                            ));
                        }
                    }
                }
            }

            log.info("getAllDomains: succesvol, {} domain(s) geretourneerd", result.size());
            return new DomainCacheEntry(result, System.currentTimeMillis());

        } catch (Exception e) {
            if (fallbackEntry != null) {
                log.error("Google API faalde! Terugvallen op oude cache: {}", e.getMessage());
                return fallbackEntry;
            }
            log.error("Fout bij ophalen Google domains: {}", e.getMessage(), e);
            throw new RuntimeException("Domains ophalen mislukt: " + e.getMessage(), e);
        }
    }

    private int countUsersForDomain(Directory directory, String domainName) {
        int count = 0;
        String pageToken = null;
        try {
            do {
                Directory.Users.List request = directory.users()
                        .list()
                        .setDomain(domainName)
                        .setMaxResults(500);
                if (pageToken != null) {
                    request.setPageToken(pageToken);
                }
                Users users = request.execute();
                if (users.getUsers() != null) {
                    count += users.getUsers().size();
                }
                pageToken = users.getNextPageToken();
            } while (pageToken != null && !pageToken.isEmpty());
        } catch (IOException e) {
            log.warn("Kon gebruikers niet tellen voor domain {}: {}", domainName, e.getMessage());
        }
        return count;
    }
}
