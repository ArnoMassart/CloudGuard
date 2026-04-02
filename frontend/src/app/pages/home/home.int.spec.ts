import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { By } from '@angular/platform-browser';
import { Home } from './home';
import { DashboardService } from '../../services/dashboard-service';
import { ReportService } from '../../services/report-service';
import { provideTranslocoTesting } from '../../testing/transloco-testing';

describe('Home Integration', () => {
  let component: Home;
  let fixture: ComponentFixture<Home>;
  let httpTesting: HttpTestingController;
  let router: Router;

  beforeEach(async () => {
    TestBed.resetTestingModule();

    await TestBed.configureTestingModule({
      imports: [Home],
      providers: [
        DashboardService,
        ReportService,
        provideHttpClient(),
        provideHttpClientTesting(),
        provideTranslocoTesting(),
        { provide: Router, useValue: { navigate: vi.fn() } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Home);
    component = fixture.componentInstance;
    httpTesting = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
  });

  afterEach(() => {
    // Deze controleert of er requests "open" zijn blijven staan
    httpTesting.verify();
  });

  it('should show loading state and then display dashboard data', () => {
    // 1. Initialiseer: dit triggert de 2 requests in ngOnInit
    fixture.detectChanges();

    // Check loading UI
    expect(fixture.nativeElement.textContent).toContain('Dashboard data is loading');
    expect(component.isLoading()).toBe(true);

    // FIX: Je MOET deze requests afhandelen, anders blijft de test 'open' staan
    const dataReq = httpTesting.expectOne((r) => r.url.endsWith('/dashboard'));
    const overviewReq = httpTesting.expectOne((r) => r.url.endsWith('/dashboard/overview'));

    dataReq.flush({
      overallScore: 82,
      lastUpdated: '2024-03-20',
      scores: { usersScore: 90, devicesScore: 75 },
    });

    overviewReq.flush({
      totalNotifications: 10,
      criticalNotifications: 0,
    });

    // Update de UI na het flushen van de data
    fixture.detectChanges();

    // Assert resultaten
    expect(component.isLoading()).toBe(false);
    expect(component.score()).toBe(82);
  });

  it('should display the critical warning box when critical notifications exist', () => {
    fixture.detectChanges();

    // Handel de requests af
    httpTesting.expectOne((r) => r.url.endsWith('/dashboard')).flush({ overallScore: 40 });
    httpTesting
      .expectOne((r) => r.url.endsWith('/dashboard/overview'))
      .flush({
        criticalNotifications: 3,
      });

    fixture.detectChanges();

    const warningBox = fixture.debugElement.query(By.css('app-page-warnings'));
    expect(warningBox).toBeTruthy();
    // Check op de Engelse tekst uit je nieuwe I18N_MOCK
    expect(warningBox.nativeElement.textContent).toContain(
      ' 3 critical security alerts  There are urgent security issues that require immediate attention. View notifications'
    );
  });

  it('should navigate to the correct page when a security component is clicked', () => {
    fixture.detectChanges();
    httpTesting.expectOne((r) => r.url.endsWith('/dashboard')).flush({ overallScore: 80 });
    httpTesting.expectOne((r) => r.url.endsWith('/dashboard/overview')).flush({});
    fixture.detectChanges();

    component.routeToPage('shared-drives');
    expect(router.navigate).toHaveBeenCalledWith(['shared-drives']);
  });

  describe('Report Generation', () => {
    it('should trigger report download and handle the blob response', async () => {
      const createSpy = vi.spyOn(globalThis.URL, 'createObjectURL').mockReturnValue('blob:url');
      const revokeSpy = vi.spyOn(globalThis.URL, 'revokeObjectURL').mockImplementation(() => {});

      fixture.detectChanges();
      // Handel initial requests af
      httpTesting.expectOne((r) => r.url.endsWith('/dashboard')).flush({});
      httpTesting.expectOne((r) => r.url.endsWith('/dashboard/overview')).flush({});
      fixture.detectChanges();

      const buttons = fixture.debugElement.queryAll(By.css('button'));
      const reportBtn = buttons.find((btn) => {
        const text = btn.nativeElement.textContent.toLowerCase();
        return text.includes('generate') && text.includes('report');
      });

      if (!reportBtn) throw new Error('Rapport knop niet gevonden!');

      reportBtn.nativeElement.click();
      fixture.detectChanges();

      expect(component.isGenerating()).toBe(true);

      const reportReq = httpTesting.expectOne((r) => r.url.endsWith('/report'));
      reportReq.flush(new Blob(['test'], { type: 'application/pdf' }));

      await fixture.whenStable();
      fixture.detectChanges();

      expect(component.isGenerating()).toBe(false);
      expect(createSpy).toHaveBeenCalled();

      createSpy.mockRestore();
      revokeSpy.mockRestore();
    });
  });
});
