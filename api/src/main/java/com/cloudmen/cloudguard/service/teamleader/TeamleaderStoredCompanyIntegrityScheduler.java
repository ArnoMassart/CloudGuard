package com.cloudmen.cloudguard.service.teamleader;

import com.cloudmen.cloudguard.domain.model.Organization;
import com.cloudmen.cloudguard.repository.OrganizationRepository;
import com.cloudmen.cloudguard.service.OrganizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;
import java.util.Map;

@Component
public class TeamleaderStoredCompanyIntegrityScheduler {

    private static final Logger log =  LoggerFactory.getLogger(TeamleaderStoredCompanyIntegrityScheduler.class);

    private final OrganizationRepository organizationRepository;
    private final OrganizationService organizationService;
    private final TeamleaderCompanyService teamleaderCompanyService;
    private final TeamleaderService teamleaderService;

    @Value("${cloudguard.teamleader.stored-company-integrity.enabled:false}")
    private boolean enabled;

    public TeamleaderStoredCompanyIntegrityScheduler(OrganizationRepository organizationRepository, OrganizationService organizationService, TeamleaderCompanyService teamleaderCompanyService, TeamleaderService teamleaderService) {
        this.organizationRepository = organizationRepository;
        this.organizationService = organizationService;
        this.teamleaderCompanyService = teamleaderCompanyService;
        this.teamleaderService = teamleaderService;
    }

    @Scheduled(cron = "${cloudguard.teamleader.stored-company-integrity.cron: 0 0 3 * * *}")
    public void validateStoredTeamleaderCompanyIds(){
        if(!enabled){
            return;
        }

        List<Organization> orgs = organizationRepository.findAllWithTeamleaderCompanyIdSet();
        if(orgs.isEmpty()){
            return;
        }

        HttpHeaders headers = teamleaderService.createHeaders();
        int cleared = 0;
        int errors = 0;

        for(Organization org: orgs){
            String companyId = org.getTeamleaderCompanyId();
            Long orgId = org.getId();
            try{
                Map<String, Object> details = teamleaderCompanyService.getCompanyDetails(companyId,headers);
                if (details == null) {
                    log.warn(
                            "Teamleader integrity: companies.info returned null for companyId={}, clearing stored id for organizationId={}",
                            companyId,
                            orgId
                    );
                    organizationService.clearTeamleaderCompanyId(orgId);
                    cleared++;
                }
            }catch(HttpClientErrorException e){
                int code = e.getStatusCode().value();
                if(code == 404){
                    log.warn(
                            "Teamleader integrity: company not found (404) for companyId={}, clearing stored id for organizationId={}",
                            companyId,
                            orgId
                    );
                    organizationService.clearTeamleaderCompanyId(orgId);
                    cleared++;
                }else{
                    log.warn(
                            "Teamleader integrity: companies.info HTTP {} for organizationId={}, companyId={}",
                            code,
                            orgId,
                            companyId
                    );
                    errors++;
                }
            }catch(Exception e){
                log.warn(
                        "Teamleader integrity: unexpected error for organizationId={}, companyId={}",
                        orgId,
                        companyId,
                        e.getMessage()
                );
                errors++;
            }
        }

        log.info(
                "Teamleader stored company integrity run finished: orgsChecked={}, cleared={}, httpOrOtherErrors={}",
                orgs.size(),
                cleared,
                errors
        );



    }
}
