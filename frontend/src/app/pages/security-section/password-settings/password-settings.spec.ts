import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTransloco, TranslocoLoader } from '@jsverse/transloco';
import { of, throwError } from 'rxjs';
import { PasswordSettings as PasswordSettingsData } from '../../../models/password/PasswordSettings';
import { PasswordSettingsService } from '../../../services/password-settings-service';
import { SecurityPreferencesFacade } from '../../../services/security-preferences-facade';
import { SecurityScoreDetailService } from '../../../services/security-score-detail.service';
import { UtilityMethods } from '../../../shared/UtilityMethods';
import { PasswordSettings } from './password-settings';

const PW_SETTINGS_I18N: Record<string, string> = {
  'password-settings': 'Password settings',
  'password-settings.description': 'Desc',
  'password-settings.loading': 'Loading',
  'try-again': 'Retry',
  'password-settings.number-of-orgs': 'Orgs',
  'critical-warning-caps': 'Critical',
  'password-settings.warning': 'Warn',
  'password-settings.warning.without-keys': 'No keys',
  'password-settings.warning.weak-length': 'Weak',
  'password-settings.warning.strong-not-required': 'Strong',
  'password-settings.warning.never-expires': 'Expires',
  refreshing: 'Refreshing',
  'renew-data': 'Refresh',
  'password-settings.admin-without-key': 'Admin key',
  'user-cap': 'User',
  role: 'Role',
  'password-settings.admins-have-keys': 'All have keys',
  'name-caps': 'Name',
  'reason-caps': 'Reason',
  'password-settings.required-next-login': 'Next login',
  unknown: 'Unknown',
  'password-settings.no-users-password-changes': 'None',
  'password-settings.password-org': 'Policy',
  'password-settings.org-detailed-settings': 'Detail',
  'users-no-cap': 'users',
  missing: 'Missing',
  'good-configured': 'OK',
  'minimal-length': 'Min',
  characters: 'chars',
  'password-cycle': 'Cycle',
  days: 'days',
  'password-settings.strong-password': 'Strong pw',
  'password-history': 'History',
  'previous-no-caps': 'prev',
  'not-active': 'Off',
  enforce: 'Enforce',
  'password-settings.no-2FA-data': 'No 2FA',
  'password-settings.inherited-from': 'Inherited',
  'active-2': 'On',
  configured: 'Yes',
  'not-configured': 'No',
  'to-admin-console': 'Admin',
  'password-settings.head-organisation': 'Head',
  'security-score.password-settings': 'Password settings score',
};

class PasswordSettingsTranslocoLoader implements TranslocoLoader {
  getTranslation() {
    return of(PW_SETTINGS_I18N);
  }
}

function buildSettings(overrides?: Partial<PasswordSettingsData>): PasswordSettingsData {
  const base: PasswordSettingsData = {
    passwordPoliciesByOu: [
      {
        orgUnitPath: '/org/a',
        orgUnitName: 'Unit A',
        userCount: 3,
        score: 80,
        problemCount: 0,
        minLength: 12,
        expirationDays: 90,
        strongPasswordRequired: true,
        reusePreventionCount: 5,
        inherited: false,
      },
    ],
    twoStepVerification: {
      byOrgUnit: [
        {
          orgUnitPath: '/org/a',
          orgUnitName: 'Unit A',
          enforced: true,
          enrolledCount: 1,
          totalCount: 3,
        },
      ],
      totalEnrolled: 1,
      totalEnforced: 1,
      totalUsers: 3,
    },
    usersWithForcedChange: [],
    summary: {
      usersWithForcedChange: 0,
      usersWith2SvEnrolled: 1,
      usersWith2SvEnforced: 1,
      totalUsers: 3,
    },
    adminsWithoutSecurityKeys: [],
    securityScore: 88,
  };
  return { ...base, ...overrides };
}

describe('PasswordSettings', () => {
  let component: PasswordSettings;
  let fixture: ComponentFixture<PasswordSettings>;
  let serviceMock: {
    getPasswordSettings: ReturnType<typeof vi.fn>;
    refreshCache: ReturnType<typeof vi.fn>;
  };
  let preferencesMock: {
    loadWithPrefs$: <T>(data$: import('rxjs').Observable<T>) => import('rxjs').Observable<T>;
    isDisabled: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    const settings = buildSettings();
    serviceMock = {
      getPasswordSettings: vi.fn(() => of(settings)),
      refreshCache: vi.fn(() => of('')),
    };
    preferencesMock = {
      loadWithPrefs$: <T>(data$: import('rxjs').Observable<T>) => data$,
      isDisabled: vi.fn(() => false),
    };

    await TestBed.configureTestingModule({
      imports: [PasswordSettings],
      providers: [
        { provide: PasswordSettingsService, useValue: serviceMock },
        { provide: SecurityPreferencesFacade, useValue: preferencesMock },
        {
          provide: SecurityScoreDetailService,
          useValue: {
            open: vi.fn(),
            createSimpleBreakdown: vi.fn((score: number, subtitle: string) => ({
              totalScore: score,
              status: 'good',
              factors: [
                {
                  title: 't',
                  description: 'd',
                  score,
                  maxScore: 100,
                  severity: 'success' as const,
                  muted: false,
                },
              ],
            })),
          },
        },
        provideTransloco({
          config: {
            availableLangs: ['en'],
            defaultLang: 'en',
            reRenderOnLangChange: true,
          },
          loader: PasswordSettingsTranslocoLoader,
        }),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PasswordSettings);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('loads settings after init', () => {
    expect(serviceMock.getPasswordSettings).toHaveBeenCalled();
    expect(component.loading()).toBe(false);
    expect(component.data()?.securityScore).toBe(88);
    expect(component.error()).toBe(null);
  });

  it('sets error when load fails', async () => {
    serviceMock.getPasswordSettings.mockReturnValue(throwError(() => new Error('boom')));
    fixture = TestBed.createComponent(PasswordSettings);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();

    expect(component.error()).toContain('boom');
    expect(component.loading()).toBe(false);
  });

  it('sets fallback error when load error has no message', async () => {
    serviceMock.getPasswordSettings.mockReturnValue(throwError(() => ({})));
    fixture = TestBed.createComponent(PasswordSettings);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();

    expect(component.error()).toBe('Kon gegevens niet laden.');
    expect(component.loading()).toBe(false);
  });

  it('retry triggers load again', () => {
    serviceMock.getPasswordSettings.mockClear();
    component.retry();
    expect(serviceMock.getPasswordSettings).toHaveBeenCalled();
  });

  it('toggleWarnings toggles warningsExpanded', () => {
    const v = component.warningsExpanded();
    component.toggleWarnings();
    expect(component.warningsExpanded()).toBe(!v);
  });

  it('toggleCriticalWarnings toggles criticalWarningsExpanded', () => {
    const v = component.criticalWarningsExpanded();
    component.toggleCriticalWarnings();
    expect(component.criticalWarningsExpanded()).toBe(!v);
  });

  it('toggleSecurityKeys toggles securityKeysExpanded', () => {
    const v = component.securityKeysExpanded();
    component.toggleSecurityKeys();
    expect(component.securityKeysExpanded()).toBe(!v);
  });

  it('toggleForcedChange toggles forcedChangeExpanded', () => {
    const v = component.forcedChangeExpanded();
    component.toggleForcedChange();
    expect(component.forcedChangeExpanded()).toBe(!v);
  });

  it('toggleOu expands and collapses org unit path', () => {
    component.toggleOu('/org/a');
    expect(component.expandedOu()).toBe('/org/a');
    component.toggleOu('/org/a');
    expect(component.expandedOu()).toBe(null);
  });

  it('get2SvForOu returns matching OU', () => {
    expect(component.get2SvForOu('/org/a')?.enforced).toBe(true);
    expect(component.get2SvForOu('/missing')).toBeUndefined();
  });

  it('hasPolicyData detects populated policy fields', () => {
    const p = component.data()!.passwordPoliciesByOu[0];
    expect(component.hasPolicyData(p)).toBe(true);
    expect(
      component.hasPolicyData({
        ...p,
        minLength: null,
        expirationDays: null,
        strongPasswordRequired: null,
        reusePreventionCount: null,
      }),
    ).toBe(false);
  });

  it('openAdminPasswordSettings opens admin security password URL', () => {
    const spy = vi.spyOn(UtilityMethods, 'openAdminPage').mockImplementation(() => {});
    component.openAdminPasswordSettings();
    expect(spy).toHaveBeenCalledWith('https://admin.google.com/ac/security/password');
    spy.mockRestore();
  });

  it('getAdminUserUrl encodes id or falls back', () => {
    expect(
      component.getAdminUserUrl({
        id: '  ',
        name: 'n',
        email: 'e',
        role: 'r',
        orgUnitPath: '/',
        twoFactorEnabled: true,
        numSecurityKeys: 0,
      }),
    ).toBe('https://admin.google.com/u/1/ac/users');
    expect(
      component.getAdminUserUrl({
        id: 'id/x',
        name: 'n',
        email: 'e',
        role: 'r',
        orgUnitPath: '/',
        twoFactorEnabled: true,
        numSecurityKeys: 0,
      }),
    ).toBe('https://admin.google.com/u/1/ac/users/id%2Fx');
  });

  it('getUserUrl encodes email or falls back', () => {
    expect(component.getUserUrl('')).toBe('https://admin.google.com/u/1/ac/users');
    expect(component.getUserUrl('  a@b.com  ')).toBe(
      'https://admin.google.com/u/1/ac/users/a%40b.com',
    );
  });

  it('formatOrgUnit maps root and trims leading slash', () => {
    expect(component.formatOrgUnit('/')).toBe('password-settings.head-organisation');
    expect(component.formatOrgUnit('/foo/bar')).toBe('foo/bar');
    expect(component.formatOrgUnit('no-slash')).toBe('no-slash');
  });

  it('formatForcedChangeReason maps known reason', () => {
    expect(component.formatForcedChangeReason('changePasswordAtNextLogin')).toContain('login');
    expect(component.formatForcedChangeReason('other')).toBe('other');
    expect(component.formatForcedChangeReason('')).toContain('Onbekend');
  });

  it('openSecurityScoreDetail passes breakdown from data when present', async () => {
    serviceMock.getPasswordSettings.mockReturnValue(
      of(
        buildSettings({
          securityScoreBreakdown: {
            totalScore: 88,
            status: 'good',
            factors: [],
          },
        }),
      ),
    );
    fixture = TestBed.createComponent(PasswordSettings);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();

    const detail = TestBed.inject(SecurityScoreDetailService);
    component.openSecurityScoreDetail();
    expect(detail.open).toHaveBeenCalledWith(
      { totalScore: 88, status: 'good', factors: [] },
      'security-score.password-settings',
    );
  });

  it('refreshData refreshes cache then reloads without full loading screen', () => {
    serviceMock.getPasswordSettings.mockClear();
    serviceMock.refreshCache.mockClear();
    component.refreshData();
    expect(serviceMock.refreshCache).toHaveBeenCalled();
    expect(serviceMock.getPasswordSettings).toHaveBeenCalled();
    expect(component.isRefreshing()).toBe(false);
  });

  it('refreshData clears refreshing flag when refresh fails', () => {
    const errSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
    serviceMock.refreshCache.mockReturnValueOnce(throwError(() => new Error('cache')));
    component.refreshData();
    expect(component.isRefreshing()).toBe(false);
    errSpy.mockRestore();
  });

  it('effectivePolicyProblemCount sums visible issues', () => {
    const policy = {
      orgUnitPath: '/x',
      orgUnitName: 'X',
      userCount: 1,
      score: 0,
      problemCount: 0,
      minLength: 8,
      expirationDays: 0,
      strongPasswordRequired: false,
      reusePreventionCount: 0,
      inherited: false,
    };
    expect(component.effectivePolicyProblemCount(policy)).toBe(4);
  });

  it('effectivePolicyProblemCount skips length when length preference is muted', () => {
    preferencesMock.isDisabled.mockImplementation((_s, key: string) => key === 'length');
    const policy = {
      orgUnitPath: '/x',
      orgUnitName: 'X',
      userCount: 1,
      score: 0,
      problemCount: 0,
      minLength: 8,
      expirationDays: 0,
      strongPasswordRequired: false,
      reusePreventionCount: 0,
      inherited: false,
    };
    expect(component.effectivePolicyProblemCount(policy)).toBe(3);
  });

  it('hasPasswordLengthWeak when min length under 12 and preference active', () => {
    const p = component.data()!.passwordPoliciesByOu[0];
    component.data.set(
      buildSettings({
        passwordPoliciesByOu: [{ ...p, minLength: 8 }],
      }),
    );
    expect(component.hasPasswordLengthWeak()).toBe(true);
  });

  it('has2SvNotEnforced and hasCriticalWarnings when an OU is not enforced', () => {
    component.data.set(
      buildSettings({
        twoStepVerification: {
          byOrgUnit: [
            {
              orgUnitPath: '/org/a',
              orgUnitName: 'Unit A',
              enforced: false,
              enrolledCount: 0,
              totalCount: 3,
            },
          ],
          totalEnrolled: 0,
          totalEnforced: 0,
          totalUsers: 3,
        },
      }),
    );
    expect(component.has2SvNotEnforced()).toBe(true);
    expect(component.hasCriticalWarnings()).toBe(true);
  });

  it('hasAdminsWithoutSecurityKeys when list non-empty and preference active', () => {
    component.data.set(
      buildSettings({
        adminsWithoutSecurityKeys: [
          {
            id: 'a1',
            name: 'Admin',
            email: 'a@b.com',
            role: 'ADMIN',
            orgUnitPath: '/',
            twoFactorEnabled: true,
            numSecurityKeys: 0,
          },
        ],
      }),
    );
    expect(component.hasAdminsWithoutSecurityKeys()).toBe(true);
  });

  it('hasMultipleWarnings when more than one warning type applies', () => {
    const p = component.data()!.passwordPoliciesByOu[0];
    component.data.set(
      buildSettings({
        passwordPoliciesByOu: [{ ...p, minLength: 8 }],
        adminsWithoutSecurityKeys: [
          {
            id: 'a1',
            name: 'Admin',
            email: 'a@b.com',
            role: 'ADMIN',
            orgUnitPath: '/',
            twoFactorEnabled: true,
            numSecurityKeys: 0,
          },
        ],
      }),
    );
    expect(component.hasWarnings()).toBe(true);
    expect(component.hasMultipleWarnings()).toBe(true);
  });

  it('hasWarnings is false when muted preferences hide all issue types', () => {
    preferencesMock.isDisabled.mockReturnValue(true);
    const p = component.data()!.passwordPoliciesByOu[0];
    component.data.set(
      buildSettings({
        passwordPoliciesByOu: [{ ...p, minLength: 8, strongPasswordRequired: false }],
        adminsWithoutSecurityKeys: [
          {
            id: 'a1',
            name: 'Admin',
            email: 'a@b.com',
            role: 'ADMIN',
            orgUnitPath: '/',
            twoFactorEnabled: true,
            numSecurityKeys: 0,
          },
        ],
      }),
    );
    expect(component.hasAdminsWithoutSecurityKeys()).toBe(false);
    expect(component.hasPasswordLengthWeak()).toBe(false);
    expect(component.hasStrongPasswordNotRequired()).toBe(false);
  });

  it('isPasswordPrefDisabled delegates to facade', async () => {
    preferencesMock.isDisabled.mockImplementation((_s: string, key: string) => key === 'length');
    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [PasswordSettings],
      providers: [
        { provide: PasswordSettingsService, useValue: serviceMock },
        { provide: SecurityPreferencesFacade, useValue: preferencesMock },
        {
          provide: SecurityScoreDetailService,
          useValue: { open: vi.fn(), createSimpleBreakdown: vi.fn() },
        },
        provideTransloco({
          config: {
            availableLangs: ['en'],
            defaultLang: 'en',
            reRenderOnLangChange: true,
          },
          loader: PasswordSettingsTranslocoLoader,
        }),
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(PasswordSettings);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.isPasswordPrefDisabled('length')).toBe(true);
    expect(preferencesMock.isDisabled).toHaveBeenCalledWith('password-settings', 'length');
  });
});
