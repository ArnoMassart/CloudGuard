package com.cloudmen.cloudguard.service.report;

import com.cloudmen.cloudguard.domain.model.Organization;
import com.cloudmen.cloudguard.domain.model.User;
import com.cloudmen.cloudguard.dto.report.ReportResponse;
import com.cloudmen.cloudguard.service.OrganizationService;
import com.cloudmen.cloudguard.service.UserService;
import com.cloudmen.cloudguard.service.cache.CacheWarmupService;
import com.cloudmen.cloudguard.service.teamleader.TeamleaderAccessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class BiannualReportScheduler {
    private static final Logger log = LoggerFactory.getLogger(BiannualReportScheduler.class);

    private final PdfReportService reportService;
    private final SecurityReportEmailService emailService;

    private final UserService userService;
    private final TeamleaderAccessService teamleaderAccessService;
    private final CacheWarmupService cacheWarmupService;
    private final OrganizationService organizationService;

    public BiannualReportScheduler(PdfReportService reportService, SecurityReportEmailService emailService, UserService userService, TeamleaderAccessService teamleaderAccessService, CacheWarmupService cacheWarmupService, OrganizationService organizationService) {
        this.reportService = reportService;
        this.emailService = emailService;
        this.userService = userService;
        this.teamleaderAccessService = teamleaderAccessService;
        this.cacheWarmupService = cacheWarmupService;
        this.organizationService = organizationService;
    }

    /**
     * CRON: "0 0 8 1 1,7 *" means:
     * At 08:00:00 AM, on the 1st day of the month, only in January and July.
     */
    @Scheduled(cron = "0 0 8 1 1,7 *")
    public void generateAndSendReports() {
        log.info("Start automatische generatie van halfjaarlijkse Security Rapporten...");
        List<String> emails = userService.getAllEmails();

        Set<Long> processedOrganizations = new HashSet<>();

        for (String email : emails) {
            User user = userService.findByEmailOptional(email).orElse(null);

            if (user == null || user.getOrganizationId() == null || user.getOrganizationId() == 0) {
                continue;
            }

            Long orgId = user.getOrganizationId();

            if (processedOrganizations.contains(orgId)) {
                continue;
            }

            processedOrganizations.add(orgId);

            Organization org = organizationService.findById(orgId).orElse(null);

            if (org == null) continue;

            String targetEmail = (org.getAdminEmail() != null && !org.getAdminEmail().isBlank())
                    ? org.getAdminEmail()
                    : email;

            if (!teamleaderAccessService.hasCloudGuardAccess(targetEmail)) continue;

            try {
                log.info("Bezig met rapport voor: {}", targetEmail);

                String userLang = userService.getLanguage(targetEmail);

                Locale locale = Locale.forLanguageTag(userLang);

                cacheWarmupService.warmupAllCachesAsync(targetEmail).join();

                ReportResponse response = reportService.generateSecurityReport(targetEmail, locale);

                String companyName = response.companyName();

                emailService.sendSecurityReportEmail(targetEmail, companyName, response.data(), locale);

                log.info("Rapport succesvol verstuurd naar: {} ({})", targetEmail, companyName);
            } catch (Exception e) {
                log.error("Fout bij het genereren/versturen van rapport voor {}: {}", targetEmail, e.getMessage(), e);
            }
        }

        log.info("Klaar met het versturen van alle halfjaarlijkse rapporten.");
    }
}
