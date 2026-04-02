import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTransloco, TranslocoLoader, TranslocoService } from '@jsverse/transloco';
import { of, throwError } from 'rxjs';
import { Devices } from './devices';
import { DeviceService } from '../../../services/device-service';
import { SecurityScoreDetailService } from '../../../services/security-score-detail.service';
import { SecurityPreferencesFacade } from '../../../services/security-preferences-facade';
import { Device } from '../../../models/devices/Device';
import { DevicesOverviewResponse } from '../../../models/devices/DevicesOverviewResponse';
import { DeviceStatus } from '../../../models/devices/DeviceStatus';
import { AppIcons } from '../../../shared/AppIcons';

// Mock vertalingen
const I18N_MOCK: Record<string, string> = {
  'devices.title': 'Apparaten',
};

class DevicesTranslocoLoader implements TranslocoLoader {
  getTranslation() {
    return of(I18N_MOCK);
  }
}

describe('Devices', () => {
  let component: Devices;
  let fixture: ComponentFixture<Devices>;
  let translocoService: TranslocoService;

  // Mocks
  let deviceServiceMock: {
    getDevices: ReturnType<typeof vi.fn>;
    getUniqueDeviceTypes: ReturnType<typeof vi.fn>;
    getDevicesPageOverview: ReturnType<typeof vi.fn>;
    refreshDeviceCache: ReturnType<typeof vi.fn>;
  };
  let securityScoreDetailMock: {
    createSimpleBreakdown: ReturnType<typeof vi.fn>;
    open: ReturnType<typeof vi.fn>;
  };
  let prefsFacadeMock: {
    isDisabled: ReturnType<typeof vi.fn>;
    loadWithPrefs$: ReturnType<typeof vi.fn>;
  };

  const mockDevices: Device[] = [
    {
      resourceId: 'dev1', // Gecorrigeerd van 'id' naar 'resourceId'
      deviceType: 'ANDROID',
      userName: 'John Doe',
      userEmail: 'john@cloudmen.com',
      deviceName: 'Johns Pixel 7',
      model: 'Pixel 7',
      os: 'Android 14',
      lastSync: '2024-03-20T10:00:00Z',
      status: 'COMPLIANT',
      complianceScore: 85,
      lockSecure: true,
      screenLockText: 'Veilig',
      encSecure: false,
      encryptionText: 'Niet versleuteld',
      osSecure: true,
      osText: 'Up-to-date',
      intSecure: true,
      integrityText: 'Ok',
    },
  ];

  const mockPageRes = {
    devices: mockDevices,
    nextPageToken: 'token_abc',
  };

  const mockOverviewRes: DevicesOverviewResponse = {
    totalDevices: 10,
    totalNonCompliant: 2,
    securityScore: 80,
    warnings: {
      hasWarnings: true,
      hasMultipleWarnings: false,
      items: {
        lockScreenWarning: false,
        encryptionWarning: true,
        osVersionWarning: false,
        integrityWarning: false,
      },
    },
    securityScoreBreakdown: undefined, // Voor test met fallback
  } as unknown as DevicesOverviewResponse;

  beforeEach(async () => {
    deviceServiceMock = {
      getDevices: vi.fn(() => of(mockPageRes)),
      getUniqueDeviceTypes: vi.fn(() => of(['ANDROID', 'IOS'])),
      getDevicesPageOverview: vi.fn(() => of(mockOverviewRes)),
      refreshDeviceCache: vi.fn(() => of('OK')),
    };

    securityScoreDetailMock = {
      createSimpleBreakdown: vi.fn(() => ({ score: 80, factors: [] })),
      open: vi.fn(),
    };

    prefsFacadeMock = {
      // We doen alsof 'osVersion' genegeerd is in de preferences voor testdoeleinden
      isDisabled: vi.fn((category, key) => key === 'osVersion'),
      loadWithPrefs$: vi.fn((obs) => obs),
    };

    await TestBed.configureTestingModule({
      imports: [Devices],
      providers: [
        { provide: DeviceService, useValue: deviceServiceMock },
        { provide: SecurityScoreDetailService, useValue: securityScoreDetailMock },
        { provide: SecurityPreferencesFacade, useValue: prefsFacadeMock },
        provideTransloco({
          config: {
            availableLangs: ['en', 'nl'],
            defaultLang: 'en',
            reRenderOnLangChange: true,
          },
          loader: DevicesTranslocoLoader,
        }),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Devices);
    component = fixture.componentInstance;
    translocoService = TestBed.inject(TranslocoService);

    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('loads types, overview, and devices on init', () => {
    expect(deviceServiceMock.getUniqueDeviceTypes).toHaveBeenCalled();
    expect(component.uniqueDeviceTypes()).toEqual(['all', 'ANDROID', 'IOS']);

    expect(prefsFacadeMock.loadWithPrefs$).toHaveBeenCalled();
    expect(component.pageOverview()).toEqual(mockOverviewRes);

    expect(deviceServiceMock.getDevices).toHaveBeenCalledWith(
      undefined,
      DeviceStatus.All,
      'all',
      4
    );
    expect(component.devices()).toEqual(mockDevices);
    expect(component.nextPageToken()).toBe('token_abc');
    expect(component.isLoading()).toBe(false);
  });

  it('computes warning signals correctly', () => {
    expect(component.hasWarnings()).toBe(true);
    expect(component.hasMultipleWarnings()).toBe(false);

    const warnings = component.devicePageWarnings();
    expect(warnings.encryptionWarning).toBe(true);
    expect(warnings.lockScreenWarning).toBe(false);
  });

  it('toggleExpanded flips the state', () => {
    const initialState = component.isExpanded();
    component.toggleExpanded();
    expect(component.isExpanded()).toBe(!initialState);
  });

  it('toggleExpand sets and unsets expanded device ID', () => {
    component.toggleExpand('dev1');
    expect(component.expandedDevice()).toBe('dev1');

    component.toggleExpand('dev1');
    expect(component.expandedDevice()).toBeNull();

    component.toggleExpand('dev2');
    expect(component.expandedDevice()).toBe('dev2');
  });

  it('onStatusChange updates status and reloads data', () => {
    deviceServiceMock.getDevices.mockClear();

    component.onStatusChange(DeviceStatus.Approved); // Zorg dat dit overeenkomt met je enum

    expect(component.selectedStatus()).toBe(DeviceStatus.Approved);
    expect(component.expandedDevice()).toBeNull();
    expect(deviceServiceMock.getDevices).toHaveBeenCalledWith(
      undefined,
      DeviceStatus.Approved,
      'all',
      4
    );
  });

  it('onDeviceTypeChange updates type and reloads data', () => {
    deviceServiceMock.getDevices.mockClear();

    component.onDeviceTypeChange('ANDROID');

    expect(component.selectedDeviceType()).toBe('ANDROID');
    expect(component.expandedDevice()).toBeNull();
    expect(deviceServiceMock.getDevices).toHaveBeenCalledWith(
      undefined,
      DeviceStatus.All,
      'ANDROID',
      4
    );
  });

  describe('getDeviceFactors', () => {
    it('returns correctly mapped factors based on device security and preferences', () => {
      const factors = component.getDeviceFactors(mockDevices[0]);

      expect(factors.length).toBe(4);

      // Lock is secure -> state 'ok'
      const lockFactor = factors.find((f) => f.key === 'lockscreen');
      expect(lockFactor?.state).toBe('ok');
      expect(lockFactor?.secure).toBe(true);

      // Encryption is not secure -> state 'warn' (want isDisabled is false voor deze key)
      const encFactor = factors.find((f) => f.key === 'encryption');
      expect(encFactor?.state).toBe('warn');
      expect(encFactor?.secure).toBe(false);

      // OS is mock-gewijs ingesteld als 'isDisabled = true' in onze prefsFacadeMock
      // Als we het device onveilig maken op OS-niveau, zou de state 'muted' moeten worden
      const mockOsDevice = { ...mockDevices[0], osSecure: false } as Device;
      const modifiedFactors = component.getDeviceFactors(mockOsDevice);
      const osFactor = modifiedFactors.find((f) => f.key === 'osVersion');

      expect(osFactor?.state).toBe('muted');
    });
  });

  describe('openSecurityScoreDetail', () => {
    it('opens detail with simple breakdown if none exists in overview', () => {
      component.openSecurityScoreDetail();

      expect(securityScoreDetailMock.createSimpleBreakdown).toHaveBeenCalledWith(80, 'devices');
      expect(securityScoreDetailMock.open).toHaveBeenCalledWith(
        { score: 80, factors: [] },
        'devices'
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
      expect(securityScoreDetailMock.open).toHaveBeenCalledWith(existingBreakdown, 'devices');
    });
  });

  describe('refreshData', () => {
    it('calls refresh endpoint, resets pagination, and reloads data', () => {
      deviceServiceMock.getDevices.mockClear();
      prefsFacadeMock.loadWithPrefs$.mockClear();

      component.refreshData();

      expect(deviceServiceMock.refreshDeviceCache).toHaveBeenCalled();
      expect(deviceServiceMock.getDevices).toHaveBeenCalled();
      expect(prefsFacadeMock.loadWithPrefs$).toHaveBeenCalled();
      expect(component.isRefreshing()).toBe(false);
    });

    it('does nothing if already refreshing', () => {
      component.isRefreshing.set(true);
      component.refreshData();
      expect(deviceServiceMock.refreshDeviceCache).not.toHaveBeenCalled();
    });

    it('handles refresh error gracefully', () => {
      const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
      deviceServiceMock.refreshDeviceCache.mockReturnValue(
        throwError(() => new Error('Refresh failed'))
      );

      component.refreshData();

      expect(consoleSpy).toHaveBeenCalledWith('Kon cache niet vernieuwen:', expect.any(Error));
      expect(component.isRefreshing()).toBe(false);

      consoleSpy.mockRestore();
    });
  });

  describe('loadDevices errors', () => {
    it('sets apiError and isLoading correctly on failure', async () => {
      const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
      deviceServiceMock.getDevices.mockReturnValue(throwError(() => new Error('Load failed')));

      translocoService.setActiveLang('nl');
      await fixture.whenStable();

      expect(component.apiError()).toBe(true);
      expect(component.isLoading()).toBe(false);
      expect(consoleSpy).toHaveBeenCalledWith('Failed to load devices', expect.any(Error));

      consoleSpy.mockRestore();
    });
  });

  it('handles getUniqueDeviceTypes error gracefully', () => {
    const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
    deviceServiceMock.getUniqueDeviceTypes.mockReturnValue(
      throwError(() => new Error('Types failed'))
    );

    // Roep het handmatig aan om de fout te triggeren (normaal in ngOnInit)
    component['loadDeviceTypes']();

    expect(consoleSpy).toHaveBeenCalledWith('Kon apparaat typen niet laden', expect.any(Error));
    consoleSpy.mockRestore();
  });

  it('cleans up language subscription on destroy', () => {
    component.ngOnDestroy();
    deviceServiceMock.getDevices.mockClear();

    translocoService.setActiveLang('nl');

    expect(deviceServiceMock.getDevices).not.toHaveBeenCalled();
  });
});
