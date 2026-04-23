package com.cloudmen.cloudguard.controller;

import com.cloudmen.cloudguard.service.JwtService;
import com.cloudmen.cloudguard.service.report.PdfReportService;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller responsible for generating and serving security reports. <p>
 *
 * This controller provides endpoints to trigger the creation of localized PDF
 * security reports based on the authenticated user's organization data. <p>
 *
 * All routes are mapped under the {@code /report} prefix.
 */
@RestController
@RequestMapping("/report")
public class ReportController {
    private final PdfReportService reportService;
    private final JwtService jwtService;

    /**
     * Constructs a new {@link ReportController} with the required services.
     *
     * @param reportService the service responsible for compiling and generating
     * the PDF report data
     * @param jwtService    the service used to validate the session token and
     * extract user identity
     */
    public ReportController(PdfReportService reportService, JwtService jwtService) {
        this.reportService = reportService;
        this.jwtService = jwtService;
    }

    /**
     * Generates and downloads a comprehensive security report as a PDF. <p>
     *
     * This endpoint validates the user's session, identifies their locale for
     * translation purposes, and triggers the generation of a customized security
     * report. The response is formatted as a downloadable PDF attachment.
     *
     * @param token the {@code AuthToken} cookie provided by the client used to
     * authenticate the request
     * @return a {@link ResponseEntity} containing the generated PDF report as a
     * byte array, with appropriate headers for file download
     */
    @GetMapping
    public ResponseEntity<byte[]> downloadReport(@CookieValue(name = "AuthToken", required = false) String token) {
        String loggedInEmail = jwtService.validateInternalToken(token);

        byte[] pdfBytes = reportService.generateSecurityReport(loggedInEmail, LocaleContextHolder.getLocale()).data();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Security_Report.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
