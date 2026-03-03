import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { RouteService } from './route-service';
import { Observable } from 'rxjs';
import { LicensePageResponse } from '../models/licenses/LicensePageResponse';

@Injectable({
  providedIn: 'root',
})
export class LicenseService {
  readonly #API_URL = RouteService.getBackendUrl('/google/license');
  readonly #http = inject(HttpClient);

  getLicenses(): Observable<LicensePageResponse> {
    return this.#http.get<LicensePageResponse>(this.#API_URL, {
      withCredentials: true,
    });
  }
}
