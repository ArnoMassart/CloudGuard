import { inject, Injectable } from '@angular/core';
import { RouteService } from './route-service';
import { HttpClient } from '@angular/common/http';
import { Organization } from '../models/org/Organization';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class OrgService {
  readonly #API_URL = RouteService.getBackendUrl('/org');
  readonly #http = inject(HttpClient);

  getAllOrgs(): Observable<Organization[]> {
    return this.#http.get<Organization[]>(`${this.#API_URL}/all`, {
      withCredentials: true,
    });
  }
}
