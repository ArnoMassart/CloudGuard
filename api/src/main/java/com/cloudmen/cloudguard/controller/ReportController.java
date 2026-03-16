package com.cloudmen.cloudguard.controller;

import com.cloudmen.cloudguard.service.PdfReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/report")
public class ReportController {
    private final PdfReportService reportService;

    public ReportController(PdfReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public ResponseEntity<byte[]> downloadReport() throws Exception {
        byte[] pdfBytes = reportService.generateSecurityRapport("CLOUDMEN Labo");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Security_Report.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
