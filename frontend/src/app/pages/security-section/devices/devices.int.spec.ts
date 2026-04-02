import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideTransloco, TranslocoLoader } from '@jsverse/transloco';
import { By } from '@angular/platform-browser';
import { of } from 'rxjs';
import { Devices } from './devices';
import { DeviceService } from '../../../services/device-service';
import { SecurityPreferencesFacade } from '../../../services/security-preferences-facade';
import { SecurityScoreDetailService } from '../../../services/security-score-detail.service';
import { DeviceStatus } from '../../../models/devices/DeviceStatus';
import { provideTranslocoTesting } from '../../../testing/transloco-testing';

// Mock Data
const MOCK_OVERVIEW = {
  totalDevices: 10,
  totalNonCompliant: 2,
  securityScore: 70,
  warnings: {
    hasWarnings: true,
    items: { lockScreenWarning: true },
  },
  lockScreenCount: 1,
};

const MOCK_DEVICES = {
  devices: [
    {
      resourceId: 'dev-123',
      userName: 'John Doe',
      deviceName: 'Pixel 7',
      lockSecure: false, // Dit is een risico
      complianceScore: 40,
      status: 'Approved',
    },
  ],
  nextPageToken: 'token-abc',
};

describe('Devices Integration', () => {
  let component: Devices;
  let fixture: ComponentFixture<Devices>;
  let httpTesting: HttpTestingController;
  let preferencesFacade: any;

  beforeEach(async () => {
    // We mocken de facade omdat deze vaak naar localStorage of een andere API kijkt
    preferencesFacade = {
      loadWithPrefs$: vi.fn((obs) => obs),
      isDisabled: vi.fn().mockReturnValue(false),
    };

    await TestBed.configureTestingModule({
      imports: [Devices],
      providers: [
        DeviceService,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: SecurityPreferencesFacade, useValue: preferencesFacade },
        {
          provide: SecurityScoreDetailService,
          useValue: { open: vi.fn(), createSimpleBreakdown: vi.fn() },
        },
        provideTranslocoTesting(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Devices);
    component = fixture.componentInstance;
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  const flushInitialRequests = () => {
    fixture.detectChanges(); // Trigger ngOnInit

    // 1. Device Types laden
    httpTesting.expectOne((r) => r.url.endsWith('/devices/types')).flush(['ANDROID', 'IOS']);
    // 2. Overview laden
    httpTesting.expectOne((r) => r.url.endsWith('/devices/overview')).flush(MOCK_OVERVIEW);
    // 3. Devices laden
    httpTesting.expectOne((r) => r.url.endsWith('/google/devices')).flush(MOCK_DEVICES);

    fixture.detectChanges();
  };

  it('should render device list and overview cards correctly', () => {
    flushInitialRequests();

    expect(component.devices().length).toBe(1);

    // Check of de top card de juiste waarde toont
    const totalCard = fixture.debugElement.query(By.css('app-section-top-card'));
    expect(totalCard.componentInstance.Value).toBe(10);

    // Check of de tabel de device naam toont
    const row = fixture.debugElement.query(By.css('tbody tr')).nativeElement;
    expect(row.textContent).toContain('John Doe');
    expect(row.textContent).toContain('Pixel 7');
  });

  it('should filter devices when status is changed via the select menu', () => {
    flushInitialRequests();

    // Act: Verander de status naar 'Blocked'
    component.onStatusChange(DeviceStatus.Blocked);
    fixture.detectChanges();

    // Assert: Er moet een nieuwe HTTP call uitgaan met de status parameter
    const req = httpTesting.expectOne((r) => r.url.endsWith('/google/devices'));
    expect(req.request.params.get('status')).toBe('Blocked');
    req.flush(MOCK_DEVICES);
  });

  describe('Security Factor Integration (Muting)', () => {
    it('should show a factor as "warn" when insecure and not disabled in preferences', () => {
      flushInitialRequests();

      // Open de details van het eerste apparaat
      component.toggleExpand('dev-123');
      fixture.detectChanges();

      const factors = component.getDeviceFactors(component.devices()[0]);
      const lockscreenFactor = factors.find((f) => f.key === 'lockscreen');

      expect(lockscreenFactor?.state).toBe('warn');

      // Check de UI kleur (red background voor 'warn')
      const factorElement = fixture.debugElement.query(By.css('.bg-\\[\\#ffe2e2\\]'));
      expect(factorElement).toBeTruthy();
    });

    it('should show a factor as "muted" when insecure but disabled in preferences', () => {
      // Mock dat 'lockscreen' checks uitstaan in de voorkeuren
      preferencesFacade.isDisabled.mockImplementation((section: string, key: string) => {
        return key === 'lockscreen';
      });

      flushInitialRequests();
      component.toggleExpand('dev-123');
      fixture.detectChanges();

      const factors = component.getDeviceFactors(component.devices()[0]);
      const lockscreenFactor = factors.find((f) => f.key === 'lockscreen');

      // Assert dat de logica de preference honoreert
      expect(lockscreenFactor?.state).toBe('muted');

      // Check of de UI de 'muted' class (gray) gebruikt
      const mutedContainer = fixture.debugElement.query(By.css('.bg-gray-50\\/90'));
      expect(mutedContainer).toBeTruthy();
    });
  });

  it('should reset pagination and clear expanded rows when filters change', () => {
    flushInitialRequests();

    // Zet een row open
    component.expandedDevice.set('dev-123');
    const paginationResetSpy = vi.spyOn(component.pagination()!, 'reset');

    // Verander type
    component.onDeviceTypeChange('ANDROID');
    fixture.detectChanges();

    expect(component.expandedDevice()).toBeNull();
    expect(paginationResetSpy).toHaveBeenCalled();

    // Vervang regel 186 door dit:
    httpTesting
      .expectOne(
        (req) => req.url.endsWith('/google/devices') && req.params.get('deviceType') === 'ANDROID'
      )
      .flush(MOCK_DEVICES);
  });
});
