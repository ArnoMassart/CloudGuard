import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideTransloco, TranslocoLoader } from '@jsverse/transloco';
import { By } from '@angular/platform-browser';
import { of } from 'rxjs';
import { SharedDrives } from './shared-drives';
import { DriveService } from '../../../services/drive-service';
import { SecurityPreferencesFacade } from '../../../services/security-preferences-facade';
import { SecurityScoreDetailService } from '../../../services/security-score-detail.service';
import { provideTranslocoTesting } from '../../../testing/transloco-testing';

// Mock Data

const MOCK_OVERVIEW = {
  totalDrives: 10,
  orphanDrives: 2,
  totalHighRisk: 1,
  externalMembersDriveCount: 3,
  securityScore: 85,
  warnings: {
    hasWarnings: true,
    items: { orphanDrivesWarning: true },
  },
};

const MOCK_DRIVES_PAGE = {
  drives: [
    {
      id: 'drive-1',
      name: 'Marketing',
      risk: 'low',
      totalMembers: 5,
      externalMembers: 0,
      totalOrganizers: 2,
      parsedTime: '2 days ago',
    },
  ],
  nextPageToken: 'token-abc',
};

describe('SharedDrives Integration', () => {
  let component: SharedDrives;
  let fixture: ComponentFixture<SharedDrives>;
  let httpTesting: HttpTestingController;
  let preferencesFacade: any;
  let securityScoreService: any;

  beforeEach(async () => {
    // Mock de Facade en de Dialog Service
    preferencesFacade = {
      loadWithPrefs$: vi.fn((obs) => obs),
      isDisabled: vi.fn().mockReturnValue(false),
    };

    securityScoreService = {
      open: vi.fn(),
      createSimpleBreakdown: vi
        .fn()
        .mockReturnValue({ totalScore: 85, status: 'good', factors: [] }),
    };

    await TestBed.configureTestingModule({
      imports: [SharedDrives],
      providers: [
        DriveService,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: SecurityPreferencesFacade, useValue: preferencesFacade },
        { provide: SecurityScoreDetailService, useValue: securityScoreService },
        provideTranslocoTesting(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SharedDrives);
    component = fixture.componentInstance;
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  const flushInitialRequests = () => {
    fixture.detectChanges(); // Trigger ngOnInit
    httpTesting.expectOne((r) => r.url.endsWith('/google/drives/overview')).flush(MOCK_OVERVIEW);
    httpTesting.expectOne((r) => r.url.endsWith('/google/drives')).flush(MOCK_DRIVES_PAGE);
    fixture.detectChanges();
  };

  it('should load overview and drives on init and render them in the UI', () => {
    flushInitialRequests();

    expect(component.drives().length).toBe(1);
    expect(component.pageOverview()?.totalDrives).toBe(10);

    // Check of de top card de juiste waarde toont
    const totalCard = fixture.debugElement.query(By.css('app-section-top-card'));
    expect(totalCard.componentInstance.Value).toBe(10);

    // Check of de drive naam in de lijst staat
    const driveTitle = fixture.debugElement.query(By.css('article p')).nativeElement;
    expect(driveTitle.textContent).toContain('Marketing');
  });

  it('should trigger a new search and reset pagination when search changes', () => {
    flushInitialRequests();
    const paginationResetSpy = vi.spyOn(component.pagination()!, 'reset');

    // Simuleer een zoekopdracht
    component.onSearch('Finance');
    fixture.detectChanges();

    // Check of de URL de query parameter bevat
    const req = httpTesting.expectOne(
      (r) => r.url.endsWith('/google/drives') && r.params.get('query') === 'Finance'
    );
    expect(req.request.method).toBe('GET');
    expect(paginationResetSpy).toHaveBeenCalled();

    req.flush(MOCK_DRIVES_PAGE);
  });

  describe('KPI and Preferences Integration', () => {
    it('should change KPI colors when a warning is "muted" in preferences', () => {
      // Mock dat 'orphan drives' alerts uitstaan
      preferencesFacade.isDisabled.mockImplementation((section: string, key: string) => {
        return section === 'shared-drives' && key === 'orphan';
      });

      flushInitialRequests();

      // De kpiOrphanDrivesColors signal moet nu de 'muted' kleuren teruggeven
      // Dit testen we door te kijken of de card component de 'muted' kleur signalen ontvangt
      const orphanCard = fixture.debugElement.queryAll(By.css('app-section-top-card'))[1];

      // De 'kpiColors' functie (die we hier indirect testen) zou bij muted een grijze kleur moeten geven
      // We verifiëren dat de component de kleur van de facade/computed waarde doorgeeft
      expect(orphanCard.componentInstance.BackgroundColor).toBe(
        component.kpiOrphanDrivesColors().bg
      );
    });
  });

  it('should show the warning box when the API returns warnings', () => {
    flushInitialRequests();

    const warningBox = fixture.debugElement.query(By.css('app-page-warnings'));
    expect(warningBox).toBeTruthy();
    expect(warningBox.nativeElement.textContent).toContain('2'); // orphanDrives uit mock
  });

  it('should refresh data and reset pagination when the refresh button is clicked', () => {
    flushInitialRequests();
    const paginationResetSpy = vi.spyOn(component.pagination()!, 'reset');

    component.refreshData();
    expect(component.isRefreshing()).toBe(true);

    // 1. Handel de POST refresh af
    httpTesting.expectOne((r) => r.url.endsWith('/google/drives/refresh')).flush('OK');

    // 2. Daarna komen de automatische GET calls weer
    httpTesting.expectOne((r) => r.url.endsWith('/google/drives/overview')).flush(MOCK_OVERVIEW);
    httpTesting.expectOne((r) => r.url.endsWith('/google/drives')).flush(MOCK_DRIVES_PAGE);

    fixture.detectChanges();
    expect(component.isRefreshing()).toBe(false);
    expect(paginationResetSpy).toHaveBeenCalled();
  });

  it('should open security score detail with correct breakdown when score card is clicked', () => {
    flushInitialRequests();

    const scoreCard = fixture.debugElement.query(By.css('[role="button"]'));
    scoreCard.nativeElement.click();

    expect(securityScoreService.open).toHaveBeenCalled();
  });
});
