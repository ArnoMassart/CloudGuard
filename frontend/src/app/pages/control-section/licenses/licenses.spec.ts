import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTransloco, TranslocoLoader, TranslocoService } from '@jsverse/transloco';
import { of, throwError } from 'rxjs';
import { Licenses } from './licenses';
import { LicenseService } from '../../../services/license-service';

// Mock vertalingen
const FB_I18N: Record<string, string> = {
  assigned: 'Toegewezen',
};

class LicensesTranslocoLoader implements TranslocoLoader {
  getTranslation() {
    return of(FB_I18N);
  }
}

describe('Licenses', () => {
  let component: Licenses;
  let fixture: ComponentFixture<Licenses>;
  let translocoService: TranslocoService;

  let licenseServiceMock: {
    getLicenses: ReturnType<typeof vi.fn>;
    getLicensesPageOverview: ReturnType<typeof vi.fn>;
  };

  const mockLicenseTypes = [
    { skuName: 'Google Workspace Enterprise Plus', totalAssigned: 50 },
    { skuName: 'Voice Starter', totalAssigned: 10 },
  ];

  const mockInactiveUsers = [{ name: 'John Doe', email: 'john@example.com', daysInactive: 45 }];

  const mockLicensePageRes = {
    licenseTypes: mockLicenseTypes,
    inactiveUsers: mockInactiveUsers,
    maxLicenseAmount: 100,
    chartStepSize: 20,
  };

  const mockOverviewRes = {
    totalLicenses: 150,
    assignedLicenses: 60,
    unassignedLicenses: 90,
    riskyAccounts: true,
  };

  beforeEach(async () => {
    // Definieer de mocks voor Vitest
    licenseServiceMock = {
      getLicenses: vi.fn(() => of(mockLicensePageRes)),
      getLicensesPageOverview: vi.fn(() => of(mockOverviewRes)),
    };

    // Voorkom dat Chart.js probeert een echte canvas te renderen in de testomgeving
    HTMLCanvasElement.prototype.getContext = vi.fn();

    await TestBed.configureTestingModule({
      imports: [Licenses],
      providers: [
        { provide: LicenseService, useValue: licenseServiceMock },
        provideTransloco({
          config: {
            availableLangs: ['en', 'nl'],
            defaultLang: 'en',
            reRenderOnLangChange: true,
          },
          loader: LicensesTranslocoLoader,
        }),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Licenses);
    component = fixture.componentInstance;
    translocoService = TestBed.inject(TranslocoService);

    // Roep ngOnInit aan en wacht tot de observables (waaronder de taal-transloco) zijn afgerond
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('loads page overview and sets warnings on init', () => {
    expect(licenseServiceMock.getLicensesPageOverview).toHaveBeenCalled();
    expect(component.pageOverview()).toEqual(mockOverviewRes);
    expect(component.hasWarnings()).toBe(true);
  });

  it('loads licenses data and maps to signals correctly', () => {
    expect(licenseServiceMock.getLicenses).toHaveBeenCalled();
    expect(component.isLoading()).toBe(false);
    expect(component.apiError()).toBe(false);
    expect(component.licenseTypes()).toEqual(mockLicenseTypes);
    expect(component.inactiveUsers()).toEqual(mockInactiveUsers);
    expect(component.maxLicenseAmount()).toBe(100);
    expect(component.stepSize()).toBe(20);
  });

  it('updates the bar chart data and options after loading', () => {
    // Controleer de dataset mapping
    expect(component.barChartData.labels).toEqual([
      'Google Workspace Enterprise Plus',
      'Voice Starter',
    ]);
    expect(component.barChartData.datasets[0].data).toEqual([50, 10]);
    expect(component.barChartData.datasets[0].label).toBe('assigned');

    // Controleer of de dynamische stepSize en max in de Y-as configuratie staan
    const yScale = component.barChartOptions?.scales?.['y'] as any;
    expect(yScale).toBeDefined();
    expect(yScale.max).toBe(100);
    expect(yScale.ticks.stepSize).toBe(20);
  });

  it('truncates long labels on the x-axis', () => {
    // Haal de callback functie van de chart configuratie op
    const callback = component.barChartOptions?.scales?.['x']?.ticks?.callback as Function;

    // Simuleer de Chart.js 'this' context die wordt gebruikt in getLabelForValue
    const mockContext = { getLabelForValue: (val: string) => val };

    // Test een kort label
    expect(callback.call(mockContext, 'Short Label')).toBe('Short Label');

    // Test een lang label (>15 tekens)
    expect(callback.call(mockContext, 'A Very Long License Type Name')).toBe('A Very Long Lic...');
  });

  it('toggleExpanded flips the state', () => {
    const initialState = component.isExpanded();
    component.toggleExpanded();
    expect(component.isExpanded()).toBe(!initialState);
  });

  it('handles getLicenses API error correctly', async () => {
    // Mock een foutmelding
    licenseServiceMock.getLicenses.mockReturnValue(throwError(() => new Error('API down')));

    // Trigger een taalverandering, dit roept in jouw ngOnInit de private methodes opnieuw aan
    translocoService.setActiveLang('nl');
    await fixture.whenStable();

    // Controleer of de fout netjes is afgevangen in de state
    expect(component.apiError()).toBe(true);
    expect(component.isLoading()).toBe(false);
  });

  it('handles getLicensesPageOverview API error gracefully (logs to console)', async () => {
    const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
    licenseServiceMock.getLicensesPageOverview.mockReturnValue(
      throwError(() => new Error('Overview failed'))
    );

    // Trigger de API call opnieuw via taalverandering
    translocoService.setActiveLang('nl');
    await fixture.whenStable();

    expect(consoleSpy).toHaveBeenCalledWith('Failed to load page overview', expect.any(Error));
    consoleSpy.mockRestore();
  });

  it('cleans up language subscription on destroy', () => {
    // Simuleer het vernietigen van het component
    component.ngOnDestroy();

    // Reset de mocks en trigger een taalwijziging
    licenseServiceMock.getLicenses.mockClear();
    translocoService.setActiveLang('nl');

    // Omdat de subscription vernietigd is, zou dit géén nieuwe API call mogen triggeren
    expect(licenseServiceMock.getLicenses).not.toHaveBeenCalled();
  });
});
