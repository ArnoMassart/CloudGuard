package com.cloudmen.cloudguard.dto.groups;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Normalized group payload returned inside {@link GroupPageResponse} and embedded in {@link CachedGroupItem}.
 * <p>
 * Note: {@link #name} is populated with the group’s <strong>email</strong> when built by
 * {@link com.cloudmen.cloudguard.service.cache.GoogleGroupsCacheService}; the display title lives on
 * {@link CachedGroupItem#name()}.
 */
@AllArgsConstructor
@Getter
@Setter
public class GroupOrgDetail {
    /** Primary group email used as stable identifier in cache and APIs. */
    private String name;
    /** Google Directory resource id for the group. */
    private String adminId;
    /** Risk tier: {@code HIGH}, {@code MEDIUM}, or {@code LOW} from external members and policy. */
    private String risk;
    /** UI labels (base risk tag plus optional Cloud Identity labels such as {@code mailing}). */
    private List<String> tags;
    /** Count of USER-type members returned by Directory for this group. */
    private int totalMembers;
    /** USER members whose email is outside the workspace primary domain. */
    private int externalMembers;
    /** {@code true} when Group Settings allow adding external members. */
    private boolean externalAllowed;
    /** Message key or raw enum for join policy ({@code groups.whoCanJoin.*}). */
    private String whoCanJoin;
    /** Message key or raw enum for membership visibility ({@code groups.whoCanView.*}). */
    private String whoCanView;
}
