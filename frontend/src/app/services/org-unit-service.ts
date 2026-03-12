import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { RouteService } from './route-service';
import { OrgUnitNode } from '../pages/security-section/organizational-units/organizational-units';
import { OrgUnitPolicyDto } from '../models/org-unit/OrgUnitPolicyDto';

@Injectable({
  providedIn: 'root',
})
export class OrgUnitService {
  readonly #API_URL = RouteService.getBackendUrl('/google/org-units');
  readonly #http = inject(HttpClient);

  getOrgUnitTree(): Observable<OrgUnitNode> {
    return this.#http.get<OrgUnitNode>(`${this.#API_URL}`, {
      withCredentials: true,
    });
  }

  getPoliciesForOrgUnit(orgUnitPath: string): Observable<OrgUnitPolicyDto[]> {
    return this.#http.get<OrgUnitPolicyDto[]>(`${this.#API_URL}/policies`, {
      params: { orgUnitPath },
      withCredentials: true,
    });
  }

  refreshOrgUnitsCache(): Observable<string> {
    return this.#http.post(
      `${this.#API_URL}/refresh`,
      {
        withCredentials: true,
      },
      { responseType: 'text' }
    );
  }
}
