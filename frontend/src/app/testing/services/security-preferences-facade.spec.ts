import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom, of } from 'rxjs';
import { SecurityPreferencesFacade } from '../../services/security-preferences-facade';
import { SecurityPreferencesService } from '../../services/security-preferences-service';

describe('SecurityPreferencesFacade', () => {
  let facade: SecurityPreferencesFacade;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [SecurityPreferencesFacade, SecurityPreferencesService, provideHttpClient(), provideHttpClientTesting()],
    });
    facade = TestBed.inject(SecurityPreferencesFacade);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('loadDisabled$ stores keys in disabledKeys signal', async () => {
    const done = firstValueFrom(facade.loadDisabled$());
    const req = httpMock.expectOne((r) => r.url === '/api/user/preferences/disabled');
    req.flush(['s:k']);
    await done;
    expect(facade.disabledKeys().has('s:k')).toBe(true);
    expect(facade.isDisabled('s', 'k')).toBe(true);
  });

  it('loadDisabled$ clears on HTTP error', async () => {
    const done = firstValueFrom(facade.loadDisabled$());
    const req = httpMock.expectOne((r) => r.url === '/api/user/preferences/disabled');
    req.flush('', { status: 500, statusText: 'err' });
    await done;
    expect(facade.disabledKeys().size).toBe(0);
  });

  it('loadWithPrefs$ emits overview data after disabled load', async () => {
    const overview = of({ x: 1 });
    const dataPromise = firstValueFrom(facade.loadWithPrefs$(overview));
    httpMock.expectOne((r) => r.url === '/api/user/preferences/disabled').flush([]);
    expect(await dataPromise).toEqual({ x: 1 });
  });

  it('refresh updates disabledKeys from API', () => {
    const prefs = TestBed.inject(SecurityPreferencesService);
    const getDisabledKeysSpy = vi.spyOn(prefs, 'getDisabledKeys');
    facade.refresh();
    httpMock.expectOne((r) => r.url === '/api/user/preferences/disabled').flush(['a:b']);
    expect(getDisabledKeysSpy).toHaveBeenCalled();
    expect(facade.isDisabled('a', 'b')).toBe(true);
  });

  it('refresh clears keys on error', () => {
    facade.disabledKeys.set(new Set(['keep:me']));
    facade.refresh();
    httpMock.expectOne((r) => r.url === '/api/user/preferences/disabled').flush('', {
      status: 500,
      statusText: 'err',
    });
    expect(facade.disabledKeys().size).toBe(0);
  });
});
