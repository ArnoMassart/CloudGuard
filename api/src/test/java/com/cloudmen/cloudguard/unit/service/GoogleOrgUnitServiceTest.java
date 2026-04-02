package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.dto.organization.OrgUnitCacheEntry;
import com.cloudmen.cloudguard.dto.organization.OrgUnitNodeDto;
import com.cloudmen.cloudguard.service.cache.GoogleOrgUnitCacheService;
import com.google.api.services.admin.directory.model.OrgUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleOrgUnitServiceTest {

    private static final String ADMIN = "admin@acme.com";

    @Mock
    private GoogleOrgUnitCacheService orgUnitCacheService;

    private ResourceBundleMessageSource messageSource;
    private GoogleOrgUnitService service;

    @BeforeEach
    void setUp() {
        messageSource = new ResourceBundleMessageSource();
        messageSource.setBasenames("messages");
        messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
        messageSource.setFallbackToSystemLocale(false);
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        service = new GoogleOrgUnitService(orgUnitCacheService, messageSource);
    }

    @AfterEach
    void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void forceRefreshCache_delegatesToCacheService() {
        service.forceRefreshCache(ADMIN);
        verify(orgUnitCacheService).forceRefreshCache(ADMIN);
    }

    @Test
    void getOrgUnitTree_nullOrgList_returnsEmptyRootWithI18nName() {
        when(orgUnitCacheService.getOrFetchOrgUnitData(ADMIN))
                .thenReturn(new OrgUnitCacheEntry(null, Map.of(), 0L));

        OrgUnitNodeDto root = service.getOrgUnitTree(ADMIN);

        assertEquals("/", root.getOrgUnitPath());
        assertTrue(root.isRoot());
        assertEquals("Organisation", root.getName());
        assertEquals(0, root.getUserCount());
        assertTrue(root.getChildren().isEmpty());
    }

    @Test
    void getOrgUnitTree_emptyOrgList_returnsEmptyRootWithI18nName() {
        when(orgUnitCacheService.getOrFetchOrgUnitData(ADMIN))
                .thenReturn(new OrgUnitCacheEntry(List.of(), Map.of(), 0L));

        OrgUnitNodeDto root = service.getOrgUnitTree(ADMIN);

        assertEquals("/", root.getOrgUnitPath());
        assertTrue(root.isRoot());
        assertEquals("Organisation", root.getName());
        assertEquals(0, root.getUserCount());
        assertTrue(root.getChildren().isEmpty());
    }

    @Test
    void getOrgUnitTree_buildsHierarchyWithDomainRootAndSortedChildren() {
        OrgUnit sales = ou("/Sales", "/", "Sales");
        OrgUnit engineering = ou("/Engineering", "/", "Engineering");
        OrgUnit emea = ou("/Sales/EMEA", "/Sales", "EMEA");
        List<OrgUnit> units = List.of(sales, engineering, emea);
        Map<String, Integer> counts = new HashMap<>();
        counts.put("/", 100);
        counts.put("/Engineering", 40);
        counts.put("/Sales", 50);
        counts.put("/Sales/EMEA", 10);

        when(orgUnitCacheService.getOrFetchOrgUnitData(ADMIN))
                .thenReturn(new OrgUnitCacheEntry(units, counts, 1L));

        OrgUnitNodeDto root = service.getOrgUnitTree(ADMIN);

        assertEquals("acme.com", root.getName());
        assertEquals("/", root.getOrgUnitPath());
        assertTrue(root.isRoot());
        assertEquals(100, root.getUserCount());
        assertEquals(2, root.getChildren().size());

        OrgUnitNodeDto first = root.getChildren().get(0);
        OrgUnitNodeDto second = root.getChildren().get(1);
        assertEquals("Engineering", first.getName());
        assertEquals("/Engineering", first.getOrgUnitPath());
        assertEquals(40, first.getUserCount());
        assertTrue(first.getChildren().isEmpty());

        assertEquals("Sales", second.getName());
        assertEquals("/Sales", second.getOrgUnitPath());
        assertEquals(50, second.getUserCount());
        assertEquals(1, second.getChildren().size());

        OrgUnitNodeDto emeaNode = second.getChildren().get(0);
        assertEquals("EMEA", emeaNode.getName());
        assertEquals("/Sales/EMEA", emeaNode.getOrgUnitPath());
        assertEquals(10, emeaNode.getUserCount());
        assertTrue(emeaNode.getChildren().isEmpty());
    }

    @Test
    void getOrgUnitTree_skipsOrgUnitsWithBlankPath() {
        OrgUnit bad = new OrgUnit();
        bad.setOrgUnitPath("   ");
        bad.setParentOrgUnitPath("/");
        OrgUnit good = ou("/Legal", "/", "Legal");
        when(orgUnitCacheService.getOrFetchOrgUnitData(ADMIN))
                .thenReturn(new OrgUnitCacheEntry(List.of(bad, good), Map.of("/", 1, "/Legal", 2), 0L));

        OrgUnitNodeDto root = service.getOrgUnitTree(ADMIN);

        assertEquals(1, root.getChildren().size());
        assertEquals("Legal", root.getChildren().get(0).getName());
    }

    @Test
    void getOrgUnitTree_usesPathSegmentWhenNameBlank() {
        OrgUnit finance = ou("/Finance", "/", "Finance");
        OrgUnit ap = ou("/Finance/Ap", "/Finance", " ");
        ap.setName(null);
        when(orgUnitCacheService.getOrFetchOrgUnitData(ADMIN))
                .thenReturn(new OrgUnitCacheEntry(List.of(finance, ap), Map.of("/Finance/Ap", 3), 0L));

        OrgUnitNodeDto root = service.getOrgUnitTree(ADMIN);
        assertEquals(1, root.getChildren().size());
        OrgUnitNodeDto financeNode = root.getChildren().get(0);
        assertEquals("Finance", financeNode.getName());
        assertEquals(1, financeNode.getChildren().size());
        OrgUnitNodeDto apNode = financeNode.getChildren().get(0);
        assertEquals("Ap", apNode.getName());
        assertEquals("/Finance/Ap", apNode.getOrgUnitPath());
        assertEquals(3, apNode.getUserCount());
    }

    private static OrgUnit ou(String path, String parentPath, String name) {
        OrgUnit o = new OrgUnit();
        o.setOrgUnitPath(path);
        o.setParentOrgUnitPath(parentPath);
        o.setName(name);
        return o;
    }
}
