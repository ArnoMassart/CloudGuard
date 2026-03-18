package com.cloudmen.cloudguard.controller;

import com.cloudmen.cloudguard.context.UserContext;
import com.cloudmen.cloudguard.service.JwtService;
import com.cloudmen.cloudguard.service.PdfReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/report")
public class ReportController {
    private final PdfReportService reportService;
    private final JwtService jwtService;
    private final UserContext userContext;

    public ReportController(PdfReportService reportService, JwtService jwtService, UserContext userContext) {
        this.reportService = reportService;
        this.jwtService = jwtService;
        this.userContext = userContext;
    }

    @GetMapping
    public ResponseEntity<byte[]> downloadReport(@CookieValue(name = "AuthToken", required = false) String token) throws Exception {
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String loggedInEmail = jwtService.validateInternalToken(token);

        userContext.setEmail(loggedInEmail);

        byte[] pdfBytes = reportService.generateSecurityRapport();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Security_Report.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
