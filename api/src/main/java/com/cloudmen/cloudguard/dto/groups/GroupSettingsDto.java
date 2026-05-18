package com.cloudmen.cloudguard.dto.groups;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Projection of Google Group Settings used when building {@link GroupOrgDetail}: join rule, who can view membership,
 * and whether external users may be members.
 */
@AllArgsConstructor
@Getter
public class GroupSettingsDto {
    /** Mapped message key or em-dash placeholder when settings could not be loaded. */
    private String whoCanJoin;
    /** Mapped message key or placeholder for membership visibility. */
    private String whoCanView;
    /** Mirrors Group Settings {@code allowExternalMembers}. */
    private boolean allowExternalMembers;
}
