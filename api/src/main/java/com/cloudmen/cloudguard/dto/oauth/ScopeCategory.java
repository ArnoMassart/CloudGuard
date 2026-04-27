package com.cloudmen.cloudguard.dto.oauth;

import lombok.Getter;

/**
 * Enum categorizing raw OAuth scopes into logical, human-readable groups. <p>
 *
 * This enum maps technical API permissions to easily understandable categories, simplifying the presentation of
 * application data access rights in the user interface so administrators can quickly grasp what services an
 * application can access.
 */
@Getter
public enum ScopeCategory {
    /**
     * Represents scopes related to basic user authentication and identity verification via Google Sign-In.
     */
    SIGN_IN("Google Sign-In"),

    /**
     * Represents scopes granting access to read, send delete, or manage the user's Gmail messages.
     */
    GMAIL("Gmail"),

    /**
     * Represents scopes granting access to configure or modify Gmail settings, aliases, and filters.
     */
    GMAIL_SETTINGS("Gmail Instellingen"),

    /**
     * Represents scopes granting access to view, edit, create, or manage files and folders in Google Drive.
     */
    DRIVE("Google Drive"),

    /**
     * Represents scopes granting access specifically to view or edit Google Spreadsheets.
     */
    SHEETS("Google Spreadsheets"),

    /**
     * Represents scopes granting access specifically to view or edit Google Documents.
     */
    DOCS("Google Documenten"),

    /**
     * Represents scopes granting access to view, create, or manage Google Forms and their responses.
     */
    FORMS("Google Formulieren"),

    /**
     * Represents scopes granting access to view, edit, or manage the user's Google Calendar events.
     */
    CALENDAR("Google Agenda"),

    /**
     * Represents scopes granting access to view or manage the user's Google Contacts and address book.
     */
    CONTACTS("Google Contacten"),

    /**
     * Represents scopes granting access to view or manage the organization's structure and units.
     */
    ORG_STRUCTURE("Organisatiestructuur"),

    /**
     * Represents administrative scopes granting access to the Google Workspace Admin Directory (users, groups, etc.).
     */
    ADMIN_DIRECTORY("Admin Directory"),

    /**
     * Represents scopes granting access to manage or view Google Cloud Platform (GCP) resources and services.
     */
    GCP("Google Cloud (GCP)"),

    /**
     * Represents administrative scopes granting access to view or modify Google Groups settings and memberships.
     */
    GROUP_SETTINGS("Groepsinstellingen"),

    /**
     * Represents administrative scopes granting access to view or manage Google Workspace license assignments.
     */
    LICENSING("Google Workspace Licenties"),

    /**
     * Represents scopes granting access to read or manage Google Chat messages, spaces, and memberships.
     */
    CHAT("Google Chat"),

    /**
     * Represents scopes granting access to manage the user's YouTube account, videos, and playlists.
     */
    YOUTUBE("YouTube"),

    /**
     * Represents scopes indicating connections to external or third-party APIs outside of the core Google ecosystem.
     */
    EXTERNAL_API("Externe API Verbindingen"),

    /**
     * Represents scopes allowing Google Apps Script projects to send emails on behalf of the user.
     */
    APPS_SCRIPT_MAIL("Apps Script Mail"),

    /**
     * Represents general scopes for executing, deploying or managing Google Apps Script projects.
     */
    APPS_SCRIPT("Google Apps Script"),

    /**
     * Represents administrative scopes granting access to transfer data and ownership between users in the domain.
     */
    DATA_TRANSFER("Data Overdracht (Admin)"),

    /**
     * Represents generalized or flexible Google APIs used for various customer or newer integrations.
     */
    FLEXIBLE_API("Google Flexible API"),

    /**
     * Represents generalized, low-risk scopes that only grant read-only access across various Google services.
     */
    READONLY("Algemeen - Alleen Lezen"),

    /**
     * Represents scopes that do not map to any recognized or predefined category within the system.
     */
    UNKNOWN("Onbekend");

    private final String displayName;

    /**
     * Constructs a new {@link ScopeCategory} with its corresponding human-readable display name.
     *
     * @param displayName the descriptive string value representing the scope category (e.g., "Gmail")
     */
    ScopeCategory(String displayName) {
        this.displayName = displayName;
    }

}