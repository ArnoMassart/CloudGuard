package com.cloudmen.cloudguard.service.report;

import com.cloudmen.cloudguard.service.UserService;
import com.cloudmen.cloudguard.service.cache.CacheWarmupService;
import com.cloudmen.cloudguard.service.teamleader.TeamleaderAccessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BiannualReportScheduler {
    private static final Logger log = LoggerFactory.getLogger(PdfReportService.class);

    private final PdfReportService reportService;
    private final SecurityReportEmailService emailService;

    private final UserService userService;
    private final TeamleaderAccessService teamleaderAccessService;
    private final CacheWarmupService cacheWarmupService;

    public BiannualReportScheduler(PdfReportService reportService, SecurityReportEmailService emailService, UserService userService, TeamleaderAccessService teamleaderAccessService, CacheWarmupService cacheWarmupService) {
        this.reportService = reportService;
        this.emailService = emailService;
        this.userService = userService;
        this.teamleaderAccessService = teamleaderAccessService;
        this.cacheWarmupService = cacheWarmupService;
    }

    /**
     * CRON: "0 0 8 1 1,7 *" means:
     * At 08:00:00 AM, on the 1st day of the month, only in January and July.
     */
    @Scheduled(cron = "0 0 8 1 1,7 *")
    public void generateAndSendReports() {
        log.info("Start automatische generatie van halfjaarlijkse Security Rapporten...");
        List<String> emails = userService.getAllEmails();

        for (String email : emails) {
            if (!teamleaderAccessService.hasCloudGuardAccess(email)) continue;

            try {
                log.info("Bezig met rapport voor: {}", email);

                cacheWarmupService.warmupAllCachesAsync(email).join();

                PdfReportService.ReportResponse response = reportService.generateSecurityRapport(email);

                String companyName = response.companyName();

                emailService.sendSecurityReportEmail(email, companyName, response.data());

                log.info("Rapport succesvol verstuurd naar: {} ({})", email, companyName);
            } catch (Exception e) {
                log.error("Fout bij het genereren/versturen van rapport voor {}: {}", email, e.getMessage(), e);
            }
        }

        log.info("Klaar met het versturen van alle halfjaarlijkse rapporten.");
    }
}
