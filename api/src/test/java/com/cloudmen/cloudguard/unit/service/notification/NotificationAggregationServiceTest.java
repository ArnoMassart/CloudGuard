package com.cloudmen.cloudguard.unit.service.notification;

import com.cloudmen.cloudguard.configuration.NotificationProjectionProperties;
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
import com.cloudmen.cloudguard.repository.NotificationInstanceRepository;
import com.cloudmen.cloudguard.repository.UserRepository;
import com.cloudmen.cloudguard.service.notification.DismissedNotificationService;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationAggregationServiceTest {

    @Mock
    GoogleDomainService domainService;
    @Mock
    DnsRecordsService dnsRecordsService;
    @Mock
    GoogleUsersService usersService;
    @Mock
    GoogleSharedDriveService driveService;
    @Mock
    GoogleDeviceService deviceService;
    @Mock
    AppPasswordsService appPasswordsService;
    @Mock
    GoogleGroupsService groupsService;
    @Mock
    GoogleOAuthService oAuthService;
    @Mock
    PasswordSettingsService passwordSettingsService;
    @Mock
    DismissedNotificationService dismissedService;
    @Mock
    NotificationFeedbackService feedbackService;
    @Mock
    UserSecurityPreferenceService preferenceService;
    @Mock
    UserRepository userRepository;
    @Mock
    NotificationInstanceRepository notificationInstanceRepository;
    @Mock
    NotificationProjectionProperties notificationProjectionProperties;

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
                dismissedService,
                feedbackService,
                preferenceService,
                userRepository,
                notificationInstanceRepository,
                notificationProjectionProperties);
    }

    @AfterEach
    void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void getNotifications_addsCriticalUserControl_whenUsersWithout2fa() {
        stubQuietBaselines();
        when(usersService.getUsersPageOverview(eq(GlobalTestHelper.ADMIN), any()))
                .thenReturn(new UserOverviewResponse(10, 2, 0, 100, 0, 0, null, null));

        var response = service.getNotifications(GlobalTestHelper.ADMIN, Locale.ENGLISH);

        assertEquals(1, response.active().size());
        var n = response.active().get(0);
        assertEquals("critical", n.severity());
        assertEquals("user-control", n.notificationType());
        assertEquals("users-groups", n.source());
        assertTrue(n.supportsDetails());
        assertFalse(n.hasReported());
        assertFalse(n.dismissed());
    }

    @Test
    void getNotifications_filtersOutActiveWhenPreferenceMapsToDisabledKey() {
        stubQuietBaselines();
        when(preferenceService.getDisabledPreferenceKeys(GlobalTestHelper.ADMIN))
                .thenReturn(Set.of("users-groups:2fa"));
        when(usersService.getUsersPageOverview(eq(GlobalTestHelper.ADMIN), any()))
                .thenReturn(new UserOverviewResponse(10, 2, 0, 100, 0, 0, null, null));

        var response = service.getNotifications(GlobalTestHelper.ADMIN, Locale.ENGLISH);

        assertTrue(response.active().isEmpty());
    }

    @Test
    void getNotifications_excludesDismissedMatchingSourceAndType() {
        stubQuietBaselines();
        User orgUser = new User();
        orgUser.setEmail(GlobalTestHelper.ADMIN);
        orgUser.setOrganizationId(99L);
        when(userRepository.findByEmail(GlobalTestHelper.ADMIN)).thenReturn(Optional.of(orgUser));
        when(usersService.getUsersPageOverview(eq(GlobalTestHelper.ADMIN), any()))
                .thenReturn(new UserOverviewResponse(10, 2, 0, 100, 0, 0, null, null));

        NotificationInstance d = new NotificationInstance();
        d.setId(99L);
        d.setSource("users-groups");
        d.setNotificationType("user-control");
        d.setTitle("t");
        d.setDescription("d");
        d.setSeverity(NotificationSeverity.CRITICAL);
        when(dismissedService.getDismissedForOrganization(99L)).thenReturn(List.of(d));

        var response = service.getNotifications(GlobalTestHelper.ADMIN, Locale.ENGLISH);

        assertTrue(response.active().isEmpty());
    }

    @Test
    void getNotifications_setsHasReportedFromGlobalFeedbackKeys() {
        stubQuietBaselines();
        when(usersService.getUsersPageOverview(eq(GlobalTestHelper.ADMIN), any()))
                .thenReturn(new UserOverviewResponse(10, 2, 0, 100, 0, 0, null, null));
        when(feedbackService.getAllFeedbackKeys()).thenReturn(Set.of("users-groups:user-control"));

        var response = service.getNotifications(GlobalTestHelper.ADMIN, Locale.ENGLISH);

        assertEquals(1, response.active().size());
        assertTrue(response.active().get(0).hasReported());
    }

    @Test
    void getNotifications_dnsCritical_whenSpfActionRequired() {
        stubQuietBaselines();
        when(usersService.getUsersPageOverview(eq(GlobalTestHelper.ADMIN), any()))
                .thenReturn(new UserOverviewResponse(10, 0, 0, 100, 0, 0, null, null));

        var badSpf = new DnsRecordDto(
                "SPF",
                "example.com",
                List.of(),
                DnsRecordStatus.ACTION_REQUIRED,
                DnsRecordImportance.REQUIRED,
                "missing");
        when(dnsRecordsService.getImportantRecords(eq("example.com"), eq("google"), any()))
                .thenReturn(new DnsRecordResponseDto("example.com", List.of(badSpf), 40, null));

        var response = service.getNotifications(GlobalTestHelper.ADMIN, Locale.ENGLISH);

        assertEquals(1, response.active().size());
        assertEquals("dns-critical", response.active().get(0).notificationType());
        assertEquals("critical", response.active().get(0).severity());
    }

    @Test
    void getNotificationsCount_matchesActiveListSize() {
        stubQuietBaselines();
        when(usersService.getUsersPageOverview(eq(GlobalTestHelper.ADMIN), any()))
                .thenReturn(new UserOverviewResponse(10, 2, 0, 100, 0, 0, null, null));

        assertEquals(1, service.getNotificationsCount(GlobalTestHelper.ADMIN));
    }

    @Test
    void getNotificationsCriticalCount_countsOnlyCriticalSeverity() {
        stubQuietBaselines();
        when(usersService.getUsersPageOverview(eq(GlobalTestHelper.ADMIN), any()))
                .thenReturn(new UserOverviewResponse(10, 2, 0, 100, 0, 0, null, null));
        when(groupsService.getGroupsOverview(eq(GlobalTestHelper.ADMIN), any()))
                .thenReturn(new com.cloudmen.cloudguard.dto.groups.GroupOverviewResponse(
                        1, 1, 0, 0, 0, 100, null, null));

        long critical = service.getNotificationsCriticalCount(GlobalTestHelper.ADMIN);

        assertEquals(1, critical);
    }

    @Test
    void getCriticalNotifications_returnsOnlyCriticalEntries() {
        stubQuietBaselines();
        when(usersService.getUsersPageOverview(eq(GlobalTestHelper.ADMIN), any()))
                .thenReturn(new UserOverviewResponse(10, 2, 0, 100, 0, 0, null, null));
        when(groupsService.getGroupsOverview(eq(GlobalTestHelper.ADMIN), any()))
                .thenReturn(new com.cloudmen.cloudguard.dto.groups.GroupOverviewResponse(
                        1, 1, 0, 0, 0, 100, null, null));

        var critical = service.getCriticalNotifications(GlobalTestHelper.ADMIN, Locale.ENGLISH);

        assertEquals(1, critical.size());
        assertEquals("critical", critical.get(0).severity());
    }

    @Test
    void getNotifications_hidesSourceWhenViewerLacksMatchingViewerRole() {
        stubQuietBaselines();
        User u = new User();
        u.setEmail(GlobalTestHelper.ADMIN);
        u.setOrganizationId(null);
        u.setRoles(new ArrayList<>(List.of(UserRole.DOMAIN_DNS_VIEWER)));
        when(userRepository.findByEmail(GlobalTestHelper.ADMIN)).thenReturn(Optional.of(u));
        when(usersService.getUsersPageOverview(eq(GlobalTestHelper.ADMIN), any()))
                .thenReturn(new UserOverviewResponse(10, 2, 0, 100, 0, 0, null, null));

        var response = service.getNotifications(GlobalTestHelper.ADMIN, Locale.ENGLISH);

        assertTrue(response.active().isEmpty());
    }

    private void stubQuietBaselines() {
        lenient().when(notificationProjectionProperties.isReadEnabled()).thenReturn(true);
        lenient().when(userRepository.findByEmail(GlobalTestHelper.ADMIN)).thenReturn(Optional.empty());

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

        lenient().when(dismissedService.getDismissedForOrganization(null)).thenReturn(List.of());
        lenient().when(feedbackService.getAllFeedbackKeys()).thenReturn(Set.of());
    }
}
