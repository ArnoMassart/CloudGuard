package com.cloudmen.cloudguard.dto.report;

/**
 * A Data Transfer Object (DTO) representing the generated content of a security or audit report. <p>
 *
 * This record encapsulates the raw binary data of the report along with the associated company name, primarily used
 * for file export and download operations within the application.
 *
 * @param data          the raw byte array containing the generated report file content
 * @param companyName   the name of the organization or company for which this specific report was generated
 */
public record ReportResponse(
        byte[] data,
        String companyName) { }
