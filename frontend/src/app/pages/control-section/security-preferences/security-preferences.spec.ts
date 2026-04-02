import { HttpErrorResponse } from '@angular/common/http';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTransloco, TranslocoLoader } from '@jsverse/transloco';
import { of, throwError } from 'rxjs';
import { PreferencesResponse } from '../../../models/preferences/PreferencesResponse';
import { AppIcons } from '../../../shared/AppIcons';
import { SecurityPreferencesFacade } from '../../../services/security-preferences-facade';
import { SecurityPreferencesService } from '../../../services/security-preferences-service';
import { SecurityPreferences } from './security-preferences';

const PREFS_I18N: Record<string, string> = {
  'preferences.title': 'Title',
  'preferences.description': 'Desc',
  'preferences.info.label': 'Info label',
  'preferences.info.title': 'Info title',
  'preferences.info.description': 'Info body',
  'preferences.loading': 'Loading',
  'preferences.error.load': 'Load failed',
  'preferences.error.save': 'Save failed',
  'preferences.error.sync': 'Sync failed',
  'try-again': 'Retry',
  cancel: 'Cancel',
  'preferences.dns.standard': 'Std',
  'preferences.dns.required': 'Req',
  'preferences.dns.recommended': 'Rec',
  'preferences.dns.optional': 'Opt',
  'preferences.toggle-off': 'Off',
  'preferences.toggle-on': 'On',
  'preferences.users-groups': 'UG',
  'preferences.users-groups.without2fa': '2FA',
  'preferences.users-groups.account-activity': 'Act',
  'preferences.users-groups.external-members': 'Ext',
  'preferences.shared-drives': 'SD',
  'preferences.shared-drives.no-admin': 'Orph',
  'preferences.shared-drives.external-members': 'Ex',
  'preferences.shared-drives.outside-domain': 'Out',
  'preferences.shared-drives.nonmember-access': 'NM',
  'preferences.devices': 'Dev',
  'preferences.devices.lockscreen': 'Lock',
  'preferences.devices.encryption': 'Enc',
  'preferences.devices.osVersion': 'OS',
  'preferences.devices.integrity': 'Int',
  'preferences.app-access': 'AA',
  'preferences.app-access.highRisk': 'Risk',
  'preferences.app-passwords': 'AP',
  'preferences.app-passwords.active': 'Act',
  'preferences.password-settings': 'PW',
  'preferences.password-settings.2fa': '2FA',
  'preferences.password-settings.passwordLength': 'Len',
  'preferences.password-settings.strongPasswords': 'Str',
  'preferences.password-settings.expiration': 'Exp',
  'preferences.password-settings.securityKeys': 'Keys',
  'preferences.domain-dns': 'DNS',
  SPF: 'SPF',
  DKIM: 'DKIM',
  DMARC: 'DMARC',
  MX: 'MX',
  DNSSEC: 'DNSSEC',
  CAA: 'CAA',
  TXT: 'TXT',
  CNAME: 'CNAME',
  'preferences.dns.site-verification': 'TXT',
  'preferences.dns.mail-subdomain': 'CNAME',
  'to-admin-console': 'Admin',
};

class PrefsTranslocoLoader implements TranslocoLoader {
  getTranslation() {
    return of(PREFS_I18N);
  }
}

describe('SecurityPreferences', () => {
  let component: SecurityPreferences;
  let fixture: ComponentFixture<SecurityPreferences>;
  let serviceMock: {
    getAllPreferences: ReturnType<typeof vi.fn>;
    setPreference: ReturnType<typeof vi.fn>;
  };
  let facadeMock: { refresh: ReturnType<typeof vi.fn>; disabledKeysRefreshFailed: ReturnType<typeof signal> };

  const payload: PreferencesResponse = {
    preferences: { 'users-groups:2fa': true, 'users-groups:activity': false },
    dnsImportance: { SPF: 'REQUIRED' },
    dnsImportanceOverrideTypes: ['SPF'],
  };

  beforeEach(async () => {
    serviceMock = {
      getAllPreferences: vi.fn(() => of(payload)),
      setPreference: vi.fn(() => of(void 0)),
    };
    facadeMock = { refresh: vi.fn(), disabledKeysRefreshFailed: signal(false) };

    await TestBed.configureTestingModule({
      imports: [SecurityPreferences],
      providers: [
        { provide: SecurityPreferencesService, useValue: serviceMock },
        { provide: SecurityPreferencesFacade, useValue: facadeMock },
        provideTransloco({
          config: {
            availableLangs: ['en'],
            defaultLang: 'en',
            reRenderOnLangChange: true,
          },
          loader: PrefsTranslocoLoader,
        }),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SecurityPreferences);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('loads preferences and clears loading', () => {
    expect(serviceMock.getAllPreferences).toHaveBeenCalled();
    expect(component.loading()).toBe(false);
    expect(component.preferences()['users-groups:2fa']).toBe(true);
    expect(component.dnsImportance()['SPF']).toBe('REQUIRED');
    expect(component.dnsOverrideTypes().has('SPF')).toBe(true);
  });

  it('isEnabled is true when key missing or not false', () => {
    expect(component.isEnabled('unknown', 'key')).toBe(true);
    expect(component.isEnabled('users-groups', '2fa')).toBe(true);
    expect(component.isEnabled('users-groups', 'activity')).toBe(false);
  });

  it('sectionNavIcon maps routes and defaults to Shield', () => {
    expect(component.sectionNavIcon('/users-groups')).toBe(AppIcons.Users);
    expect(component.sectionNavIcon('/unknown')).toBe(AppIcons.Shield);
  });

  it('dnsSelectValue returns stored value only for override types', () => {
    expect(component.dnsSelectValue(undefined)).toBe('');
    expect(component.dnsSelectValue('SPF')).toBe('REQUIRED');
    expect(component.dnsSelectValue('DKIM')).toBe('');
  });

  it('toggle calls API, updates map, and refreshes facade on success', async () => {
    serviceMock.setPreference.mockClear();
    facadeMock.refresh.mockClear();
    component.toggle('users-groups', '2fa');
    await fixture.whenStable();

    expect(serviceMock.setPreference).toHaveBeenCalledWith('users-groups', '2fa', false);
    expect(component.preferences()['users-groups:2fa']).toBe(false);
    expect(facadeMock.refresh).toHaveBeenCalled();
    expect(component.saving()).toBe(null);
  });

  it('toggle on error reloads preferences, clears saving, and sets saveError', async () => {
    serviceMock.setPreference.mockReturnValueOnce(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 400,
            error: 'Preference key is required.',
          }),
      ),
    );
    const calls = serviceMock.getAllPreferences.mock.calls.length;
    component.toggle('users-groups', '2fa');
    await fixture.whenStable();

    expect(serviceMock.getAllPreferences.mock.calls.length).toBeGreaterThan(calls);
    expect(component.saving()).toBe(null);
    expect(component.saveError()).toBe('Preference key is required.');
  });

  it('setDnsImportance no-ops when value unchanged', () => {
    serviceMock.setPreference.mockClear();
    component.setDnsImportance('domain-dns', 'impSpf', 'SPF', 'REQUIRED');
    expect(serviceMock.setPreference).not.toHaveBeenCalled();
  });

  it('setDnsImportance calls API when value changes', async () => {
    serviceMock.getAllPreferences.mockClear();
    component.setDnsImportance('domain-dns', 'impSpf', 'SPF', 'OPTIONAL');
    await fixture.whenStable();

    expect(serviceMock.setPreference).toHaveBeenCalledWith('domain-dns', 'impSpf', true, 'OPTIONAL');
    expect(facadeMock.refresh).toHaveBeenCalled();
    expect(serviceMock.getAllPreferences).toHaveBeenCalled();
    expect(component.saving()).toBe(null);
  });

  it('clears state when load fails', async () => {
    serviceMock.getAllPreferences.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 500, error: 'Server error' })),
    );
    fixture = TestBed.createComponent(SecurityPreferences);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();

    expect(serviceMock.getAllPreferences).toHaveBeenCalled();
    expect(component.preferences()).toEqual({});
    expect(component.dnsImportance()).toEqual({});
    expect(component.dnsOverrideTypes().size).toBe(0);
    expect(component.loading()).toBe(false);
    expect(component.loadError()).toBe('Server error');
  });

  it('uses translated message when load fails without HTTP string body', async () => {
    serviceMock.getAllPreferences.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 500, error: { code: 'x' } })),
    );
    fixture = TestBed.createComponent(SecurityPreferences);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();

    expect(component.loadError()).toBe('Load failed');
  });

  it('retryLoad triggers another getAllPreferences', async () => {
    const calls = serviceMock.getAllPreferences.mock.calls.length;
    component.retryLoad();
    await fixture.whenStable();
    expect(serviceMock.getAllPreferences.mock.calls.length).toBeGreaterThan(calls);
    expect(component.loading()).toBe(false);
  });

  it('dismissLoadError and dismissSaveError clear error signals', () => {
    component.loadError.set('e');
    component.saveError.set('s');
    component.dismissLoadError();
    component.dismissSaveError();
    expect(component.loadError()).toBe(null);
    expect(component.saveError()).toBe(null);
  });

  it('dismissSyncWarning clears facade.disabledKeysRefreshFailed', () => {
    facadeMock.disabledKeysRefreshFailed.set(true);
    component.dismissSyncWarning();
    expect(facadeMock.disabledKeysRefreshFailed()).toBe(false);
  });

  it('setDnsImportance on error reloads preferences and sets saveError', async () => {
    serviceMock.setPreference.mockReturnValueOnce(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 400,
            error: 'DNS save invalid',
          }),
      ),
    );
    const calls = serviceMock.getAllPreferences.mock.calls.length;
    component.setDnsImportance('domain-dns', 'impSpf', 'SPF', 'OPTIONAL');
    await fixture.whenStable();

    expect(component.saveError()).toBe('DNS save invalid');
    expect(serviceMock.getAllPreferences.mock.calls.length).toBeGreaterThan(calls);
    expect(component.saving()).toBe(null);
  });

  it('setDnsImportance uses i18n save message when error has no string body', async () => {
    serviceMock.setPreference.mockReturnValueOnce(
      throwError(() => new HttpErrorResponse({ status: 500 })),
    );
    component.setDnsImportance('domain-dns', 'impSpf', 'SPF', 'OPTIONAL');
    await fixture.whenStable();

    expect(component.saveError()).toBe('Save failed');
  });

  it('toggle uses translated save error when setPreference fails with non-HTTP error', async () => {
    serviceMock.setPreference.mockReturnValueOnce(throwError(() => new Error('offline')));
    component.toggle('users-groups', '2fa');
    await fixture.whenStable();

    expect(component.saveError()).toBe('Save failed');
  });
});
