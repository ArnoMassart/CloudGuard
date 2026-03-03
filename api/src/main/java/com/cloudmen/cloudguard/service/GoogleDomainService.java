package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.dto.DomainDto;
import com.cloudmen.cloudguard.utility.GoogleApiFactory;
import com.google.api.services.admin.directory.Directory;
import com.google.api.services.admin.directory.DirectoryScopes;
import com.google.api.services.admin.directory.model.Domains;
import com.google.api.services.admin.directory.model.Domains2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

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
            Directory directory = apiFactory.getDirectoryService(DirectoryScopes.ADMIN_DIRECTORY_DOMAIN_READONLY, adminEmail);
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
                        result.add(new DomainDto(
                                domain.getDomainName(),
                                domain.getKind(),
                                Boolean.TRUE.equals(domain.getIsPrimary()),
                                Boolean.TRUE.equals(domain.getVerified())
                        ));
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
}
