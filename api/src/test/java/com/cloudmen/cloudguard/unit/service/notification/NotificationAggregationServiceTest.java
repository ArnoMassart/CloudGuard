package com.cloudmen.cloudguard.unit.service.notification;

import com.cloudmen.cloudguard.configuration.NotificationProjectionProperties;
import com.cloudmen.cloudguard.domain.model.Organization;
import com.cloudmen.cloudguard.domain.model.User;
import com.cloudmen.cloudguard.domain.model.UserRole;
import com.cloudmen.cloudguard.domain.model.DnsRecordImportance;
import com.cloudmen.cloudguard.domain.model.DnsRecordStatus;
import com.cloudmen.cloudguard.domain.model.notification.NotificationInstance;
import com.cloudmen.cloudguard.domain.model.notification.NotificationSeverity;
import com.cloudmen.cloudguard.dto.apppasswords.AppPasswordOverviewResponse;
import com.cloudmen.cloudguard.dto.dns.DnsRecordDto;
import com.cloudmen.cloudguard.dto.dns.DnsRecordResponseDto;
import com.cloudmen.cloudguard.dto.domain.DomainDto;
import com.cloudmen.cloudguard.dto.users.UserOverviewResponse;
import com.cloudmen.cloudguard.service.*;
import com.cloudmen.cloudguard.service.dns.DnsRecordsService;
import com.cloudmen.cloudguard.domain.model.notification.NotificationInstanceStatus;
import com.cloudmen.cloudguard.repository.NotificationInstanceRepository;
import com.cloudmen.cloudguard.service.notification.NotificationAggregationService;
import com.cloudmen.cloudguard.service.notification.NotificationFeedbackService;
import com.cloudmen.cloudguard.service.preference.UserSecurityPreferenceService;
import com.cloudmen.cloudguard.unit.helper.GlobalTestHelper;
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
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationAggregationServiceTest {

    @Mock GoogleDomainService domainService;
    @Mock DnsRecordsService dnsRecordsService;
    @Mock GoogleUsersService usersService;
    @Mock GoogleSharedDriveService driveService;
    @Mock GoogleDeviceService deviceService;
    @Mock AppPasswordsService appPasswordsService;
    @Mock GoogleGroupsService groupsService;
    @Mock GoogleOAuthService oAuthService;
    @Mock PasswordSettingsService passwordSettingsService;
    @Mock NotificationFeedbackService feedbackService;
    @Mock UserSecurityPreferenceService preferenceService;
    @Mock NotificationInstanceRepository notificationInstanceRepository;
    @Mock NotificationProjectionProperties notificationProjectionProperties;
    @Mock UserService userService;
    @Mock OrganizationService organizationService;

    private ResourceBundleMessageSource messageSource;
    private NotificationAggregationService service;

    @BeforeEach
    void setUp() {
        messageSource = new ResourceBundleMessageSource();
        messageSource.setBasenames("messages");
        messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
        messageSource.setFallbackToSystemLocale(false);
        LocaleContextHolder.setLocale(Locale.ENGLISH);

        service = new NotificationAggregationService(
                domainService,
                dnsRecordsService,
                usersService,
                driveService,
                deviceService,
                appPasswordsService,
                groupsService,
                oAuthService,
                messageSource,
                passwordSettingsService,
                feedbackService,
                preferenceService,
                notificationInstanceRepository,
                notificationProjectionProperties,
                userService,
                organizationService);
    }

    @AfterEach
    void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void getNotifications_addsCriticalUserControl_whenUsersWithout2fa() {
        stubQuietBaselines();
        lenient().when(usersService.getUsersPageOverview(eq(GlobalTestHelper.ADMIN), any()))
                .thenReturn(new UserOverviewResponse(10, 2, 0, 100, 0, 0, null, null));

        var response = service.getNotifications(GlobalTestHelper.ADMIN, Locale.ENGLISH);

        assertEquals(1, response.active().size());
        var n = response.active().get(0);
        assertEquals("critical", n.severity());
        assertEquals("user-control", n.notificationType());
        assertEquals("users-groups", n.source());
        assertTrue(n.supportsDetails());
        assertFalse(n.hasReported());
    }

    @Test
    void getNotifications_filtersOutActiveWhenPreferenceMapsToDisabledKey() {
        stubQuietBaselines();
        lenient().when(preferenceService.getDisabledPreferenceKeys(GlobalTestHelper.ADMIN))
                .thenReturn(Set.of("users-groups:2fa"));
        lenient().when(usersService.getUsersPageOverview(eq(GlobalTestHelper.ADMIN), any()))
                .thenReturn(new UserOverviewResponse(10, 2, 0, 100, 0, 0, null, null));

        var response = service.getNotifications(GlobalTestHelper.ADMIN, Locale.ENGLISH);

        assertTrue(response.active().isEmpty());
    }

    @Test
    void getNotifications_excludesActiveWhenMarkedDisabledInDb() {
        stubQuietBaselines();

        User orgUser = new User();
        orgUser.setEmail(GlobalTestHelper.ADMIN);
        orgUser.setOrganizationId(99L);
        orgUser.setRoles(List.of(UserRole.SUPER_ADMIN));
        lenient().when(userService.findByEmailOptional(GlobalTestHelper.ADMIN)).thenReturn(Optional.of(orgUser));
        lenient().when(userService.findByEmail(GlobalTestHelper.ADMIN)).thenReturn(orgUser);

        lenient().when(usersService.getUsersPageOverview(eq(GlobalTestHelper.ADMIN), any()))
                .thenReturn(new UserOverviewResponse(10, 2, 0, 100, 0, 0, null, null));

        lenient().when(notificationInstanceRepository.existsByOrganizationId(99L)).thenReturn(false);
        lenient()
                .when(notificationInstanceRepository.findByOrganizationIdAndSourceAndNotificationType(
                        eq(99L), eq("users-groups"), eq("user-control")))
                .thenReturn(Optional.empty());
        lenient().when(notificationInstanceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        NotificationInstance disabled = new NotificationInstance();
        disabled.setId(1L);
        disabled.setOrganizationId(99L);
        disabled.setSource("users-groups");
        disabled.setNotificationType("user-control");
        disabled.setStatus(NotificationInstanceStatus.DISABLED);
        lenient()
                .when(notificationInstanceRepository.findByOrganizationIdAndStatus(
                        eq(99L), eq(NotificationInstanceStatus.DISABLED)))
                .thenReturn(List.of(disabled));

        var response = service.getNotifications(GlobalTestHelper.ADMIN, Locale.ENGLISH);

        assertTrue(response.active().isEmpty());
        verify(notificationInstanceRepository).save(any());
    }

    @Test
    void getNotifications_setsHasReportedFromGlobalFeedbackKeys() {
        stubQuietBaselines();
        lenient().when(usersService.getUsersPageOverview(eq(GlobalTestHelper.ADMIN), any()))
                .thenReturn(new UserOverviewResponse(10, 2, 0, 100, 0, 0, null, null));
        lenient().when(feedbackService.getAllFeedbackKeys()).thenReturn(Set.of("users-groups:user-control"));

        var response = service.getNotifications(GlobalTestHelper.ADMIN, Locale.ENGLISH);

        assertEquals(1, response.active().size());
        assertTrue(response.active().get(0).hasReported());
    }

    @Test
    void getNotifications_dnsCritical_whenSpfActionRequired() {
        stubQuietBaselines();
        lenient().when(usersService.getUsersPageOverview(eq(GlobalTestHelper.ADMIN), any()))
                .thenReturn(new UserOverviewResponse(10, 0, 0, 100, 0, 0, null, null));

        var badSpf = new DnsRecordDto(
                "SPF",
                "example.com",
                List.of(),
                DnsRecordStatus.ACTION_REQUIRED,
                DnsRecordImportance.REQUIRED,
                "missing");
        lenient().when(dnsRecordsService.getImportantRecords(eq("example.com"), eq("google"), any()))
                .thenReturn(new DnsRecordResponseDto("example.com", List.of(badSpf), 40, null));

        var response = service.getNotifications(GlobalTestHelper.ADMIN, Locale.ENGLISH);

        assertEquals(1, response.active().size());
        assertEquals("dns-critical", response.active().get(0).notificationType());
        assertEquals("critical", response.active().get(0).severity());
    }

    @Test
    void getNotificationsCount_matchesActiveListSize() {
        stubQuietBaselines();
        lenient().when(usersService.getUsersPageOverview(eq(GlobalTestHelper.ADMIN), any()))
                .thenReturn(new UserOverviewResponse(10, 2, 0, 100, 0, 0, null, null));

        assertEquals(1, service.getNotificationsCount(GlobalTestHelper.ADMIN));
    }

    @Test
    void getNotificationsCriticalCount_countsOnlyCriticalSeverity() {
        stubQuietBaselines();
        lenient().when(usersService.getUsersPageOverview(eq(GlobalTestHelper.ADMIN), any()))
                .thenReturn(new UserOverviewResponse(10, 2, 0, 100, 0, 0, null, null));
        lenient().when(groupsService.getGroupsOverview(eq(GlobalTestHelper.ADMIN), any()))
                .thenReturn(new com.cloudmen.cloudguard.dto.groups.GroupOverviewResponse(
                        1, 1, 0, 0, 0, 100, null, null));

        long critical = service.getNotificationsCriticalCount(GlobalTestHelper.ADMIN);

        assertEquals(1, critical);
    }

    @Test
    void getCriticalNotifications_returnsOnlyCriticalEntries() {
        stubQuietBaselines();
        lenient().when(usersService.getUsersPageOverview(eq(GlobalTestHelper.ADMIN), any()))
                .thenReturn(new UserOverviewResponse(10, 2, 0, 100, 0, 0, null, null));
        lenient().when(groupsService.getGroupsOverview(eq(GlobalTestHelper.ADMIN), any()))
                .thenReturn(new com.cloudmen.cloudguard.dto.groups.GroupOverviewResponse(
                        1, 1, 0, 0, 0, 100, null, null));

        var critical = service.getCriticalNotifications(GlobalTestHelper.ADMIN, Locale.ENGLISH);

        assertEquals(1, critical.size());
        assertEquals("critical", critical.get(0).severity());
    }

    @Test
    void getNotifications_hidesSourceWhenViewerLacksMatchingViewerRole() {
        stubQuietBaselines();

        User viewer = new User();
        viewer.setEmail(GlobalTestHelper.ADMIN);
        viewer.setRoles(List.of(UserRole.DOMAIN_DNS_VIEWER));
        viewer.setOrganizationId(99L); // Noodzakelijk voor de admin validatie check!

        lenient().when(userService.findByEmailOptional(GlobalTestHelper.ADMIN)).thenReturn(Optional.of(viewer));
        lenient().when(userService.findByEmail(GlobalTestHelper.ADMIN)).thenReturn(viewer);

        lenient().when(usersService.getUsersPageOverview(eq(GlobalTestHelper.ADMIN), any()))
                .thenReturn(new UserOverviewResponse(10, 2, 0, 100, 0, 0, null, null));

        var response = service.getNotifications(GlobalTestHelper.ADMIN, Locale.ENGLISH);

        assertTrue(response.active().isEmpty());
    }

    @Test
    void getNotifications_localizesProjectedNotificationPerRequestLocale() {
        stubQuietBaselines();
        lenient().when(notificationProjectionProperties.isReadEnabled()).thenReturn(true);

        User orgUser = new User();
        orgUser.setEmail(GlobalTestHelper.ADMIN);
        orgUser.setOrganizationId(7L);
        orgUser.setRoles(List.of(UserRole.SUPER_ADMIN));
        lenient().when(userService.findByEmailOptional(GlobalTestHelper.ADMIN)).thenReturn(Optional.of(orgUser));
        lenient().when(notificationInstanceRepository.existsByOrganizationId(7L)).thenReturn(true);
        lenient().when(notificationInstanceRepository.findByOrganizationIdAndStatus(
                eq(7L), eq(NotificationInstanceStatus.DISABLED))).thenReturn(List.of());
        lenient().when(notificationInstanceRepository.findByOrganizationIdAndStatus(
                eq(7L), eq(NotificationInstanceStatus.SOLVED))).thenReturn(new ArrayList<>());

        NotificationInstance row = new NotificationInstance();
        row.setId(100L);
        row.setOrganizationId(7L);
        row.setSource("devices");
        row.setSourceLabel("Devices");
        row.setSourceRoute("/devices");
        row.setNotificationType("device-os");
        row.setSeverity(NotificationSeverity.WARNING);
        row.setTitle("Devices with outdated OS version");
        row.setDescription("3 device(s) are running an outdated operating system version.");
        row.setRecommendedActions(List.of("Require a minimum OS version for all devices"));
        lenient().when(notificationInstanceRepository.findByOrganizationIdAndStatus(
                eq(7L), eq(NotificationInstanceStatus.ACTIVE))).thenReturn(List.of(row));

        var response = service.getNotifications(GlobalTestHelper.ADMIN, Locale.forLanguageTag("nl"));

        assertEquals(1, response.active().size());
        var n = response.active().get(0);
        assertEquals("Apparaten met verouderde OS versie", n.title());
        assertTrue(n.description().contains("3"));
        assertEquals("Apparaten", n.sourceLabel());
        assertEquals(List.of("Vereis minimale OS-versie voor alle apparaten"), n.recommendedActions());
    }

    private void stubQuietBaselines() {
        lenient().when(notificationProjectionProperties.isReadEnabled()).thenReturn(true);

        // Zorg dat de mock-organisatie altijd een geldig Admin e-mailadres klaar heeft staan
        Organization mockOrg = new Organization();
        mockOrg.setId(99L);
        mockOrg.setAdminEmail(GlobalTestHelper.ADMIN);
        lenient().when(organizationService.findById(anyLong())).thenReturn(Optional.of(mockOrg));

        // Zorg dat de mock-gebruiker correct is gekoppeld aan de organisatie
        User superAdmin = new User();
        superAdmin.setEmail(GlobalTestHelper.ADMIN);
        superAdmin.setRoles(List.of(UserRole.SUPER_ADMIN));
        superAdmin.setOrganizationId(99L);
        lenient().when(userService.findByEmailOptional(anyString())).thenReturn(Optional.of(superAdmin));
        lenient().when(userService.findByEmail(anyString())).thenReturn(superAdmin);

        lenient().when(preferenceService.getDisabledPreferenceKeys(GlobalTestHelper.ADMIN)).thenReturn(Set.of());
        lenient().when(preferenceService.getDnsImportanceOverrides(GlobalTestHelper.ADMIN)).thenReturn(Map.of());

        lenient().when(domainService.getAllDomains(GlobalTestHelper.ADMIN))
                .thenReturn(List.of(new DomainDto("example.com", "Primary Domain", true, 1)));
        lenient().when(dnsRecordsService.getImportantRecords(eq("example.com"), eq("google"), any()))
                .thenReturn(new DnsRecordResponseDto("example.com", List.of(), 100, null));

        lenient().when(usersService.getUsersPageOverview(eq(GlobalTestHelper.ADMIN), any()))
                .thenReturn(new UserOverviewResponse(10, 0, 0, 100, 0, 0, null, null));
        lenient().when(driveService.getDrivesPageOverview(eq(GlobalTestHelper.ADMIN), any())).thenReturn(null);
        lenient().when(deviceService.getDevicesPageOverview(eq(GlobalTestHelper.ADMIN), any())).thenReturn(null);
        lenient().when(groupsService.getGroupsOverview(eq(GlobalTestHelper.ADMIN), any())).thenReturn(null);
        lenient().when(oAuthService.getOAuthPageOverview(eq(GlobalTestHelper.ADMIN), any())).thenReturn(null);
        lenient().when(appPasswordsService.getOverview(eq(GlobalTestHelper.ADMIN), eq(true), any()))
                .thenReturn(new AppPasswordOverviewResponse(true, 0, 0, 100, null));
        lenient().when(passwordSettingsService.getPasswordSettings(GlobalTestHelper.ADMIN)).thenReturn(null);

        lenient().when(feedbackService.getAllFeedbackKeys()).thenReturn(Set.of());
    }
}