import { Injectable, inject } from '@angular/core';
import { RouteService } from './route-service';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AdminSecurityKeysResponse } from '../models/admin-security-keys/AdminSecurityKeysResponse';

@Injectable({
  providedIn: 'root',
})
export class AdminSecurityKeysService {
  readonly #API_URL = RouteService.getBackendUrl('/google/admin-security-keys');
  readonly #http = inject(HttpClient);

  getAdminsWithSecurityKeys(): Observable<AdminSecurityKeysResponse> {
    return this.#http.get<AdminSecurityKeysResponse>(this.#API_URL, {
      withCredentials: true,
    });
  }
}
