import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { TranslocoService } from '@jsverse/transloco';
import { firstValueFrom, of } from 'rxjs';
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

describe('NotificationService', () => {
  let service: NotificationService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        NotificationService,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: TranslocoService, useValue: { translate: (key: string) => key } },
        { provide: UserService, useValue: { getUsersWithoutTwoFactor: () => of({ users: [] }) } },
        { provide: DriveService, useValue: { getDrives: () => of({ drives: [], nextPageToken: null }) } },
        {
          provide: DeviceService,
          useValue: { getDevices: () => of({ devices: [], nextPageToken: null }) },
        },
        { provide: GroupService, useValue: { getOrgGroups: () => of({ groups: [], nextPageToken: null }) } },
        {
          provide: OAuthService,
          useValue: {
            getApps: () =>
              of({
                apps: [],
                nextPageToken: '',
                allFilteredApps: 0,
                allHighRiskApps: 0,
                allNotHighRiskApps: 0,
              }),
          },
        },
        { provide: PasswordSettingsService, useValue: { getPasswordSettings: () => of(emptyPasswordSettings) } },
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
      const n: Notification = {
        id: '1',
        severity: 'info',
        title: 'T',
        description: 'D',
        notificationType: 'unknown-type',
        source: 's',
        sourceLabel: 'L',
        sourceRoute: '/',
      };
      const lines = await firstValueFrom(service.getNotificationDetails(n));
      expect(lines).toEqual([]);
    });
  });
});
