import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { RouteService } from './route-service';
import { PasswordSettings } from '../models/password/PasswordSettings';

@Injectable({
  providedIn: 'root',
})
export class PasswordSettingsService {
  readonly #API_URL = RouteService.getBackendUrl('/google/password-settings');
  readonly #http = inject(HttpClient);

  getPasswordSettings(): Observable<PasswordSettings> {
    return this.#http.get<PasswordSettings>(this.#API_URL, {
      withCredentials: true,
    });
  }

  refreshCache(): Observable<string> {
    return this.#http.post(
      `${this.#API_URL}/refresh`,
      {},
      { withCredentials: true, responseType: 'text' }
    );
  }
}
