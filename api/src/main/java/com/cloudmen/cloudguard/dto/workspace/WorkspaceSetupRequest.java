package com.cloudmen.cloudguard.dto.workspace;

/**
 * A Data Transfer Object (DTO) used as a request payload to initiate the setup or configuration of a Google
 * Workspace integration. <p>
 *
 * This record encapsulates the minimal initial data required to begin the onboarding, authorization, or
 * synchronization process for a new environment.
 *
 * @param adminEmail the primary email address of the administrator initiating the setup process, or the target
 *                   Workspace administrator's email
 */
public record WorkspaceSetupRequest(String adminEmail) {
}
