package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.dto.report.FullSecurityReport;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.svgsupport.BatikSVGDrawer;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.Document;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
public class PdfReportService {
    private final TemplateEngine templateEngine;

    public PdfReportService(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public byte[] generateSecurityRapport(String clientName) throws Exception {
        byte[] imageBytes = new ClassPathResource("static/logo.png").getInputStream().readAllBytes();
        String base64Logo = "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);

        List<FullSecurityReport.SecurityCheck> checks = List.of(
                new FullSecurityReport.SecurityCheck("SPF Record", "SPF record ontbreekt of niet correct geconfigureerd", "Als geen andere servers mail voor dit domein verzenden, stel dan in: v=spf1 include:_spf.google.com ~all", "RED", "Actie vereist"),
                new FullSecurityReport.SecurityCheck("DKIM Signing", "DKIM is niet ingesteld", "Configureer DKIM in Google Admin voor uw domein", "RED", "Actie vereist"),
                new FullSecurityReport.SecurityCheck("MX Records", "MX records verwijzen correct naar Google", null, "GREEN", "Geldig"),
                new FullSecurityReport.SecurityCheck("Mail subdomein", "Optioneel - niet geconfigureerd", null, "GRAY", "Optioneel")
        );

        List<FullSecurityReport.DnsRecord> records = List.of(
                new FullSecurityReport.DnsRecord("SPF", "nl.cloudmen.net", insertZeroWidthSpaces("-", 40)),
                new FullSecurityReport.DnsRecord("MX", "nl.cloudmen.net", insertZeroWidthSpaces("1 smtp.google.com", 40)),
                new FullSecurityReport.DnsRecord("TXT", "nl.cloudmen.net", insertZeroWidthSpaces("v=DKIM1; k=rsa; p=MIIBIjANBgkqhkiG9w0BAQEFAAO.....", 40))
        );

        FullSecurityReport.DomainData domain1 = new FullSecurityReport.DomainData("nl.cloudmen.net", true, checks, records);

// Voeg de domeinen toe aan je hoofdrapport
        List<FullSecurityReport.DomainData> allDomains = List.of(domain1);

        FullSecurityReport reportData = new FullSecurityReport(
                65, // overallScore
                List.of(
                        new FullSecurityReport.RiskItem("DMARC niet afgedwongen", "Het domein is kwetsbaar voor spoofing."),
                        new FullSecurityReport.RiskItem("App-wachtwoorden actief", "Er zijn 4 actieve app-wachtwoorden die MFA omzeilen.")
                ),
                new FullSecurityReport.UsersMetrics(145, 95, 3, 12, 60),
                new FullSecurityReport.OrgUnitMetrics(8, 5, 1),
                new FullSecurityReport.DriveMetrics(24, 3, 0, 70),
                new FullSecurityReport.DeviceMetrics(40, 80, 10, 20, 48, 96, 45, 90, 40),
                new FullSecurityReport.AppAccessMetrics(112, 45, 2, 100),
                new FullSecurityReport.AppPasswordMetrics(4, 2, 30),
                allDomains
        );

        Context context = new Context();
        context.setVariable("logoBase64", base64Logo);
        context.setVariable("clientName", clientName);
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
    }

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
