package com.cloudmen.cloudguard.dto.organization;

/**
 * Single policy card in the organization-units UI: localized title, status pill, inheritance hints, and optional Admin
 * Console deep link. Produced by {@link com.cloudmen.cloudguard.service.policy.OrgUnitPolicyAggregator} from
 * {@link com.cloudmen.cloudguard.service.policy.OrgUnitPolicyProvider} implementations.
 *
 * @param key                     stable provider id (e.g. chrome extensions policy key)
 * @param title                   short heading for the card
 * @param description             human-readable policy summary
 * @param status                  status line text (severity wording)
 * @param statusClass             CSS utility classes for the badge
 * @param baseExplanation         longer context shown on the card
 * @param inheritanceExplanation  how the setting relates to parent OUs / inheritance
 * @param inherited               whether the effective value is inherited rather than set on this OU
 * @param adminLink               optional URL or relative Admin Console path fragment for deep links
 * @param details                 optional compact stats line (e.g. extension counts) for the client
 */
public record OrgUnitPolicyDto(
         String key,
         String title,
         String description,
         String status,
         String statusClass,
         String baseExplanation,
         String inheritanceExplanation,
         boolean inherited,
         String adminLink,
         String details
) {}