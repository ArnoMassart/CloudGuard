package com.cloudmen.cloudguard.dto.report;

import java.util.List;

public record FullSecurityReport(
        int overallScore,
        List<RiskItem> criticalRisks,
        UsersMetrics users,
        OrgUnitMetrics orgUnits,
        DriveMetrics drives,
        DeviceMetrics devices,
        AppAccessMetrics appAccess,
        AppPasswordMetrics appPasswords,
        List<DomainData> domains
) {
    public record RiskItem(String title, String description) {}

    public record UsersMetrics(int total, int mfaPct, int admins, int inactive, int securityScore) {}

    public record OrgUnitMetrics(int total, int withCustomPolicies, int unprotected) {}

    public record DriveMetrics(int total, int externallyShared, int noManagers, int securityScore) {}

    public record DeviceMetrics(
            int safe, int safePct,
            int unsafe, int unsafePct,
            int encrypted, int encryptedPct,
            int updated, int updatedPct, int securityScore) {}

    public record AppAccessMetrics(int totalConnected, int trusted, int highRisk, int securityScore) {}

    public record AppPasswordMetrics(int activePasswords, int usersUsingThem, int securityScore) {}

    public record DomainData(
            String name,
            boolean hasCriticalIssues,
            List<SecurityCheck> checks,
            List<DnsRecord> records
    ) {}

    public record SecurityCheck(
            String title,
            String description,
            String tip,
            String status,    // Gebruik: "RED", "ORANGE", "GREEN", "GRAY"
            String badgeText  // Gebruik: "Actie vereist", "Aandacht", "Geldig", "Optioneel"
    ) {}

    public record DnsRecord(
            String type,
            String name,
            String value
    ) {}
}
