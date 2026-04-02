import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTransloco, TranslocoLoader, TranslocoService } from '@jsverse/transloco';
import { of, throwError } from 'rxjs';
import { UsersSection } from './users-section';
import { UserService } from '../../../../services/user-service';
import { SecurityScoreDetailService } from '../../../../services/security-score-detail.service';
import { SecurityPreferencesFacade } from '../../../../services/security-preferences-facade';
import { USER_SECURITY_VIOLATION, UserOrgDetail } from '../../../../models/users/UserOrgDetails';
import { UserOverviewResponse } from '../../../../models/users/UserOverviewResponse';

// Mock vertalingen
const I18N_MOCK: Record<string, string> = {
  'users.title': 'Gebruikers',
};

class UsersTranslocoLoader implements TranslocoLoader {
  getTranslation() {
    return of(I18N_MOCK);
  }
}

describe('UsersSection', () => {
  let component: UsersSection;
  let fixture: ComponentFixture<UsersSection>;
  let translocoService: TranslocoService;

  // Mocks
  let userServiceMock: {
    getOrgUsers: ReturnType<typeof vi.fn>;
    getUsersPageOverview: ReturnType<typeof vi.fn>;
    refreshUsersCache: ReturnType<typeof vi.fn>;
  };
  let securityScoreDetailMock: {
    createSimpleBreakdown: ReturnType<typeof vi.fn>;
    open: ReturnType<typeof vi.fn>;
  };
  let prefsFacadeMock: {
    isDisabled: ReturnType<typeof vi.fn>;
    loadWithPrefs$: ReturnType<typeof vi.fn>;
  };

  const mockUsers: UserOrgDetail[] = [
    {
      email: 'test@cloudmen.com',
      twoFactorEnabled: true,
      securityConform: true,
      securityViolationCodes: [],
      roles: ['Super Admin'],
    } as unknown as UserOrgDetail,
    {
      email: 'risk@cloudmen.com',
      twoFactorEnabled: false,
      securityConform: false,
      securityViolationCodes: [USER_SECURITY_VIOLATION.NO_2FA],
      roles: ['Regular User'],
    } as unknown as UserOrgDetail,
  ];

  const mockPageRes = {
    users: mockUsers,
    nextPageToken: 'token_123',
  };

  const mockOverviewRes: UserOverviewResponse = {
    totalUsers: 50,
    withoutTwoFactor: 5,
    securityScore: 85,
    warnings: {
      hasWarnings: true,
      hasMultipleWarnings: false,
      items: { twoFactorWarning: true },
    },
  } as unknown as UserOverviewResponse;

  beforeEach(async () => {
    userServiceMock = {
      getOrgUsers: vi.fn(() => of(mockPageRes)),
      getUsersPageOverview: vi.fn(() => of(mockOverviewRes)),
      refreshUsersCache: vi.fn(() => of('OK')),
    };

    securityScoreDetailMock = {
      createSimpleBreakdown: vi.fn(() => ({ score: 85, factors: [] })),
      open: vi.fn(),
    };

    prefsFacadeMock = {
      isDisabled: vi.fn(() => false),
      loadWithPrefs$: vi.fn((obs) => obs),
    };

    await TestBed.configureTestingModule({
      imports: [UsersSection],
      providers: [
        { provide: UserService, useValue: userServiceMock },
        { provide: SecurityScoreDetailService, useValue: securityScoreDetailMock },
        { provide: SecurityPreferencesFacade, useValue: prefsFacadeMock },
        provideTransloco({
          config: {
            availableLangs: ['en', 'nl'],
            defaultLang: 'en',
            reRenderOnLangChange: true,
          },
          loader: UsersTranslocoLoader,
        }),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(UsersSection);
    component = fixture.componentInstance;
    translocoService = TestBed.inject(TranslocoService);

    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('loads overview and users on init', () => {
    expect(userServiceMock.getOrgUsers).toHaveBeenCalled();
    expect(component.orgUsers()).toEqual(mockUsers);
    expect(component.pageOverview()).toEqual(mockOverviewRes);
    expect(component.hasWarnings()).toBe(true);
  });

  it('getRoleClass returns correct Tailwind classes', () => {
    expect(component.getRoleClass('Super Admin')).toContain('bg-primary');
    expect(component.getRoleClass('Regular User')).toContain('bg-blue-100');
    expect(component.getRoleClass('Unknown')).toContain('bg-gray-100');
  });

  describe('Muting and Conform logic', () => {
    it('twoFactorCellMuted returns true only if 2fa is disabled and preference is muted', () => {
      const userNo2fa = { ...mockUsers[1], twoFactorEnabled: false } as UserOrgDetail;

      // Niet gemuted in prefs
      prefsFacadeMock.isDisabled.mockReturnValue(false);
      expect(component.twoFactorCellMuted(userNo2fa)).toBe(false);

      // Wel gemuted in prefs
      prefsFacadeMock.isDisabled.mockReturnValue(true);
      expect(component.twoFactorCellMuted(userNo2fa)).toBe(true);
    });

    it('effectiveSecurityConform returns true if violations are muted in preferences', () => {
      const userWithViolations = {
        securityConform: false,
        securityViolationCodes: [
          USER_SECURITY_VIOLATION.NO_2FA,
          USER_SECURITY_VIOLATION.ACTIVITY_STALE,
        ],
      } as UserOrgDetail;

      // Alles gemuted
      prefsFacadeMock.isDisabled.mockImplementation((cat, key) => true);
      expect(component.effectiveSecurityConform(userWithViolations)).toBe(true);

      // Slechts één gemuted
      prefsFacadeMock.isDisabled.mockImplementation((cat, key) => key === '2fa');
      expect(component.effectiveSecurityConform(userWithViolations)).toBe(false);
    });
  });

  it('onSearch resets pagination and reloads users', () => {
    userServiceMock.getOrgUsers.mockClear();
    component.onSearch('John');

    expect(component.searchQuery()).toBe('John');
    expect(userServiceMock.getOrgUsers).toHaveBeenCalledWith(4, undefined, 'John');
  });

  describe('refreshData', () => {
    it('triggers cache refresh and reloads data', () => {
      userServiceMock.getOrgUsers.mockClear();
      component.refreshData();

      expect(userServiceMock.refreshUsersCache).toHaveBeenCalled();
      expect(userServiceMock.getOrgUsers).toHaveBeenCalled();
      expect(component.isRefreshing()).toBe(false);
    });

    it('handles refresh errors gracefully', () => {
      const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
      userServiceMock.refreshUsersCache.mockReturnValue(
        throwError(() => new Error('Refresh failed'))
      );

      component.refreshData();

      expect(consoleSpy).toHaveBeenCalledWith('Kon cache niet vernieuwen:', expect.any(Error));
      expect(component.isRefreshing()).toBe(false);
      consoleSpy.mockRestore();
    });
  });

  it('openSecurityScoreDetail opens detail service', () => {
    component.openSecurityScoreDetail();
    expect(securityScoreDetailMock.open).toHaveBeenCalled();
  });

  it('handles loadUsers API error', async () => {
    const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
    userServiceMock.getOrgUsers.mockReturnValue(throwError(() => new Error('API Error')));

    translocoService.setActiveLang('nl'); // Trigger herladen via ngOnInit subscription
    await fixture.whenStable();

    expect(component.apiError()).toBe(true);
    expect(component.isLoading()).toBe(false);
    consoleSpy.mockRestore();
  });

  it('unsubscribes on destroy', () => {
    const unsubscribeSpy = vi.spyOn((component as any).langSubscription, 'unsubscribe');
    component.ngOnDestroy();
    expect(unsubscribeSpy).toHaveBeenCalled();
  });
});
