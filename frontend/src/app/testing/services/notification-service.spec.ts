import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { TranslocoService } from '@jsverse/transloco';
import { firstValueFrom, of, throwError } from 'rxjs';
import { AggregatedAppDto } from '../../models/o-auth/AggregatedAppDto';
import { Device } from '../../models/devices/Device';
import { SharedDrive } from '../../models/drives/SharedDrive';
import { GroupOrgDetail } from '../../models/groups/GroupOrgDetail';
import { Notification } from '../../models/notification/Notification';
import { NotificationsResponse } from '../../models/notification/NotificationsResponse';
import { PasswordSettings } from '../../models/password/PasswordSettings';
import { DeviceService } from '../../services/device-service';
import { DriveService } from '../../services/drive-service';
import { GroupService } from '../../services/group-service';
import { NotificationService } from '../../services/notification-service';
import { OAuthService } from '../../services/o-auth-service';
import { PasswordSettingsService } from '../../services/password-settings-service';
import { UserService } from '../../services/user-service';

const emptyPasswordSettings: PasswordSettings = {
  passwordPoliciesByOu: [],
  twoStepVerification: {
    byOrgUnit: [],
    totalEnrolled: 0,
    totalEnforced: 0,
    totalUsers: 0,
  },
  usersWithForcedChange: [],
  summary: {
    usersWithForcedChange: 0,
    usersWith2SvEnrolled: 0,
    usersWith2SvEnforced: 0,
    totalUsers: 0,
  },
  adminsWithoutSecurityKeys: [],
  securityScore: 0,
};

function baseNotification(overrides: Partial<Notification> & Pick<Notification, 'notificationType'>): Notification {
  return {
    id: '1',
    severity: 'info',
    title: 'T',
    description: 'D',
    source: 's',
    sourceLabel: 'L',
    sourceRoute: '/',
    ...overrides,
  };
}

function shareDrive(overrides: Partial<SharedDrive>): SharedDrive {
  return {
    id: 'd1',
    name: 'Drive A',
    totalMembers: 3,
    externalMembers: 0,
    totalOrganizers: 1,
    createdTime: '',
    parsedTime: '',
    onlyDomainUsersAllowed: true,
    onlyMembersCanAccess: true,
    risk: 'low',
    ...overrides,
  };
}

function makeDevice(overrides: Partial<Device>): Device {
  return {
    resourceId: 'r1',
    deviceType: 'phone',
    userName: 'User',
    userEmail: 'u@x.com',
    deviceName: 'Pixel',
    model: 'm',
    os: 'o',
    lastSync: '',
    status: 's',
    complianceScore: 0,
    lockSecure: true,
    screenLockText: '',
    encSecure: true,
    encryptionText: '',
    osSecure: true,
    osText: '',
    intSecure: true,
    integrityText: '',
    ...overrides,
  };
}

function groupDetail(overrides: Partial<GroupOrgDetail>): GroupOrgDetail {
  return {
    name: 'G',
    adminId: '',
    risk: 'low',
    tags: [],
    totalMembers: 1,
    externalMembers: 0,
    externalAllowed: false,
    whoCanJoin: '',
    whoCanView: '',
    ...overrides,
  };
}

describe('NotificationService', () => {
  let service: NotificationService;
  let httpMock: HttpTestingController;

  let userServiceMock: { getUsersWithoutTwoFactor: ReturnType<typeof vi.fn> };
  let driveServiceMock: { getDrives: ReturnType<typeof vi.fn> };
  let deviceServiceMock: { getDevices: ReturnType<typeof vi.fn> };
  let groupServiceMock: { getOrgGroups: ReturnType<typeof vi.fn> };
  let oAuthServiceMock: { getApps: ReturnType<typeof vi.fn> };
  let passwordServiceMock: { getPasswordSettings: ReturnType<typeof vi.fn> };
  let translateMock: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    translateMock = vi.fn((key: string) => key);
    userServiceMock = { getUsersWithoutTwoFactor: vi.fn(() => of({ users: [] })) };
    driveServiceMock = { getDrives: vi.fn(() => of({ drives: [], nextPageToken: null })) };
    deviceServiceMock = {
      getDevices: vi.fn(() => of({ devices: [], nextPageToken: null })),
    };
    groupServiceMock = { getOrgGroups: vi.fn(() => of({ groups: [], nextPageToken: null })) };
    oAuthServiceMock = {
      getApps: vi.fn(() =>
        of({
          apps: [],
          nextPageToken: '',
          allFilteredApps: 0,
          allHighRiskApps: 0,
          allNotHighRiskApps: 0,
        }),
      ),
    };
    passwordServiceMock = {
      getPasswordSettings: vi.fn(() => of(emptyPasswordSettings)),
    };

    TestBed.configureTestingModule({
      providers: [
        NotificationService,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: TranslocoService, useValue: { translate: translateMock } },
        { provide: UserService, useValue: userServiceMock },
        { provide: DriveService, useValue: driveServiceMock },
        { provide: DeviceService, useValue: deviceServiceMock },
        { provide: GroupService, useValue: groupServiceMock },
        { provide: OAuthService, useValue: oAuthServiceMock },
        { provide: PasswordSettingsService, useValue: passwordServiceMock },
      ],
    });
    service = TestBed.inject(NotificationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('getNotificationsAndDismissed', () => {
    it('GETs notifications with credentials and maps response', () => {
      const active: Notification[] = [
        {
          id: '1',
          severity: 'info',
          title: 'T',
          description: 'D',
          notificationType: 'x',
          source: 's',
          sourceLabel: 'L',
          sourceRoute: '/',
        },
      ];
      const body: NotificationsResponse = { active, dismissed: [] };
      service.getNotificationsAndDismissed().subscribe((res) => expect(res).toEqual(body));

      const req = httpMock.expectOne((r) => r.url === '/api/notifications');
      expect(req.request.method).toBe('GET');
      expect(req.request.withCredentials).toBe(true);
      req.flush(body);
    });

    it('returns empty lists when HTTP fails', () => {
      service.getNotificationsAndDismissed().subscribe((res) => {
        expect(res.active).toEqual([]);
        expect(res.dismissed).toEqual([]);
      });

      const req = httpMock.expectOne((r) => r.url === '/api/notifications');
      req.flush('', { status: 500, statusText: 'Server Error' });
    });
  });

  describe('getNotificationDetails', () => {
    it('default type returns empty array', async () => {
      const n = baseNotification({ notificationType: 'unknown-type' });
      const lines = await firstValueFrom(service.getNotificationDetails(n));
      expect(lines).toEqual([]);
    });

    it('user-control maps users from UserService', async () => {
      userServiceMock.getUsersWithoutTwoFactor.mockReturnValue(
        of({ users: [{ fullName: 'Ada', email: 'ada@ex.com' }] }),
      );
      const n = baseNotification({ notificationType: 'user-control' });
      const lines = await firstValueFrom(service.getNotificationDetails(n));
      expect(lines).toEqual(['Ada (ada@ex.com)']);
      expect(userServiceMock.getUsersWithoutTwoFactor).toHaveBeenCalled();
    });

    it('user-control returns empty when UserService errors', async () => {
      userServiceMock.getUsersWithoutTwoFactor.mockReturnValue(throwError(() => new Error('x')));
      const n = baseNotification({ notificationType: 'user-control' });
      const lines = await firstValueFrom(service.getNotificationDetails(n));
      expect(lines).toEqual([]);
    });

    it('group-external lists only groups with external members and pluralizes label', async () => {
      groupServiceMock.getOrgGroups.mockReturnValue(
        of({
          groups: [
            groupDetail({ name: 'A', externalMembers: 0 }),
            groupDetail({ name: 'B', externalMembers: 2 }),
            groupDetail({ name: 'C', externalMembers: 1 }),
          ],
          nextPageToken: null,
        }),
      );
      translateMock.mockImplementation((k: string) => (k === 'external-members' ? 'ext-plural' : k));

      const n = baseNotification({ notificationType: 'group-external' });
      const lines = await firstValueFrom(service.getNotificationDetails(n));
      expect(lines).toEqual(['B (2 ext-plural)', 'C (1 external-member)']);
      expect(groupServiceMock.getOrgGroups).toHaveBeenCalledWith(undefined, undefined, 200);
    });

    it('group-external returns empty when GroupService errors', async () => {
      groupServiceMock.getOrgGroups.mockReturnValue(throwError(() => new Error('fail')));
      const n = baseNotification({ notificationType: 'group-external' });
      expect(await firstValueFrom(service.getNotificationDetails(n))).toEqual([]);
    });

    it('oauth-high-risk maps apps with singular and plural user labels', async () => {
      const apps: AggregatedAppDto[] = [
        {
          id: '1',
          name: 'App1',
          appType: 't',
          appSource: 's',
          isThirdParty: false,
          isAnonymous: false,
          isHighRisk: true,
          totalUsers: 1,
          exposurePercentage: 0,
          scopeCount: 0,
          dataAccess: [],
          highRiskCount: 0,
        },
        {
          id: '2',
          name: 'App2',
          appType: 't',
          appSource: 's',
          isThirdParty: false,
          isAnonymous: false,
          isHighRisk: true,
          totalUsers: 3,
          exposurePercentage: 0,
          scopeCount: 0,
          dataAccess: [],
          highRiskCount: 0,
        },
      ];
      oAuthServiceMock.getApps.mockReturnValue(
        of({
          apps,
          nextPageToken: '',
          allFilteredApps: 2,
          allHighRiskApps: 2,
          allNotHighRiskApps: 0,
        }),
      );

      const n = baseNotification({ notificationType: 'oauth-high-risk' });
      const lines = await firstValueFrom(service.getNotificationDetails(n));
      expect(lines).toEqual(['App1 (1 user)', 'App2 (3 users-no-cap)']);
      expect(oAuthServiceMock.getApps).toHaveBeenCalledWith(50, 'high');
    });

    it('oauth-high-risk returns empty when OAuthService errors', async () => {
      oAuthServiceMock.getApps.mockReturnValue(throwError(() => new Error('x')));
      const n = baseNotification({ notificationType: 'oauth-high-risk' });
      expect(await firstValueFrom(service.getNotificationDetails(n))).toEqual([]);
    });

    it('drive-orphan lists drives with no organizers', async () => {
      driveServiceMock.getDrives.mockReturnValue(
        of({
          drives: [
            shareDrive({ name: 'Orphan', totalOrganizers: 0 }),
            shareDrive({ name: 'Ok', totalOrganizers: 1 }),
          ],
          nextPageToken: null,
        }),
      );
      const n = baseNotification({ notificationType: 'drive-orphan' });
      expect(await firstValueFrom(service.getNotificationDetails(n))).toEqual(['Orphan']);
      expect(driveServiceMock.getDrives).toHaveBeenCalledWith(100);
    });

    it('drive-external formats external member counts', async () => {
      driveServiceMock.getDrives.mockReturnValue(
        of({
          drives: [
            shareDrive({ name: 'X', externalMembers: 2 }),
            shareDrive({ name: 'Y', externalMembers: 1 }),
            shareDrive({ name: 'Z', externalMembers: 0 }),
          ],
          nextPageToken: null,
        }),
      );
      translateMock.mockImplementation((k: string) => (k === 'external-members' ? 'M' : 'm'));

      const n = baseNotification({ notificationType: 'drive-external' });
      expect(await firstValueFrom(service.getNotificationDetails(n))).toEqual([
        'X (2 M)',
        'Y (1 m)',
      ]);
    });

    it('drive-outside-domain lists drives that allow outside-domain users', async () => {
      driveServiceMock.getDrives.mockReturnValue(
        of({
          drives: [
            shareDrive({ name: 'Open', onlyDomainUsersAllowed: false }),
            shareDrive({ name: 'Closed', onlyDomainUsersAllowed: true }),
          ],
          nextPageToken: null,
        }),
      );
      const n = baseNotification({ notificationType: 'drive-outside-domain' });
      expect(await firstValueFrom(service.getNotificationDetails(n))).toEqual(['Open']);
    });

    it('drive-non-member-access lists drives where non-members can access', async () => {
      driveServiceMock.getDrives.mockReturnValue(
        of({
          drives: [
            shareDrive({ name: 'Wide', onlyMembersCanAccess: false }),
            shareDrive({ name: 'Tight', id: '2', onlyMembersCanAccess: true }),
          ],
          nextPageToken: null,
        }),
      );
      const n = baseNotification({ notificationType: 'drive-non-member-access' });
      expect(await firstValueFrom(service.getNotificationDetails(n))).toEqual(['Wide']);
    });

    it('drive details return empty when DriveService errors', async () => {
      driveServiceMock.getDrives.mockReturnValue(throwError(() => new Error('e')));
      const n = baseNotification({ notificationType: 'drive-orphan' });
      expect(await firstValueFrom(service.getNotificationDetails(n))).toEqual([]);
    });

    it('device-lockscreen maps noncompliant devices and dedupes resourceId', async () => {
      deviceServiceMock.getDevices.mockReturnValue(
        of({
          devices: [
            makeDevice({ resourceId: 'a', deviceName: 'D1', lockSecure: false }),
            makeDevice({ resourceId: 'a', deviceName: 'Dup', lockSecure: false }),
            makeDevice({ resourceId: 'b', deviceName: 'D2', lockSecure: true }),
          ],
          nextPageToken: null,
        }),
      );
      const n = baseNotification({ notificationType: 'device-lockscreen' });
      const lines = await firstValueFrom(service.getNotificationDetails(n));
      expect(lines).toEqual(['D1 – User (u@x.com)']);
    });

    it('device details return empty when DeviceService errors', async () => {
      deviceServiceMock.getDevices.mockReturnValue(throwError(() => new Error('e')));
      const n = baseNotification({ notificationType: 'device-encryption' });
      expect(await firstValueFrom(service.getNotificationDetails(n))).toEqual([]);
    });

    it('password-2sv-not-enforced lists org units without enforcement', async () => {
      passwordServiceMock.getPasswordSettings.mockReturnValue(
        of({
          ...emptyPasswordSettings,
          twoStepVerification: {
            ...emptyPasswordSettings.twoStepVerification,
            byOrgUnit: [
              {
                orgUnitPath: '/',
                orgUnitName: 'Root',
                enforced: false,
                enrolledCount: 0,
                totalCount: 4,
              },
              {
                orgUnitPath: '/child',
                orgUnitName: 'Child',
                enforced: true,
                enrolledCount: 1,
                totalCount: 2,
              },
            ],
          },
        }),
      );
      const n = baseNotification({ notificationType: 'password-2sv-not-enforced' });
      const lines = await firstValueFrom(service.getNotificationDetails(n));
      expect(lines).toEqual(['Root (/) – 4 users-no-cap']);
    });

    it('password-weak-length uses character vs characters by minLength', async () => {
      passwordServiceMock.getPasswordSettings.mockReturnValue(
        of({
          ...emptyPasswordSettings,
          passwordPoliciesByOu: [
            {
              orgUnitPath: '/a',
              orgUnitName: 'OU1',
              userCount: 1,
              score: 0,
              problemCount: 0,
              minLength: 1,
              expirationDays: null,
              strongPasswordRequired: null,
              reusePreventionCount: null,
              inherited: false,
            },
            {
              orgUnitPath: '/b',
              orgUnitName: 'OU2',
              userCount: 1,
              score: 0,
              problemCount: 0,
              minLength: 8,
              expirationDays: null,
              strongPasswordRequired: null,
              reusePreventionCount: null,
              inherited: false,
            },
          ],
        }),
      );
      const n = baseNotification({ notificationType: 'password-weak-length' });
      const lines = await firstValueFrom(service.getNotificationDetails(n));
      expect(lines).toEqual(['OU1 (/a) – min. 1 character', 'OU2 (/b) – min. 8 characters']);
    });

    it('password-strong-not-required and password-never-expires map policies', async () => {
      passwordServiceMock.getPasswordSettings.mockReturnValue(
        of({
          ...emptyPasswordSettings,
          passwordPoliciesByOu: [
            {
              orgUnitPath: '/x',
              orgUnitName: 'Weak',
              userCount: 1,
              score: 0,
              problemCount: 0,
              minLength: 12,
              expirationDays: 90,
              strongPasswordRequired: false,
              reusePreventionCount: null,
              inherited: false,
            },
            {
              orgUnitPath: '/y',
              orgUnitName: 'NoExp',
              userCount: 1,
              score: 0,
              problemCount: 0,
              minLength: 12,
              expirationDays: 0,
              strongPasswordRequired: true,
              reusePreventionCount: null,
              inherited: false,
            },
          ],
        }),
      );
      const strong = await firstValueFrom(
        service.getNotificationDetails(baseNotification({ notificationType: 'password-strong-not-required' })),
      );
      const never = await firstValueFrom(
        service.getNotificationDetails(baseNotification({ notificationType: 'password-never-expires' })),
      );
      expect(strong).toEqual(['Weak (/x)']);
      expect(never).toEqual(['NoExp (/y)']);
    });

    it('password-admins-no-security-keys lists admins', async () => {
      passwordServiceMock.getPasswordSettings.mockReturnValue(
        of({
          ...emptyPasswordSettings,
          adminsWithoutSecurityKeys: [
            { id: '1', name: 'Admin', email: 'a@x.com', role: 'r', orgUnitPath: '/', twoFactorEnabled: true, numSecurityKeys: 0 },
          ],
        }),
      );
      const n = baseNotification({ notificationType: 'password-admins-no-security-keys' });
      expect(await firstValueFrom(service.getNotificationDetails(n))).toEqual(['Admin (a@x.com)']);
    });

    it('password details return empty when PasswordSettingsService errors', async () => {
      passwordServiceMock.getPasswordSettings.mockReturnValue(throwError(() => new Error('e')));
      const n = baseNotification({ notificationType: 'password-2sv-not-enforced' });
      expect(await firstValueFrom(service.getNotificationDetails(n))).toEqual([]);
    });
  });
});
