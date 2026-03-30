package com.cloudmen.cloudguard.dto.report;

import java.util.List;

public record FullSecurityReport(
        int overallScore,
        List<RiskItem> criticalRisks,
        UsersMetrics users,
        GroupsMetrics groups,
        DriveMetrics drives,
        DeviceMetrics devices,
        AppAccessMetrics appAccess,
        AppPasswordMetrics appPasswords,
        PasswordMetrics passwordSettings,
        List<DomainData> domains
) {
    public record RiskItem(String title, String description) {}

    public record UsersMetrics(int total, int mfaPct, int admins, int inactive, int securityScore, boolean hasError) {
    }

    public record GroupsMetrics(int total,
                                int withExternalMembers,
                                int highRisk,
                                int securityScore, boolean hasError) {

    }

    public record DriveMetrics(int total, int externallyShared, int noManagers, int securityScore, boolean hasError) {

    }

    public record DeviceMetrics(
            int safe, int safePct,
            int unsafe, int unsafePct,
            int encrypted, int encryptedPct,
            int updated, int updatedPct, int securityScore, boolean hasError) {

    }

    public record AppAccessMetrics(int totalConnected, int trusted, int highRisk, int securityScore, boolean hasError) {

    }

    public record AppPasswordMetrics(int activePasswords, int usersUsingThem, int securityScore, boolean hasError) {}

    public record PasswordMetrics(int totalOus, int enforced2FaOus,long unenforcedOusWithUsers, int adminsWithoutKeys, int securityScore, boolean hasError){}

    public record DomainData(
            String name,
            int score,
            boolean hasCriticalIssues,
            List<SecurityCheck> checks,
            List<DnsRecord> records, boolean hasError
    ) {}

    public record SecurityCheck(
            String title,
            String description,
            String tip,
            String status,
            String badgeText
    ) {}

    public record DnsRecord(
            String type,
            String name,
            String value
    ) {}
}
