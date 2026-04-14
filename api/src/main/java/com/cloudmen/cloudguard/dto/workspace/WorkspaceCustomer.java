package com.cloudmen.cloudguard.dto.workspace;

/**
 * Google Workspace tenant identity from Directory API {@code customers.get(my_customer)}.
 *
 * @param id Workspace customer id (e.g. {@code Cxxxxx})
 * @param displayName Human-facing label: {@code postalAddress.organizationName} when set, else primary
 *                    {@code customerDomain}, else {@code Workspace {id}}
 */
public record WorkspaceCustomer(String id, String displayName) {}
