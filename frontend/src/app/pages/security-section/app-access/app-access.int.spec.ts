import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideTransloco, TranslocoLoader } from '@jsverse/transloco';
import { By } from '@angular/platform-browser';
import { of } from 'rxjs';
import { AppAccess } from './app-access';
import { OAuthService } from '../../../services/o-auth-service';
import { SecurityPreferencesFacade } from '../../../services/security-preferences-facade';
import { SecurityScoreDetailService } from '../../../services/security-score-detail.service';
import { provideTranslocoTesting } from '../../../testing/transloco-testing';

// Mock Data
const MOCK_OVERVIEW = { totalThirdPartyApps: 10, totalHighRiskApps: 2, securityScore: 85 };
const MOCK_APPS = {
  apps: [{ id: 1, name: 'Test App', isHighRisk: true, appType: 'Web', dataAccess: [] }],
  nextPageToken: 'next-123',
  allFilteredApps: 10,
  allHighRiskApps: 2,
  allNotHighRiskApps: 8,
};

describe('AppAccess Integration', () => {
  let component: AppAccess;
  let fixture: ComponentFixture<AppAccess>;
  let httpTesting: HttpTestingController;
  let preferencesFacade: any;
  let securityScoreService: any;

  beforeEach(async () => {
    // Mock de Facade en de Dialog Service
    preferencesFacade = {
      loadWithPrefs$: vi.fn((obs) => obs), // Geeft de observable gewoon door
      isDisabled: vi.fn().mockReturnValue(false),
    };

    securityScoreService = {
      open: vi.fn(),
      createSimpleBreakdown: vi
        .fn()
        .mockReturnValue({ totalScore: 85, status: 'good', factors: [] }),
    };

    await TestBed.configureTestingModule({
      imports: [AppAccess],
      providers: [
        OAuthService,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: SecurityPreferencesFacade, useValue: preferencesFacade },
        { provide: SecurityScoreDetailService, useValue: securityScoreService },
        provideTranslocoTesting(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AppAccess);
    component = fixture.componentInstance;
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  const flushInitialData = () => {
    fixture.detectChanges(); // Trigger ngOnInit
    httpTesting.expectOne((r) => r.url.endsWith('/google/oAuth/overview')).flush(MOCK_OVERVIEW);
    httpTesting.expectOne((r) => r.url.endsWith('/google/oAuth')).flush(MOCK_APPS);
    fixture.detectChanges();
  };

  it('should load overview and apps on initialization', () => {
    flushInitialData();

    expect(component.apps().length).toBe(1);
    expect(component.pageOverview()?.totalThirdPartyApps).toBe(10);

    const appTitle = fixture.debugElement.query(By.css('.text-slate-900.text-base')).nativeElement;
    expect(appTitle.textContent).toContain('Test App');
  });

  describe('Filtering and Searching', () => {
    it('should trigger a new API call when risk filter changes', () => {
      flushInitialData();

      // Verander filter naar 'high'
      component.setRiskFilter('high');
      fixture.detectChanges();

      // Verwacht een nieuwe call met de 'high' parameter
      const req = httpTesting.expectOne((r) => r.url.endsWith('/google/oAuth'));
      expect(req.request.params.get('risk')).toBe('high');
      req.flush(MOCK_APPS);
    });

    it('should reset pagination when searching', () => {
      flushInitialData();
      const paginationResetSpy = vi.spyOn(component.pagination()!, 'reset');

      component.onSearch('new search');
      fixture.detectChanges();

      const req = httpTesting.expectOne((r) => r.url.endsWith('/google/oAuth'));
      expect(req.request.params.get('query')).toBe('new search');
      expect(paginationResetSpy).toHaveBeenCalled();
      req.flush(MOCK_APPS);
    });
  });

  describe('Preferences Integration (Muting Alerts)', () => {
    it('should apply "muted" styles when high risk alerts are disabled in preferences', () => {
      // Zet preferences op disabled
      preferencesFacade.isDisabled.mockReturnValue(true);
      flushInitialData();

      const highRiskCard = fixture.debugElement.queryAll(By.css('app-section-top-card'))[1];
      // De kleuren zouden nu uit de 'muted' sectie van KpiColors moeten komen
      // (Dit testen we indirect door te kijken of de input signal reageert)
      expect(component.highRiskAlertsEnabled()).toBe(false);

      const appIconContainer = fixture.debugElement.query(By.css('.h-12.w-12'));
      expect(appIconContainer.nativeElement.classList).toContain('text-[#6b7280]'); // Muted grey
    });
  });

  describe('Data Refreshing', () => {
    it('should show refreshing state and reload data on refreshData()', () => {
      flushInitialData();

      component.refreshData();
      expect(component.isRefreshing()).toBe(true);

      const refreshReq = httpTesting.expectOne((r) => r.url.endsWith('/google/oAuth/refresh'));
      refreshReq.flush('Success');

      // Na refresh worden de overview en apps opnieuw opgehaald
      httpTesting.expectOne((r) => r.url.endsWith('/google/oAuth/overview')).flush(MOCK_OVERVIEW);
      httpTesting.expectOne((r) => r.url.endsWith('/google/oAuth')).flush(MOCK_APPS);

      fixture.detectChanges();
      expect(component.isRefreshing()).toBe(false);
    });
  });

  it('should open security score detail dialog when card is clicked', () => {
    flushInitialData();

    const scoreCard = fixture.debugElement.query(By.css('[role="button"]'));
    scoreCard.nativeElement.click();

    expect(securityScoreService.open).toHaveBeenCalled();
  });
});
