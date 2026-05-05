package com.cloudmen.cloudguard.dto.report;

import java.util.List;

/**
 * A comprehensive Data Transfer Object (DTO) representing the full security posture of a Google Workspace
 * environment. <p>
 * <p>
 * This record aggregates security metrics, risk assessments, and configuration data from various modules (Users,
 * Groups, Drives, Devices, etc.) into a single report, providing a holistic view of the organization's security health.
 *
 * @param overallScore     the aggregated security score for the entire organization
 * @param criticalRisks    a list of high-priority security issues requiring immediate attention
 * @param users            detailed metrics regarding user accounts and authentication security
 * @param groups           metrics focusing on group memberships and external exposure
 * @param drives           statistics on Shared Drive usage and sharing permissions
 * @param devices          compliance and encryption status of managed mobile and desktop devices
 * @param appAccess        metrics regarding third-party application integrations and OAuth risks
 * @param appPasswords     data on the usage of application-specific passwords within the domain
 * @param passwordSettings overview of password policies and 2-Step Verification enforcement
 * @param domains          a detailed security breakdown for each individual domain in the organization
 */
public record FullSecurityReport(
        Integer overallScore,
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

    /**
     * Represents an individual high-risk security finding
     *
     * @param title       the name or summary of the risk
     * @param description a detailed explanation of the threat
     */
    public record RiskItem(
            String title,
            String description
    ) { }

    /**
     * Metrics related to user account security and activity.
     *
     * @param total         total number of users in the domain
     * @param mfaPct        percentage of users with MFA enabled
     * @param admins        total number of users with admin roles
     * @param inactive      number of users flagged as inactive
     * @param securityScore specific security score for user management
     * @param hasError      {@code true} if an error occurred during data retrieval
     */
    public record UsersMetrics(
            int total,
            int mfaPct,
            int admins,
            int inactive,
            Integer securityScore,
            boolean hasError
    ) { }

    /**
     * Metrics related to Google Groups security.
     *
     * @param total               total number of groups
     * @param withExternalMembers groups containing external users
     * @param highRisk            groups with risky configurations
     * @param securityScore       specific security score for groups
     * @param hasError            {@code true} if an error occurred during data retrieval
     */
    public record GroupsMetrics(
            int total,
            int withExternalMembers,
            int highRisk,
            Integer securityScore,
            boolean hasError
    ) { }

    /**
     * Metrics related to Shared Drive security.
     *
     * @param total            total number of Shared Drives
     * @param externallyShared drives accessible to external parties
     * @param noManagers       drives without an active manager
     * @param securityScore    specific security score for drives
     * @param hasError         {@code true} if an error occurred during data retrieval
     */
    public record DriveMetrics(
            int total,
            int externallyShared,
            int noManagers,
            Integer securityScore,
            boolean hasError
    ) { }

    /**
     * Metrics related to managed device security and compliance.
     *
     * @param safe          number of compliant devices
     * @param safePct       percentage of compliant devices
     * @param unsafe        number of non-compliant devices
     * @param unsafePct     percentage of non-compliant devices
     * @param encrypted     number of encrypted devices
     * @param encryptedPct  percentage of encrypted devices
     * @param updated       number of devices on the latest OS
     * @param updatedPct    percentage of up-to-date devices
     * @param securityScore specific security score for devices
     * @param hasError      {@code true} if an error occurred during data retrieval
     */
    public record DeviceMetrics(
            int safe, int safePct,
            int unsafe, int unsafePct,
            int encrypted, int encryptedPct,
            int updated, int updatedPct,
            Integer securityScore,
            boolean hasError
    ) { }

    /**
     * Metrics related to third-party app access and OAuth tokens.
     *
     * @param totalConnected    total number of authorized apps
     * @param trusted           number of apps marked as trusted
     * @param highRisk          number of apps flagged as high risk
     * @param securityScore     specific security score for app access
     * @param hasError          {@code true} if an error occurred during data retrieval
     */
    public record AppAccessMetrics(
            int totalConnected,
            int trusted,
            int highRisk,
            Integer securityScore,
            boolean hasError
    ) { }

    /**
     * Metrics for application-specific passwords.
     *
     * @param activePasswords   total number of active app passwords
     * @param usersUsingThem    number of users with at least one
     * @param securityScore     specific security score for app passwords
     * @param hasError          {@code true} if an error occurred during data retrieval
     */
    public record AppPasswordMetrics(int activePasswords,
                                     int usersUsingThem,
                                     Integer securityScore,
                                     boolean hasError
    ) { }

    /**
     * Metrics for password policies and 2SV enforcement
     *
     * @param totalOus                  total organizational units
     * @param enforced2FaOus            OUs where 2SV is enforced
     * @param unenforcedOusWithUsers    OUs with users but no 2SV enforcement
     * @param adminsWithoutKeys         admins not using security keys
     * @param securityScore             specific security score for settings
     * @param hasError                  {@code true} if an error occurred during data retrieval
     */
    public record PasswordMetrics(int totalOus,
                                  int enforced2FaOus,
                                  long unenforcedOusWithUsers,
                                  int adminsWithoutKeys,
                                  Integer securityScore,
                                  boolean hasError
    ) { }

    /**
     * Security data for a specific domain within the organization.
     *
     * @param name              the domain name (e.g., example.com)
     * @param score             the security score for this domain
     * @param hasCriticalIssues {@code true} if critical issues were found
     * @param checks            list of individual security checks (SPF, etc.)
     * @param records           relevant DNS records for this domain
     * @param hasError          {@code true} if an error occurred during data retrieval
     */
    public record DomainData(
            String name,
            int score,
            boolean hasCriticalIssues,
            List<SecurityCheck> checks,
            List<DnsRecord> records,
            boolean hasError
    ) { }

    /**
     * Details of a single security validation check.
     *
     * @param title         name of the check (e.g., "DMARC Policy")
     * @param description   explanation of the current state
     * @param tip           actionable advice to improve the state
     * @param status        the result status
     * @param badgeText     short text for UI status indicators
     */
    public record SecurityCheck(
            String title,
            String description,
            String tip,
            String status,
            String badgeText
    ) { }

    /**
     * Representation of a DNS record.
     *
     * @param type  record type (e.g., TXT, MX)
     * @param name  record name/host
     * @param value the content of the DNS record
     */
    public record DnsRecord(
            String type,
            String name,
            String value
    ) { }
}
