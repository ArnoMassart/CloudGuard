import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideTransloco, TranslocoLoader, TranslocoService } from '@jsverse/transloco';
import { of } from 'rxjs';
import { By } from '@angular/platform-browser';
import { Licenses } from './licenses';
import { LicenseService } from '../../../services/license-service';
import { AppIcons } from '../../../shared/AppIcons';
import { provideTranslocoTesting } from '../../../testing/transloco-testing';

describe('Licenses Integration', () => {
  let component: Licenses;
  let fixture: ComponentFixture<Licenses>;
  let httpTesting: HttpTestingController;
  let translocoService: TranslocoService;

  beforeEach(async () => {
    TestBed.resetTestingModule();

    await TestBed.configureTestingModule({
      imports: [Licenses],
      providers: [
        LicenseService, // De ECHTE service
        provideHttpClient(),
        provideHttpClientTesting(),
        provideTranslocoTesting(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Licenses);
    component = fixture.componentInstance;
    httpTesting = TestBed.inject(HttpTestingController);
    translocoService = TestBed.inject(TranslocoService);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('should show loading spinner and then render data from the API', () => {
    // 1. Trigger ngOnInit (start loading)
    fixture.detectChanges();

    // Check of de loading spinner zichtbaar is
    const loader = fixture.debugElement.query(By.css('.animate-spin'));
    expect(loader).toBeTruthy();
    expect(fixture.nativeElement.textContent).toContain('Licenses are loading');
    // 2. Handel de HTTP requests af
    const overviewReq = httpTesting.expectOne((r) => r.url.endsWith('/google/license/overview'));
    const licenseReq = httpTesting.expectOne((r) => r.url.endsWith('/google/license'));

    overviewReq.flush({
      totalAssigned: 150,
      riskyAccounts: 3,
      unusedLicenses: 12,
    });

    licenseReq.flush({
      licenseTypes: [{ skuId: '1', skuName: 'Google Workspace', totalAssigned: 150 }],
      inactiveUsers: [],
      maxLicenseAmount: 200,
      chartStepSize: 50,
    });

    // 3. Update UI
    fixture.detectChanges();

    // Assert: Top cards moeten data bevatten
    const cards = fixture.debugElement.queryAll(By.css('app-section-top-card'));
    expect(cards[0].componentInstance.Value).toBe(150);
    expect(cards[1].componentInstance.Value).toBe(3);

    // Assert: Tabel moet rij bevatten
    const tableRows = fixture.debugElement.queryAll(By.css('tbody tr'));
    expect(tableRows.length).toBe(1);
    expect(tableRows[0].nativeElement.textContent).toContain('Google Workspace');

    // Assert: Chart data moet geüpdatet zijn
    expect(component.barChartData.labels).toContain('Google Workspace');
    expect(component.barChartData.datasets[0].data).toContain(150);
  });

  it('should display warnings when risky accounts are present', () => {
    fixture.detectChanges();

    // Flush alleen de overview met risky accounts
    httpTesting
      .expectOne((r) => r.url.endsWith('/google/license/overview'))
      .flush({
        riskyAccounts: 5,
      });
    httpTesting
      .expectOne((r) => r.url.endsWith('/google/license'))
      .flush({
        licenseTypes: [],
        inactiveUsers: [],
      });

    fixture.detectChanges();

    const warningBox = fixture.debugElement.query(By.css('app-page-warnings'));
    expect(warningBox).toBeTruthy();
    expect(warningBox.nativeElement.textContent).toContain(
      'Security alert There are 5 accounts with potential security risks that require attention. These accounts are inactive or suspended but still have licenses assigned.'
    );
  });

  it('should show error state when API fails', () => {
    fixture.detectChanges();

    // Laat de hoofd-aanroep falen
    httpTesting
      .expectOne((r) => r.url.endsWith('/google/license'))
      .flush('Fout', {
        status: 500,
        statusText: 'Server Error',
      });
    // Overview mag wel lukken
    httpTesting.expectOne((r) => r.url.endsWith('/google/license/overview')).flush({});

    fixture.detectChanges();

    const errorDiv = fixture.debugElement.query(By.css('.bg-red-50\\/50'));
    expect(errorDiv).toBeTruthy();
    expect(component.apiError()).toBe(true);
  });

  it('should reload data when language changes', () => {
    fixture.detectChanges();

    // Eerste lading afhandelen
    httpTesting.expectOne((r) => r.url.endsWith('/google/license/overview')).flush({});
    httpTesting
      .expectOne((r) => r.url.endsWith('/google/license'))
      .flush({ licenseTypes: [], inactiveUsers: [] });

    // Verander taal
    translocoService.setActiveLang('nl');

    // De component heeft een subscription op langChanges$, dus er moeten nieuwe requests komen
    const reqs = httpTesting.match((r) => r.url.includes('/google/license'));
    expect(reqs.length).toBe(2); // Een voor overview, een voor licenses
  });
});
