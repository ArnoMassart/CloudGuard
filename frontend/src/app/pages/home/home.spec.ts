import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTransloco, TranslocoLoader, TranslocoService } from '@jsverse/transloco';
import { of, throwError } from 'rxjs';
import { Home } from './home';
import { DashboardService } from '../../services/dashboard-service';
import { ReportService } from '../../services/report-service';
import { Router } from '@angular/router';
import { DashboardPageResponse } from '../../models/dashboard/DashboardPageResponse';
import { DashboardOverviewResponse } from '../../models/dashboard/DashboardOverviewResponse';

// Mock vertalingen (als je die gebruikt in de HTML)
const I18N_MOCK: Record<string, string> = {
  welcome: 'Welkom',
};

class HomeTranslocoLoader implements TranslocoLoader {
  getTranslation() {
    return of(I18N_MOCK);
  }
}

describe('Home', () => {
  let component: Home;
  let fixture: ComponentFixture<Home>;
  let translocoService: TranslocoService;

  let dashboardServiceMock: {
    getDashboardData: ReturnType<typeof vi.fn>;
    getDashboardPageOverview: ReturnType<typeof vi.fn>;
  };
  let reportServiceMock: {
    downloadSecurityRapport: ReturnType<typeof vi.fn>;
  };
  let routerMock: {
    navigate: ReturnType<typeof vi.fn>;
  };

  const mockDashboardData = {
    overallScore: 85,
    // Voeg hier eventuele andere verplichte velden toe van DashboardPageResponse
  } as DashboardPageResponse;

  const mockOverviewData = {
    criticalNotifications: 2, // Zorgt ervoor dat hasWarnings() true wordt
    // Andere overzicht data...
  } as DashboardOverviewResponse;

  beforeEach(async () => {
    dashboardServiceMock = {
      getDashboardData: vi.fn(() => of(mockDashboardData)),
      getDashboardPageOverview: vi.fn(() => of(mockOverviewData)),
    };

    reportServiceMock = {
      downloadSecurityRapport: vi.fn(() =>
        of(new Blob(['dummy pdf data'], { type: 'application/pdf' }))
      ),
    };

    routerMock = {
      navigate: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [Home],
      providers: [
        { provide: DashboardService, useValue: dashboardServiceMock },
        { provide: ReportService, useValue: reportServiceMock },
        { provide: Router, useValue: routerMock },
        provideTransloco({
          config: {
            availableLangs: ['en', 'nl'],
            defaultLang: 'en',
            reRenderOnLangChange: true,
          },
          loader: HomeTranslocoLoader,
        }),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Home);
    component = fixture.componentInstance;
    translocoService = TestBed.inject(TranslocoService);

    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('loads dashboard data on init (via transloco subscription)', () => {
    expect(dashboardServiceMock.getDashboardData).toHaveBeenCalled();
    expect(component.isLoading()).toBe(false);
    expect(component.pageResponse()).toEqual(mockDashboardData);
    expect(component.score()).toBe(85);
  });

  it('loads page overview on init and sets warnings', () => {
    expect(dashboardServiceMock.getDashboardPageOverview).toHaveBeenCalled();
    expect(component.pageOverview()).toEqual(mockOverviewData);
    // hasWarnings is true omdat criticalNotifications in onze mock 2 is
    expect(component.hasWarnings()).toBe(true);
  });

  it('routeToPage navigates to the given link', () => {
    component.routeToPage('/settings');
    expect(routerMock.navigate).toHaveBeenCalledWith(['/settings']);
  });

  describe('generateRapport', () => {
    it('downloads the pdf and triggers the browser save dialog', () => {
      // Mock de DOM en Browser API's
      const createObjectURLSpy = vi
        .spyOn(globalThis.URL, 'createObjectURL')
        .mockReturnValue('blob:mock-url');
      const revokeObjectURLSpy = vi
        .spyOn(globalThis.URL, 'revokeObjectURL')
        .mockImplementation(() => {});

      const mockAnchorElement = {
        href: '',
        download: '',
        click: vi.fn(),
      } as unknown as HTMLAnchorElement;

      const createElementSpy = vi
        .spyOn(document, 'createElement')
        .mockReturnValue(mockAnchorElement);

      // Voer functie uit
      component.generateRapport();

      // Controleer API call
      expect(reportServiceMock.downloadSecurityRapport).toHaveBeenCalled();

      // Controleer of de Blob URL is aangemaakt
      expect(createObjectURLSpy).toHaveBeenCalledWith(expect.any(Blob));

      // Controleer of het (onzichtbare) a-element is aangemaakt en aangeklikt
      expect(createElementSpy).toHaveBeenCalledWith('a');
      expect(mockAnchorElement.href).toBe('blob:mock-url');
      expect(mockAnchorElement.download).toBe('Security_Report_CLOUDMEN_Labo.pdf');
      expect(mockAnchorElement.click).toHaveBeenCalled();

      // Controleer of de URL netjes is opgeruimd uit het geheugen
      expect(revokeObjectURLSpy).toHaveBeenCalledWith('blob:mock-url');

      // Herstel de spys
      createObjectURLSpy.mockRestore();
      revokeObjectURLSpy.mockRestore();
      createElementSpy.mockRestore();
    });

    it('does not trigger download if already generating', () => {
      component.isGenerating.set(true);
      component.generateRapport();

      expect(reportServiceMock.downloadSecurityRapport).not.toHaveBeenCalled();
    });

    it('handles download error gracefully', () => {
      const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
      const alertSpy = vi.spyOn(window, 'alert').mockImplementation(() => {});

      reportServiceMock.downloadSecurityRapport.mockReturnValue(
        throwError(() => new Error('Server error'))
      );

      component.generateRapport();

      expect(consoleSpy).toHaveBeenCalledWith('Download failed', expect.any(Error));
      expect(alertSpy).toHaveBeenCalledWith('Could not generate PDF. Please try again.');
      expect(component.isGenerating()).toBe(false);

      consoleSpy.mockRestore();
      alertSpy.mockRestore();
    });
  });

  it('handles getDashboardData API error gracefully', async () => {
    const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
    dashboardServiceMock.getDashboardData.mockReturnValue(
      throwError(() => new Error('Dashboard API down'))
    );

    // Trigger de API call opnieuw via taalverandering
    translocoService.setActiveLang('nl');
    await fixture.whenStable();

    expect(consoleSpy).toHaveBeenCalledWith('Failed to load dashboard data', expect.any(Error));
    expect(component.isLoading()).toBe(false);

    consoleSpy.mockRestore();
  });

  it('cleans up language subscription on destroy', () => {
    component.ngOnDestroy();
    dashboardServiceMock.getDashboardData.mockClear();

    // Na vernietiging zou een taalwijziging geen API calls meer mogen triggeren
    translocoService.setActiveLang('nl');
    expect(dashboardServiceMock.getDashboardData).not.toHaveBeenCalled();
  });
});
