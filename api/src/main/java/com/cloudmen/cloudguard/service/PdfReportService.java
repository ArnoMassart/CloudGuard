package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.context.UserContext;
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
import com.cloudmen.cloudguard.service.dns.DnsRecordsService;
import com.cloudmen.cloudguard.service.notification.NotificationAggregationService;
import com.cloudmen.cloudguard.service.teamleader.TeamleaderCompanyService;
import com.cloudmen.cloudguard.service.teamleader.TeamleaderService;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.svgsupport.BatikSVGDrawer;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final UserContext userContext;
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

    public PdfReportService(TemplateEngine templateEngine, DashboardService dashboardService, NotificationAggregationService notificationAggregationService, GoogleUsersService googleUsersService, UserContext userContext, GoogleGroupsService googleGroupsService, GoogleSharedDriveService googleSharedDriveService, GoogleDeviceService googleDeviceService, GoogleOAuthService googleOAuthService, AppPasswordsService appPasswordsService, DnsRecordsService dnsRecordsService, GoogleDomainService googleDomainService, PasswordSettingsService passwordSettingsService, TeamleaderCompanyService teamleaderCompanyService, TeamleaderService teamleaderService) {
        this.templateEngine = templateEngine;
        this.dashboardService = dashboardService;
        this.notificationAggregationService = notificationAggregationService;
        this.googleUsersService = googleUsersService;
        this.userContext = userContext;
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
    }

    public byte[] generateSecurityRapport() {
        log.info("Starting pdf generation");
        try {
            byte[] imageBytes = new ClassPathResource("static/logo.png").getInputStream().readAllBytes();
            String base64Logo = "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);

            byte[] coverBytes = new ClassPathResource("static/cover-background-2.png").getInputStream().readAllBytes();
            String base64Cover = "data:image/png;base64," + Base64.getEncoder().encodeToString(coverBytes);

            HttpHeaders headers = teamleaderService.createHeaders();

            String companyName = teamleaderCompanyService.getCompanyNameByEmail(userContext.getEmail(), headers);

            FullSecurityReport reportData = getFullSecurityReport();

            Context context = new Context();
            context.setVariable("logoBase64", base64Logo);
            context.setVariable("bgImage", base64Cover);
            context.setVariable("clientName", companyName);
            context.setVariable("reportDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            context.setVariable("report", reportData);

            // 2. Spring Boot vindt je template nu wél succesvol
            String renderedHtml = templateEngine.process("security-template", context);

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

                return baos.toByteArray();
            }
        } catch (Exception e) {
            throw new RuntimeException("Error with pdf generation", e);
        }
    }

    private FullSecurityReport getFullSecurityReport() {
        int overallScore = dashboardService.getDashboardSecurityScores(userContext.getEmail()).overallScore();

        return new FullSecurityReport(
                overallScore,
                getSecurityReportRiskItems(),
                getSecurityReportUsersMetrics(),
                getSecurityReportGroupsMetrics(),
                getSecurityReportDriveMetrics(),
                getSecurityReportDeviceMetrics(),
                getSecurityReportAppAccessMetrics(),
                getSecurityReportAppPasswordMetrics(),
                getSecurityReportPasswordMetrics(),
                getSecurityReportDomainData()
        );
    }

    private List<FullSecurityReport.RiskItem> getSecurityReportRiskItems() {
        List<NotificationDto> criticalNotifications = notificationAggregationService.getCriticalNotifications(userContext.getEmail());

        return criticalNotifications.stream().map(n -> new FullSecurityReport.RiskItem(n.title(), n.description())).toList();
    }

    private FullSecurityReport.UsersMetrics getSecurityReportUsersMetrics() {
        UserOverviewResponse response = googleUsersService.getUsersPageOverview(userContext.getEmail());

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

    private FullSecurityReport.GroupsMetrics getSecurityReportGroupsMetrics() {
        GroupOverviewResponse response = googleGroupsService.getGroupsOverview(userContext.getEmail());

        return new FullSecurityReport.GroupsMetrics(
                (int) response.totalGroups(),
                (int)response.groupsWithExternal(),
                (int) response.highRiskGroups(),
                response.securityScore()
        );
    }

    private FullSecurityReport.DriveMetrics getSecurityReportDriveMetrics() {
        SharedDriveOverviewResponse response = googleSharedDriveService.getDrivesPageOverview(userContext.getEmail());

        return new FullSecurityReport.DriveMetrics(
                response.totalDrives(),
                response.notOnlyDomainUsersAllowedCount(),
                response.orphanDrives(),
                response.securityScore()
        );
    }

    private FullSecurityReport.DeviceMetrics getSecurityReportDeviceMetrics() {
        DeviceOverviewResponse response = googleDeviceService.getDevicesPageOverview(userContext.getEmail());

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

    private FullSecurityReport.AppAccessMetrics getSecurityReportAppAccessMetrics() {
        OAuthOverviewResponse response = googleOAuthService.getOAuthPageOverview(userContext.getEmail());

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

    private FullSecurityReport.AppPasswordMetrics getSecurityReportAppPasswordMetrics() {
        AppPasswordOverviewResponse response = appPasswordsService.getOverview(userContext.getEmail(), true);

        return new FullSecurityReport.AppPasswordMetrics(
                response.totalAppPasswords(),
                response.usersWithAppPasswords(),
                response.securityScore()
        );
    }

    private FullSecurityReport.PasswordMetrics getSecurityReportPasswordMetrics() {
        PasswordSettingsDto response = passwordSettingsService.getPasswordSettings(userContext.getEmail());

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

    private List<FullSecurityReport.DomainData> getSecurityReportDomainData() {
        List<FullSecurityReport.DomainData> domainData = new ArrayList<>();
        List<DomainDto> domains = googleDomainService.getAllDomains(userContext.getEmail());

        for (DomainDto d : domains) {
            String domain = d.domainName();

            DnsRecordResponseDto dnsResponse = dnsRecordsService.getImportantRecords(domain,"google");
            List<DnsRecordDto> rows = dnsResponse.rows();
            int score = dnsResponse.securityScore();

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
                        description = "Optioneel – niet geconfigureerd";
                        badgeText = "Optioneel";
                        statusText = "GRAY";
                    }
                    case ERROR -> {
                        description = "Fout bij ophalen DNS gegevens";
                        badgeText = "Error";
                        statusText = "RED";
                    }
                    case VALID -> {
                        description = info.validDesc;
                        badgeText = "Geldig";
                    }
                    case ATTENTION -> {
                        description = info.attentionDesc;
                        tip = info.tip;
                        badgeText = "Aandacht";
                        statusText = "ORANGE";
                    }
                    case ACTION_REQUIRED -> {
                        description = info.actionDesc;
                        tip = info.tip;
                        badgeText = "Actie vereist";
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
            entry("SPF", new DomainTip(
                    "SPF Record",
                    "SPF record is correct geconfigureerd",
                    "SPF record ontbreekt of niet correct geconfigureerd",
                    "SPF record bevat geen include:_spf.google.com",
                    "Als geen andere servers mail voor dit domein verzenden, stel dan in: v=spf1 include:_spf.google.com ~all"
            )),
            entry("DKIM", new DomainTip(
                    "DKIM Signing",
                    "DKIM is actief voor uitgaande mail",
                    "DKIM is niet ingesteld",
                    "DKIM record is niet correct geconfigureerd",
                    "Configureer DKIM in Google Admin voor uw domein"
            )),
            entry("DMARC", new DomainTip(
                    "DMARC Policy",
                    "DMARC policy is correct ingesteld",
                    "DMARC is niet ingesteld",
                    "DMARC policy kan worden aangescherpt naar reject",
                    "Overweeg policy te verhogen naar 'reject' voor maximale beveiliging"
            )),
            entry("MX", new DomainTip(
                    "MX Records",
                    "MX records verwijzen correct naar Google",
                    "Domein heeft geen mailserver of MX records ontbreken",
                    "Geen Google mail exchangers gevonden – controleer relayhost configuratie",
                    "Wijzig MX records naar Google mail servers (bijv. ASPMX.L.GOOGLE.COM)"
            )),
            entry("DNSSEC", new DomainTip(
                    "DNSSEC",
                    "DNSSEC is ingeschakeld",
                    "DNSSEC is niet ingeschakeld",
                    "DNSSEC configuratie vereist aandacht",
                    "Schakel DNSSEC in bij je domeinregistrar voor extra beveiliging"
            )),
            entry("CAA", new DomainTip(
                    "CAA Records",
                    "CAA records zijn ingesteld",
                    "CAA records niet ingesteld",
                    "CAA records niet ingesteld",
                    "Voeg CAA records toe om te bepalen welke certificaatautoriteiten certificaten voor uw domein mogen uitgeven"
            )),
            entry("TXT", new DomainTip(
                    "Site verificatie",
                    "Google site verificatie is aanwezig",
                    "Site verificatie ontbreekt",
                    "Site verificatie vereist aandacht",
                    "Voeg google-site-verification TXT record toe voor verificatie"
            )),
            entry("CNAME", new DomainTip(
                    "Mail subdomein",
                    "CNAME record is geconfigureerd",
                    "CNAME record ontbreekt",
                    "CNAME configuratie vereist aandacht",
                    "Configureer CNAME voor mail subdomein indien gewenst"
            ))
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
