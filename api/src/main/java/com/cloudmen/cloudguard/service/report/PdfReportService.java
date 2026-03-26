package com.cloudmen.cloudguard.service.report;

import com.cloudmen.cloudguard.domain.model.DnsRecordStatus;
import com.cloudmen.cloudguard.dto.devices.DeviceOverviewResponse;
import com.cloudmen.cloudguard.dto.dns.DnsRecordDto;
import com.cloudmen.cloudguard.dto.dns.DnsRecordResponseDto;
import com.cloudmen.cloudguard.dto.domain.DomainDto;
import com.cloudmen.cloudguard.dto.drives.SharedDriveOverviewResponse;
import com.cloudmen.cloudguard.dto.groups.GroupOverviewResponse;
import com.cloudmen.cloudguard.dto.notifications.NotificationDto;
import com.cloudmen.cloudguard.dto.oauth.OAuthOverviewResponse;
import com.cloudmen.cloudguard.dto.apppasswords.AppPasswordOverviewResponse;
import com.cloudmen.cloudguard.dto.password.OrgUnit2SvDto;
import com.cloudmen.cloudguard.dto.password.PasswordSettingsDto;
import com.cloudmen.cloudguard.dto.report.FullSecurityReport;
import com.cloudmen.cloudguard.dto.users.UserOverviewResponse;
import com.cloudmen.cloudguard.service.*;
import com.cloudmen.cloudguard.service.dns.DnsRecordsService;
import com.cloudmen.cloudguard.service.notification.NotificationAggregationService;
import com.cloudmen.cloudguard.service.preference.UserSecurityPreferenceService;
import com.cloudmen.cloudguard.service.teamleader.TeamleaderCompanyService;
import com.cloudmen.cloudguard.service.teamleader.TeamleaderService;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.svgsupport.BatikSVGDrawer;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static java.util.Map.entry;

@Service
public class PdfReportService {
    private static final Logger log = LoggerFactory.getLogger(PdfReportService.class);

    private final TemplateEngine templateEngine;
    private final DashboardService dashboardService;
    private final NotificationAggregationService notificationAggregationService;
    private final GoogleUsersService googleUsersService;
    private final GoogleGroupsService googleGroupsService;
    private final GoogleSharedDriveService googleSharedDriveService;
    private final GoogleDeviceService googleDeviceService;
    private final GoogleOAuthService googleOAuthService;
    private final AppPasswordsService appPasswordsService;
    private final DnsRecordsService dnsRecordsService;
    private final GoogleDomainService googleDomainService;
    private final PasswordSettingsService passwordSettingsService;
    private final TeamleaderCompanyService teamleaderCompanyService;
    private final TeamleaderService teamleaderService;
    private final MessageSource messageSource;

    public PdfReportService(TemplateEngine templateEngine, DashboardService dashboardService, NotificationAggregationService notificationAggregationService, GoogleUsersService googleUsersService, GoogleGroupsService googleGroupsService, GoogleSharedDriveService googleSharedDriveService, GoogleDeviceService googleDeviceService, GoogleOAuthService googleOAuthService, AppPasswordsService appPasswordsService, DnsRecordsService dnsRecordsService, GoogleDomainService googleDomainService, PasswordSettingsService passwordSettingsService, TeamleaderCompanyService teamleaderCompanyService, TeamleaderService teamleaderService, @Qualifier("messageSource") MessageSource messageSource) {
        this.templateEngine = templateEngine;
        this.dashboardService = dashboardService;
        this.notificationAggregationService = notificationAggregationService;
        this.googleUsersService = googleUsersService;
        this.googleGroupsService = googleGroupsService;
        this.googleSharedDriveService = googleSharedDriveService;
        this.googleDeviceService = googleDeviceService;
        this.googleOAuthService = googleOAuthService;
        this.appPasswordsService = appPasswordsService;
        this.dnsRecordsService = dnsRecordsService;
        this.googleDomainService = googleDomainService;
        this.passwordSettingsService = passwordSettingsService;
        this.teamleaderCompanyService = teamleaderCompanyService;
        this.teamleaderService = teamleaderService;
        this.messageSource = messageSource;
    }

    public record ReportResponse(
            byte[] data,
            String companyName) { }

    public ReportResponse generateSecurityRapport(String adminEmail) {
        Locale locale = LocaleContextHolder.getLocale();
        log.info("Starting pdf generation");

        try {
            byte[] imageBytes = new ClassPathResource("static/logo.png").getInputStream().readAllBytes();
            String base64Logo = "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);

            byte[] coverBytes = new ClassPathResource("static/cover-background-2.png").getInputStream().readAllBytes();
            String base64Cover = "data:image/png;base64," + Base64.getEncoder().encodeToString(coverBytes);

            HttpHeaders headers = teamleaderService.createHeaders();

            String companyName = teamleaderCompanyService.getCompanyNameByEmail(adminEmail, headers);

            FullSecurityReport reportData = getFullSecurityReport(adminEmail);

            Context context = new Context(locale);

            context.setVariable("logoBase64", base64Logo);
            context.setVariable("bgImage", base64Cover);
            context.setVariable("clientName", companyName);
            context.setVariable("reportDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            context.setVariable("report", reportData);

            // 2. Spring Boot vindt je template nu wél succesvol
            String renderedHtml = templateEngine.process("security-template_"+locale.getLanguage(), context);

            // 3. Gebruik JSoup om de HTML5 op te schonen en om te zetten naar een strict W3C DOM Document
            Document jsoupDoc = Jsoup.parse(renderedHtml, "UTF-8");
            org.w3c.dom.Document w3cDoc = new W3CDom().fromJsoup(jsoupDoc);

            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                PdfRendererBuilder builder = new PdfRendererBuilder();
                builder.useFastMode();

                builder.useSVGDrawer(new BatikSVGDrawer());

                // 4. Geef het veilige W3C Document door aan OpenHTMLtoPDF (voorkomt de SAX parser crash)
                builder.withW3cDocument(w3cDoc, "/");
                builder.toStream(baos);
                builder.run();

                return new ReportResponse(baos.toByteArray(), companyName);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error with pdf generation: " + e.getMessage(), e);
        }
    }

    private FullSecurityReport getFullSecurityReport(String adminEmail) {
        int overallScore = dashboardService.getDashboardSecurityScores(adminEmail).overallScore();
        Set<String> disabled = userSecurityPreferenceService.getDisabledPreferenceKeys(adminEmail);

        return new FullSecurityReport(
                overallScore,
                getSecurityReportRiskItems(adminEmail),
                getSecurityReportUsersMetrics(adminEmail, disabled),
                getSecurityReportGroupsMetrics(adminEmail, disabled),
                getSecurityReportDriveMetrics(adminEmail, disabled),
                getSecurityReportDeviceMetrics(adminEmail, disabled),
                getSecurityReportAppAccessMetrics(adminEmail, disabled),
                getSecurityReportAppPasswordMetrics(adminEmail, disabled),
                getSecurityReportPasswordMetrics(adminEmail),
                getSecurityReportDomainData(adminEmail)
        );
    }

    private List<FullSecurityReport.RiskItem> getSecurityReportRiskItems(String adminEmail) {
        List<NotificationDto> criticalNotifications = notificationAggregationService.getCriticalNotifications(adminEmail);

        return criticalNotifications.stream().map(n -> new FullSecurityReport.RiskItem(n.title(), n.description())).toList();
    }

    private FullSecurityReport.UsersMetrics getSecurityReportUsersMetrics(String adminEmail, Set<String> disabled) {
        UserOverviewResponse response = googleUsersService.getUsersPageOverview(adminEmail, disabled);

        int total = response.totalUsers();

        int mfaPct = response.totalUsers() > 0 ? (int) (100 - Math.floor((double) response.withoutTwoFactor() / response.totalUsers() * 100)) : 100;

        return new FullSecurityReport.UsersMetrics(
                total,
                mfaPct,
                response.adminUsers(),
                response.activeLongNoLoginCount(),
                response.securityScore()
        );
    }

    private FullSecurityReport.GroupsMetrics getSecurityReportGroupsMetrics(String adminEmail, Set<String> disabled) {
        GroupOverviewResponse response = googleGroupsService.getGroupsOverview(adminEmail, disabled);

        return new FullSecurityReport.GroupsMetrics(
                (int) response.totalGroups(),
                (int)response.groupsWithExternal(),
                (int) response.highRiskGroups(),
                response.securityScore()
        );
    }

    private FullSecurityReport.DriveMetrics getSecurityReportDriveMetrics(String adminEmail, Set<String> disabled) {
        SharedDriveOverviewResponse response = googleSharedDriveService.getDrivesPageOverview(adminEmail, disabled);

        return new FullSecurityReport.DriveMetrics(
                response.totalDrives(),
                response.notOnlyDomainUsersAllowedCount(),
                response.orphanDrives(),
                response.securityScore()
        );
    }

    private FullSecurityReport.DeviceMetrics getSecurityReportDeviceMetrics(String adminEmail, Set<String> disabled) {
        DeviceOverviewResponse response = googleDeviceService.getDevicesPageOverview(adminEmail, disabled);

        int total = response.totalDevices();

        int unsafe = response.totalNonCompliant();
        int safe = total - unsafe;

        int encrypted = total - response.encryptionCount();
        int updated = total - response.osVersionCount();

        int safePct = total == 0 ? 0 : (int) Math.round((double) safe / total * 100);
        int unsafePct = total == 0 ? 0 : (int) Math.round((double) unsafe / total * 100);
        int encryptedPct = total == 0 ? 0 : (int) Math.round((double) encrypted / total * 100);
        int updatedPct = total == 0 ? 0 : (int) Math.round((double) updated / total * 100);

        return new FullSecurityReport.DeviceMetrics(
                safe, safePct, unsafe, unsafePct, encrypted, encryptedPct, updated, updatedPct, response.securityScore()
        );
    }

    private FullSecurityReport.AppAccessMetrics getSecurityReportAppAccessMetrics(String adminEmail, Set<String> disabled) {
        OAuthOverviewResponse response = googleOAuthService.getOAuthPageOverview(adminEmail, disabled);

        int totalConnected = (int) response.totalThirdPartyApps();
        int highRisk = (int) response.totalHighRiskApps();

        int trusted = totalConnected - highRisk;

        return new FullSecurityReport.AppAccessMetrics(
                totalConnected,
                trusted,
                highRisk,
                response.securityScore()
        );
    }

    private FullSecurityReport.AppPasswordMetrics getSecurityReportAppPasswordMetrics(String adminEmail, Set<String> disabled) {
        AppPasswordOverviewResponse response = appPasswordsService.getOverview(adminEmail, true, disabled);

        return new FullSecurityReport.AppPasswordMetrics(
                response.totalAppPasswords(),
                response.usersWithAppPasswords(),
                response.securityScore()
        );
    }

    private FullSecurityReport.PasswordMetrics getSecurityReportPasswordMetrics(String adminEmail) {
        PasswordSettingsDto response = passwordSettingsService.getPasswordSettings(adminEmail);

        int enforcedOus = (int) response.twoStepVerification().byOrgUnit().stream().filter(OrgUnit2SvDto::enforced).count();

        long unenforcedWithUsers = response.twoStepVerification().byOrgUnit().stream().filter(ou-> !ou.enforced() && ou.totalCount() > 0).count();

        return new FullSecurityReport.PasswordMetrics(
                response.passwordPoliciesByOu().size(),
                enforcedOus,
                unenforcedWithUsers,
                response.adminsWithoutSecurityKeys().size(),
                response.securityScore()
        );
    }

    private List<FullSecurityReport.DomainData> getSecurityReportDomainData(String adminEmail) {
        List<FullSecurityReport.DomainData> domainData = new ArrayList<>();
        List<DomainDto> domains = googleDomainService.getAllDomains(adminEmail);

        for (DomainDto d : domains) {
            String domain = d.domainName();

            var dnsOverrides = userSecurityPreferenceService.getDnsImportanceOverrides(adminEmail);
            DnsRecordResponseDto dnsResponse = dnsRecordsService.getImportantRecords(domain, "google", dnsOverrides);
            List<DnsRecordDto> rows = dnsResponse.rows();
            int score = dnsResponse.securityScore();

            Locale locale = LocaleContextHolder.getLocale();

            List<FullSecurityReport.SecurityCheck> checks = rows.stream().map(r -> {
                String type = r.type();

                DomainTip info = TIPS.get(type);

                DnsRecordStatus status = r.status();

                String description = "";
                String tip = "";
                String badgeText = "";
                String statusText = "GREEN";

                switch (status) {
                    case OK -> {
                        description = messageSource.getMessage("report.domain.status.ok.description_1", null, locale) +" - " + messageSource.getMessage("report.domain.status.ok.description_2", null, locale);
                        badgeText = messageSource.getMessage("report.domain.status.ok.badgeText", null, locale);
                        statusText = "GRAY";
                    }
                    case ERROR -> {
                        description = messageSource.getMessage("report.domain.status.error.description", null, locale);
                        badgeText = messageSource.getMessage("report.domain.status.error.badgeText", null, locale);
                        statusText = "RED";
                    }
                    case VALID -> {
                        description = messageSource.getMessage(info.validDesc, null, locale);
                        badgeText = messageSource.getMessage("report.domain.status.valid.badgeText", null, locale);
                    }
                    case ATTENTION -> {
                        description = messageSource.getMessage(info.attentionDesc, null, locale);
                        tip = messageSource.getMessage(info.tip, null, locale);
                        badgeText = messageSource.getMessage("report.domain.status.attention.badgeText", null, locale);
                        statusText = "ORANGE";
                    }
                    case ACTION_REQUIRED -> {
                        description = messageSource.getMessage(info.actionDesc, null, locale);
                        tip = messageSource.getMessage(info.tip, null, locale);
                        badgeText = messageSource.getMessage("report.domain.status.action_required.badgeText", null, locale);
                        statusText = "RED";
                    }
                }

                return new FullSecurityReport.SecurityCheck(
                        info.title(),
                        description,
                        tip,
                        statusText,
                        badgeText

                );
            }).sorted(Comparator.comparingInt(check -> switch (check.status()) {
                        case "RED" -> 1;
                        case "ORANGE" -> 2;
                        case "GREEN" -> 3;
                        case "GRAY" -> 4;
                        default -> 5;
            })).toList();

            List<FullSecurityReport.DnsRecord> records = rows.stream().map(r -> new FullSecurityReport.DnsRecord(
                    r.type(),
                    r.name(),
                    r.values().isEmpty() ? "" : insertZeroWidthSpaces(r.values().get(0), 40)
            )).toList();

            boolean hasCritical = rows.stream().anyMatch(r -> r.status().equals(DnsRecordStatus.ACTION_REQUIRED));

            domainData.add(new FullSecurityReport.DomainData(domain, score, hasCritical, checks, records));

        }

        return domainData;
    }

    public record DomainTip(
            String title,
            String validDesc,
            String actionDesc,
            String attentionDesc,
            String tip
    ) {}

    public static final Map<String, DomainTip> TIPS = Map.ofEntries(
            entry("SPF", new DomainTip("report.domain.tip.spf.title", "report.domain.tip.spf.valid", "report.domain.tip.spf.action", "report.domain.tip.spf.attention", "report.domain.tip.spf.tip")),
            entry("DKIM", new DomainTip("report.domain.tip.dkim.title", "report.domain.tip.dkim.valid", "report.domain.tip.dkim.action", "report.domain.tip.dkim.attention", "report.domain.tip.dkim.tip")),
            entry("DMARC", new DomainTip("report.domain.tip.dmarc.title", "report.domain.tip.dmarc.valid", "report.domain.tip.dmarc.action", "report.domain.tip.dmarc.attention", "report.domain.tip.dmarc.tip")),
            entry("MX", new DomainTip("report.domain.tip.mx.title", "report.domain.tip.mx.valid", "report.domain.tip.mx.action", "report.domain.tip.mx.attention", "report.domain.tip.mx.tip")),
            entry("DNSSEC", new DomainTip("report.domain.tip.dnssec.title", "report.domain.tip.dnssec.valid", "report.domain.tip.dnssec.action", "report.domain.tip.dnssec.attention", "report.domain.tip.dnssec.tip")),
            entry("CAA", new DomainTip("report.domain.tip.caa.title", "report.domain.tip.caa.valid", "report.domain.tip.caa.action", "report.domain.tip.caa.attention", "report.domain.tip.caa.tip")),
            entry("TXT", new DomainTip("report.domain.tip.txt.title", "report.domain.tip.txt.valid", "report.domain.tip.txt.action", "report.domain.tip.txt.attention", "report.domain.tip.txt.tip")),
            entry("CNAME", new DomainTip("report.domain.tip.cname.title", "report.domain.tip.cname.valid", "report.domain.tip.cname.action", "report.domain.tip.cname.attention", "report.domain.tip.cname.tip"))
    );

    // Helpt PDF-engines om extreem lange strings zonder spaties af te breken
    private String insertZeroWidthSpaces(String text, int interval) {
        if (text == null || text.length() <= interval || text.contains(" ")) {
            return text; // Als de tekst al spaties heeft of kort is, doe niets
        }

        StringBuilder sb = new StringBuilder(text);
        // Voeg elke 'interval' karakters een onzichtbaar breekpunt toe (&#8203;)
        for (int i = interval; i < sb.length(); i += interval + 1) {
            sb.insert(i, "\u200B");
        }
        return sb.toString();
    }
}
