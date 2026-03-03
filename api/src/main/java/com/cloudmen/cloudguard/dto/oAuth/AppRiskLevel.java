package com.cloudmen.cloudguard.dto.oAuth;

import lombok.Getter;

@Getter
public enum AppRiskLevel {
    HIGH("Hoog risico"),
    MEDIUM("Gemiddeld risico"),
    LOW("Laag risico");

    private final String label;

    AppRiskLevel(String label) {
        this.label = label;
    }
}
