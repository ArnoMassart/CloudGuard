import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTransloco, TranslocoLoader, TranslocoService } from '@jsverse/transloco';
import { of, throwError } from 'rxjs';
import { AppAccess } from './app-access';
import { OAuthService } from '../../../services/o-auth-service';
import { SecurityScoreDetailService } from '../../../services/security-score-detail.service';
import { SecurityPreferencesFacade } from '../../../services/security-preferences-facade';
import { AggregatedAppDto } from '../../../models/o-auth/AggregatedAppDto';
import { OAuthOverviewResponse } from '../../../models/o-auth/OAuthOverviewResponse';

// Mock vertalingen
const I18N_MOCK: Record<string, string> = {
  'all-apps': 'Alle apps',
  'high-risk': 'Hoog risico',
  'no-risk': 'Geen risico',
};

class AppAccessTranslocoLoader implements TranslocoLoader {
  getTranslation() {
    return of(I18N_MOCK);
  }
}

describe('AppAccess', () => {
  let component: AppAccess;
  let fixture: ComponentFixture<AppAccess>;
  let translocoService: TranslocoService;

  // Mocks
  let oAuthServiceMock: {
    getApps: ReturnType<typeof vi.fn>;
    getOAuthPageOverview: ReturnType<typeof vi.fn>;
    refreshOAuthCache: ReturnType<typeof vi.fn>;
  };
  let securityScoreDetailMock: {
    createSimpleBreakdown: ReturnType<typeof vi.fn>;
    open: ReturnType<typeof vi.fn>;
  };
  let prefsFacadeMock: {
    isDisabled: ReturnType<typeof vi.fn>;
    loadWithPrefs$: ReturnType<typeof vi.fn>;
  };

  const mockApps: AggregatedAppDto[] = [
    { id: '1', name: 'App 1', isHighRisk: true } as AggregatedAppDto,
    { id: '2', name: 'App 2', isHighRisk: false } as AggregatedAppDto,
  ];

  const mockPageRes = {
    apps: mockApps,
    nextPageToken: 'token123',
    allFilteredApps: 2,
    allHighRiskApps: 1,
    allNotHighRiskApps: 1,
  };

  const mockOverviewRes: OAuthOverviewResponse = {
    totalThirdPartyApps: 10,
    totalHighRiskApps: 2,
    totalPermissionsGranted: 15,
    securityScore: 80,
    breakdown: undefined, // Gebruikt voor de SecurityScoreDetailService test
  } as unknown as OAuthOverviewResponse;

  beforeEach(async () => {
    oAuthServiceMock = {
      getApps: vi.fn(() => of(mockPageRes)),
      getOAuthPageOverview: vi.fn(() => of(mockOverviewRes)),
      refreshOAuthCache: vi.fn(() => of('OK')),
    };

    securityScoreDetailMock = {
      createSimpleBreakdown: vi.fn(() => ({ score: 80, factors: [] })),
      open: vi.fn(),
    };

    prefsFacadeMock = {
      isDisabled: vi.fn(() => false),
      // Geeft simpelweg de observable door (geen ingewikkelde pipe mock nodig)
      loadWithPrefs$: vi.fn((obs) => obs),
    };

    await TestBed.configureTestingModule({
      imports: [AppAccess],
      providers: [
        { provide: OAuthService, useValue: oAuthServiceMock },
        { provide: SecurityScoreDetailService, useValue: securityScoreDetailMock },
        { provide: SecurityPreferencesFacade, useValue: prefsFacadeMock },
        provideTransloco({
          config: {
            availableLangs: ['en', 'nl'],
            defaultLang: 'en',
            reRenderOnLangChange: true,
          },
          loader: AppAccessTranslocoLoader,
        }),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AppAccess);
    component = fixture.componentInstance;
    translocoService = TestBed.inject(TranslocoService);

    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('loads overview and apps on init via language change', () => {
    expect(prefsFacadeMock.loadWithPrefs$).toHaveBeenCalled();
    expect(oAuthServiceMock.getApps).toHaveBeenCalledWith(3, 'all', undefined, '');

    expect(component.pageOverview()).toEqual(mockOverviewRes);
    expect(component.apps()).toEqual(mockApps);
    expect(component.nextPageToken()).toBe('token123');
    expect(component.allFilteredApps()).toBe(2);
    expect(component.allHighRiskApps()).toBe(1);
    expect(component.allNotHighRiskApps()).toBe(1);
  });

  it('computes filter options correctly based on preferences', () => {
    const filters = component.filterOptions();
    expect(filters.length).toBe(3);

    // Check de activeClasses
    expect(filters.find((f) => f.value === 'all')?.activeClass).toContain('bg-[#3ABFAD]');
    // Omdat highRiskAlertsEnabled (via isDisabled mock = false) true is:
    expect(filters.find((f) => f.value === 'high')?.activeClass).toContain('bg-red-100');
  });

  it('toggleExpanded flips the state', () => {
    const initialState = component.isExpanded();
    component.toggleExpanded();
    expect(component.isExpanded()).toBe(!initialState);
  });

  it('toggleExpand sets and unsets expanded app ID', () => {
    component.toggleExpand('app_1');
    expect(component.expandedApp()).toBe('app_1');

    component.toggleExpand('app_1');
    expect(component.expandedApp()).toBeNull();

    component.toggleExpand('app_2');
    expect(component.expandedApp()).toBe('app_2');
  });

  it('onSearch updates query and triggers reload', () => {
    oAuthServiceMock.getApps.mockClear();

    component.onSearch('test query');

    expect(component.searchQuery()).toBe('test query');
    expect(oAuthServiceMock.getApps).toHaveBeenCalledWith(3, 'all', undefined, 'test query');
  });

  it('setRiskFilter updates risk and triggers reload', () => {
    oAuthServiceMock.getApps.mockClear();

    component.setRiskFilter('high');

    expect(component.riskFilter()).toBe('high');
    expect(oAuthServiceMock.getApps).toHaveBeenCalledWith(3, 'high', undefined, '');
  });

  describe('refreshData', () => {
    it('calls refresh endpoint and reloads data', () => {
      oAuthServiceMock.getApps.mockClear();
      prefsFacadeMock.loadWithPrefs$.mockClear();

      component.refreshData();

      expect(oAuthServiceMock.refreshOAuthCache).toHaveBeenCalled();
      expect(oAuthServiceMock.getApps).toHaveBeenCalled();
      expect(prefsFacadeMock.loadWithPrefs$).toHaveBeenCalled();
      expect(component.isRefreshing()).toBe(false); // op false gezet in de complete()
    });

    it('does nothing if already refreshing', () => {
      component.isRefreshing.set(true);
      component.refreshData();
      expect(oAuthServiceMock.refreshOAuthCache).not.toHaveBeenCalled();
    });

    it('handles refresh error gracefully', () => {
      const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
      oAuthServiceMock.refreshOAuthCache.mockReturnValue(
        throwError(() => new Error('Refresh failed'))
      );

      component.refreshData();

      expect(consoleSpy).toHaveBeenCalledWith('Kon cache niet vernieuwen:', expect.any(Error));
      expect(component.isRefreshing()).toBe(false);

      consoleSpy.mockRestore();
    });
  });

  describe('openSecurityScoreDetail', () => {
    it('opens detail with simple breakdown if none exists in overview', () => {
      component.openSecurityScoreDetail();

      expect(securityScoreDetailMock.createSimpleBreakdown).toHaveBeenCalledWith(80, 'app-access');
      expect(securityScoreDetailMock.open).toHaveBeenCalledWith(
        { score: 80, factors: [] },
        'app-access'
      );
    });

    it('opens detail directly with overview breakdown if it exists', () => {
      const existingBreakdown = { score: 90, factors: [{ title: 'test', score: 90 }] };
      component.pageOverview.set({
        ...mockOverviewRes,
        securityScoreBreakdown: existingBreakdown,
      } as any);

      component.openSecurityScoreDetail();

      expect(securityScoreDetailMock.createSimpleBreakdown).not.toHaveBeenCalled();
      expect(securityScoreDetailMock.open).toHaveBeenCalledWith(existingBreakdown, 'app-access');
    });
  });

  describe('loadApps errors', () => {
    it('sets apiError and isLoading correctly on failure', async () => {
      const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
      oAuthServiceMock.getApps.mockReturnValue(throwError(() => new Error('Load apps failed')));

      // Trigger herladen via taal
      translocoService.setActiveLang('nl');
      await fixture.whenStable();

      expect(component.apiError()).toBe(true);
      expect(component.isLoading()).toBe(false);
      expect(consoleSpy).toHaveBeenCalledWith('Failed to load oAuth apps', expect.any(Error));

      consoleSpy.mockRestore();
    });
  });

  it('cleans up language subscription on destroy', () => {
    component.ngOnDestroy();
    oAuthServiceMock.getApps.mockClear();

    translocoService.setActiveLang('nl');

    expect(oAuthServiceMock.getApps).not.toHaveBeenCalled();
  });
});
