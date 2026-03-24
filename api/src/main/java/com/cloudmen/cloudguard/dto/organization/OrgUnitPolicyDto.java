package com.cloudmen.cloudguard.dto.organization;


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
) {
}
