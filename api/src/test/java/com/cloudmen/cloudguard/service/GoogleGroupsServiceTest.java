package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.dto.groups.CachedGroupItem;
import com.cloudmen.cloudguard.dto.groups.GroupCacheEntry;
import com.cloudmen.cloudguard.dto.groups.GroupOrgDetail;
import com.cloudmen.cloudguard.dto.password.SecurityScoreFactorDto;
import com.cloudmen.cloudguard.service.cache.GoogleGroupsCacheService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleGroupsServiceTest {

    private static final String ADMIN = "admin@example.com";

    @Mock
    GoogleGroupsCacheService groupsCacheService;

    private ResourceBundleMessageSource messageSource;
    private GoogleGroupsService service;

    @BeforeEach
    void setUp() {
        messageSource = new ResourceBundleMessageSource();
        messageSource.setBasenames("messages");
        messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
        messageSource.setFallbackToSystemLocale(false);
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        service = new GoogleGroupsService(groupsCacheService, messageSource);
    }

    @AfterEach
    void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void forceRefreshCache_delegatesToCacheService() {
        service.forceRefreshCache(ADMIN);
        verify(groupsCacheService).forceRefreshCache(ADMIN);
    }

    @Test
    void getGroupsPaged_emptyCache_returnsEmptyPageAndNoNextToken() {
        when(groupsCacheService.getOrFetchGroupData(ADMIN)).thenReturn(entry(List.of()));

        var page = service.getGroupsPaged(ADMIN, null, null, 10);

        assertTrue(page.groups().isEmpty());
        assertNull(page.nextPageToken());
    }

    @Test
    void getGroupsPaged_queryFiltersByNameOrEmail() {
        List<CachedGroupItem> groups = List.of(
                group("Alpha Team", "alpha@example.com", "LOW", 0, false),
                group("Beta Club", "other@example.com", "LOW", 0, false));
        when(groupsCacheService.getOrFetchGroupData(ADMIN)).thenReturn(entry(groups));

        var page = service.getGroupsPaged(ADMIN, "beta", null, 10);

        assertEquals(1, page.groups().size());
        assertEquals("Beta Club", page.groups().get(0).getName());
    }

    @Test
    void getGroupsPaged_blankQuery_returnsAllInOrder() {
        List<CachedGroupItem> groups = List.of(
                group("A", "a@x.com", "LOW", 0, false),
                group("B", "b@x.com", "LOW", 0, false));
        when(groupsCacheService.getOrFetchGroupData(ADMIN)).thenReturn(entry(groups));

        var page = service.getGroupsPaged(ADMIN, "   ", null, 1);

        assertEquals("A", page.groups().get(0).getName());
        assertEquals("2", page.nextPageToken());
    }

    @Test
    void getGroupsPaged_middlePage_returnsNextTokenWhenMoreRemain() {
        List<CachedGroupItem> groups = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            groups.add(group("G" + i, "g" + i + "@x.com", "LOW", 0, false));
        }
        when(groupsCacheService.getOrFetchGroupData(ADMIN)).thenReturn(entry(groups));

        var page = service.getGroupsPaged(ADMIN, null, "2", 2);

        assertEquals(2, page.groups().size());
        assertEquals("G2", page.groups().get(0).getName());
        assertEquals("3", page.nextPageToken());
    }

    @Test
    void getGroupsPaged_lastPage_nextTokenNull() {
        List<CachedGroupItem> groups = List.of(
                group("G0", "g0@x.com", "LOW", 0, false),
                group("G1", "g1@x.com", "LOW", 0, false));
        when(groupsCacheService.getOrFetchGroupData(ADMIN)).thenReturn(entry(groups));

        var page = service.getGroupsPaged(ADMIN, null, "1", 2);

        assertEquals(2, page.groups().size());
        assertNull(page.nextPageToken());
    }

    @Test
    void getGroupsPaged_pageBeyondTotal_returnsEmptyList() {
        List<CachedGroupItem> groups = List.of(group("Only", "o@x.com", "LOW", 0, false));
        when(groupsCacheService.getOrFetchGroupData(ADMIN)).thenReturn(entry(groups));

        var page = service.getGroupsPaged(ADMIN, null, "5", 2);

        assertTrue(page.groups().isEmpty());
    }

    @Test
    void getGroupsOverview_noGroups_securityScore100() {
        when(groupsCacheService.getOrFetchGroupData(ADMIN)).thenReturn(entry(List.of()));

        var overview = service.getGroupsOverview(ADMIN);

        assertEquals(0, overview.totalGroups());
        assertEquals(100, overview.securityScore());
        assertEquals("perfect", overview.securityScoreBreakdown().status());
        assertNull(overview.warnings());
    }

    @Test
    void getGroupsOverview_mixedRisk_computesWeightedScore() {
        List<CachedGroupItem> groups = List.of(
                group("Low", "l@x.com", "LOW", 0, false),
                group("Med", "m@x.com", "MEDIUM", 0, false),
                group("Hi", "h@x.com", "HIGH", 0, false));
        when(groupsCacheService.getOrFetchGroupData(ADMIN)).thenReturn(entry(groups));

        var overview = service.getGroupsOverview(ADMIN);

        assertEquals(3, overview.totalGroups());
        assertEquals(1, overview.highRiskGroups());
        assertEquals(1, overview.mediumRiskGroups());
        assertEquals(1, overview.lowRiskGroups());
        assertEquals(60, overview.securityScore());
    }

    @Test
    void getGroupsOverview_externalDetected_fromMembersOrAllowFlag() {
        List<CachedGroupItem> groups = List.of(
                group("Open", "o@x.com", "LOW", 0, true),
                group("Guests", "g@x.com", "LOW", 3, false),
                group("Closed", "c@x.com", "LOW", 0, false));
        when(groupsCacheService.getOrFetchGroupData(ADMIN)).thenReturn(entry(groups));

        var overview = service.getGroupsOverview(ADMIN);

        assertEquals(2, overview.groupsWithExternal());
    }

    @Test
    void getGroupsOverview_withDisabledGroupExternalPreference_forcesScore100AndMutedBreakdown() {
        List<CachedGroupItem> groups = List.of(
                group("Hi", "h@x.com", "HIGH", 1, true));
        when(groupsCacheService.getOrFetchGroupData(ADMIN)).thenReturn(entry(groups));

        var overview = service.getGroupsOverview(ADMIN, Set.of("users-groups:groupExternal"));

        assertEquals(1, overview.highRiskGroups());
        assertEquals(100, overview.securityScore());
        assertTrue(overview.securityScoreBreakdown().factors().stream().allMatch(SecurityScoreFactorDto::muted));
        assertNotNull(overview.warnings());
        assertFalse(overview.warnings().hasWarnings());
    }

    @Test
    void getGroupsOverview_withWarnings_whenRiskAndPreferenceEnabled() {
        List<CachedGroupItem> groups = List.of(
                group("Hi", "h@x.com", "HIGH", 1, false));
        when(groupsCacheService.getOrFetchGroupData(ADMIN)).thenReturn(entry(groups));

        var overview = service.getGroupsOverview(ADMIN, Set.of());

        assertTrue(overview.warnings().hasWarnings());
        assertTrue(overview.warnings().items().get("externalMember"));
        assertTrue(overview.warnings().items().get("highRisk"));
    }

    private static GroupCacheEntry entry(List<CachedGroupItem> items) {
        return new GroupCacheEntry(items, System.currentTimeMillis());
    }

    private static CachedGroupItem group(
            String name,
            String email,
            String risk,
            int externalMembers,
            boolean externalAllowed) {
        GroupOrgDetail detail = new GroupOrgDetail(
                name,
                "admin-1",
                risk,
                List.of(),
                10,
                externalMembers,
                externalAllowed,
                "ANYONE_CAN_JOIN",
                "ANYONE_CAN_VIEW");
        return new CachedGroupItem(name, email, detail);
    }
}
