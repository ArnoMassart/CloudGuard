import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { RouteService } from './route-service';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class SecurityPreferencesService {
  readonly #API_URL = RouteService.getBackendUrl('/user/preferences');
  readonly #http = inject(HttpClient);

  getAllPreferences(): Observable<Record<string, boolean>> {
    return this.#http.get<Record<string, boolean>>(this.#API_URL, {
      withCredentials: true,
    });
  }

  getDisabledKeys(): Observable<string[]> {
    return this.#http.get<string[]>(`${this.#API_URL}/disabled`, {
      withCredentials: true,
    });
  }

  setPreference(section: string, preferenceKey: string, enabled: boolean): Observable<void> {
    return this.#http.put<void>(
      this.#API_URL,
      { section, preferenceKey, enabled },
      { withCredentials: true }
    );
  }

}
