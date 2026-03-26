import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { RouteService } from './route-service';
import { Observable } from 'rxjs';
import type { PreferencesResponse } from '../models/preferences/PreferencesResponse';

@Injectable({
  providedIn: 'root',
})
export class SecurityPreferencesService {
  readonly #API_URL = RouteService.getBackendUrl('/user/preferences');
  readonly #http = inject(HttpClient);

  getAllPreferences(): Observable<PreferencesResponse> {
    return this.#http.get<PreferencesResponse>(this.#API_URL, {
      withCredentials: true,
    });
  }

  getDisabledKeys(): Observable<string[]> {
    return this.#http.get<string[]>(`${this.#API_URL}/disabled`, {
      withCredentials: true,
    });
  }

  /**
   * Boolean toggles: omit `value` or pass null.
   * DNS importance (domain-dns / imp*): pass REQUIRED | RECOMMENDED | OPTIONAL, or "" to clear override.
   */
  setPreference(
    section: string,
    preferenceKey: string,
    enabled: boolean,
    value?: string | null,
  ): Observable<void> {
    const body: { section: string; preferenceKey: string; enabled: boolean; value: string | null } = {
      section,
      preferenceKey,
      enabled,
      value: value === undefined || value === null ? null : value,
    };
    return this.#http.put<void>(this.#API_URL, body, { withCredentials: true });
  }
}
