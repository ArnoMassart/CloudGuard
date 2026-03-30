package com.cloudmen.cloudguard.dto.report;

public record ReportResponse(
        byte[] data,
        String companyName) { }
