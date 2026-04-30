package com.cloudmen.cloudguard.dto.report;

/**
 * A Data Transfer Object (DTO) representing a security tip or recommendation related to domain configuration <p>
 *
 * This record provides structured content used to display domain-specific security advice (e.g., SPF, DKIM, or DMARC
 * settings). It includes descriptive text for various validation states and actionable guidance to help organizations
 * improve their domain's security posture.
 *
 * @param title         the short, descriptive name of the security check or domain setting
 * @param validDesc     the description to display when the security check passes successfully
 * @param actionDesc    the instructions or description to display when administrative action is required
 * @param attentionDesc the description to display when a potential issue is detected that requires the administrator's
 *                      attention.
 * @param tip           a supplementary piece of advice or context explaining the importance of this setting
 */
public record DomainTip(
        String title,
        String validDesc,
        String actionDesc,
        String attentionDesc,
        String tip
) {}
