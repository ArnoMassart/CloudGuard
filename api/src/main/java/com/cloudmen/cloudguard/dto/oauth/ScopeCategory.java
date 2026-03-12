package com.cloudmen.cloudguard.dto.oauth;

import lombok.Getter;

@Getter
public enum ScopeCategory {
    SIGN_IN("Google Sign-In"),
    GMAIL("Gmail"),
    GMAIL_SETTINGS("Gmail Instellingen"),
    DRIVE("Google Drive"),
    SHEETS("Google Spreadsheets"),
    DOCS("Google Documenten"),
    FORMS("Google Formulieren"),
    CALENDAR("Google Agenda"),
    CONTACTS("Google Contacten"),
    ORG_STRUCTURE("Organisatiestructuur"),
    ADMIN_DIRECTORY("Admin Directory"),
    GCP("Google Cloud (GCP)"),
    GROUP_SETTINGS("Groepsinstellingen"),
    LICENSING("Google Workspace Licenties"),
    CHAT("Google Chat"),
    YOUTUBE("YouTube"),
    EXTERNAL_API("Externe API Verbindingen"),
    APPS_SCRIPT_MAIL("Apps Script Mail"),
    APPS_SCRIPT("Google Apps Script"),
    DATA_TRANSFER("Data Overdracht (Admin)"),
    FLEXIBLE_API("Google Flexible API"),
    READONLY("Algemeen - Alleen Lezen"),
    UNKNOWN("Onbekend");

    private final String displayName;

    ScopeCategory(String displayName) {
        this.displayName = displayName;
    }

}