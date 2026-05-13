package com.cloudmen.cloudguard.dto.organization;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * One node in the OU tree returned by {@link com.cloudmen.cloudguard.service.GoogleOrgUnitService#getOrgUnitTree(String)}.
 * The synthetic domain root uses path {@code "/"} and {@code isRoot == true}; nested nodes mirror Google paths such as
 * {@code /Sales/EMEA}.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class OrgUnitNodeDto {
    /** Stable identifier for APIs; currently the same as {@link #orgUnitPath}. */
    private String id;
    /** Display label (OU name or last path segment when name is blank). */
    private String name;
    /** Admin SDK organizational unit path. */
    private String orgUnitPath;
    /** Users assigned to this path per cached Directory tally. */
    private int userCount;
    /** Child OUs directly under this path. */
    private List<OrgUnitNodeDto> children = new ArrayList<>();
    /** {@code true} only for the top-level domain root row. */
    private boolean isRoot;
}
