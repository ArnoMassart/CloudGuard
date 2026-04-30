package com.cloudmen.cloudguard.dto.drives;

/**
 * A Data Transfer Object (DTO) encapsulating key membership metrics for a Shared Drive. <p>
 *
 * This record provides a concise summary of user access statistics, including the overall size of the
 * drive's membership, its exposure to external users, and the number of members holding administrative control.
 *
 * @param totalMembers      the total number of members (users or groups)
 * @param externalMembers   the number of members who reside outside the organization's domain
 * @param totalOrganizers   the number of members who possess organizer or administrative privileges on the drive
 */
public record DriveMetrics(
        int totalMembers,
        int externalMembers,
        int totalOrganizers
) {}

