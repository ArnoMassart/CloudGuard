package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.dto.DomainDto;
import com.cloudmen.cloudguard.utility.GoogleApiFactory;
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

@Service
public class GoogleDomainService {

    private static final Logger log = LoggerFactory.getLogger(GoogleDomainService.class);

    private final GoogleApiFactory apiFactory;

    public GoogleDomainService(GoogleApiFactory apiFactory) {
        this.apiFactory = apiFactory;
    }

    public List<DomainDto> getAllDomains(String adminEmail) {
        log.info("getAllDomains: start voor adminEmail={}", adminEmail);
        try {
            Directory directory = apiFactory.getDirectoryService(
                    Set.of(
                            DirectoryScopes.ADMIN_DIRECTORY_DOMAIN_READONLY,
                            DirectoryScopes.ADMIN_DIRECTORY_USER_READONLY
                    ), adminEmail);
            log.debug("getAllDomains: Directory service aangemaakt");

            List<DomainDto> result = new ArrayList<>();
            String pageToken = null;

            do {
                log.debug("getAllDomains: aanroepen domains().list(my_customer)");
                Domains2 response = directory.domains()
                        .list("my_customer")
                        .execute();

                int domainCount = response.getDomains() != null ? response.getDomains().size() : 0;
                log.info("getAllDomains: response ontvangen, {} domain(s)", domainCount);

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
                        // Add domain aliases 
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

            } while (pageToken != null && !pageToken.isEmpty());

            log.info("getAllDomains: succesvol, {} domain(s) geretourneerd", result.size());
            return result;

        } catch (Exception e) {
            log.error("getAllDomains: fout voor adminEmail={} - {}: {}", adminEmail, e.getClass().getSimpleName(), e.getMessage(), e);
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
