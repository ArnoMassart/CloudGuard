package com.cloudmen.cloudguard.dto.dashboard;

/**
 * A Data Transfer Object (DTO) containing a detailed breakdown of security scores across various organizational
 * categories. <p>
 *
 * This record provides individual metrics for users, groups, devices and configuration settings, which are typically
 * used to calculate the overall health and security posture of the Google Workspace.
 *
 * @param usersScore            The security score associated with user account configurations and 2FA compliance
 * @param groupsScore           vhe security score evaluating Google Workspace group settings and access controls
 * @param drivesScore           The security score reflecting the safety and external sharing policies of Google Drives
 * @param devicesScore          The security score based on the management and compliance of connected devices
 * @param appAccessScore        The security score evaluating internal and third-party applications access and
 *                              authorized OAuth scopes
 * @param appPasswordsScore     The security score indicating the status or risks associated with legacy app passwords
 * @param passwordSettingsScore The security score reflecting the strength and enforcement of organizational
 *                              password policies
 * @param dnsScore              The security score evaluating domain configurations such as SPF, DKIM, and DMARC
 */
public record DashboardScores(
        int usersScore,
        int groupsScore,
        int drivesScore,
        int devicesScore,
        int appAccessScore,
        int appPasswordsScore,
        int passwordSettingsScore,
        int dnsScore
) {
}
