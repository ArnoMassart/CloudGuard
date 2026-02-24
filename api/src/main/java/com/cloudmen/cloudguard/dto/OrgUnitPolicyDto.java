package com.cloudmen.cloudguard.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class OrgUnitPolicyDto {
    private String key;
    private String title;
    private String description;
    private String status;
    private String statusClass;
    private String baseExplanation;
    private String inheritanceExplanation;
    private boolean inherited;
    private String settingsLinkText;
    private String adminLink;
}
