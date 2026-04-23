import { inject, Injectable } from '@angular/core';
import { RouteService } from './route-service';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Organization } from '../models/org/Organization';
import { Observable } from 'rxjs';
import { DatabaseOrgResponse } from '../models/org/DatabaseOrgResponse';

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

  getAllOrgsPaged(
    size: number,
    pageToken?: string,
    query?: string
  ): Observable<DatabaseOrgResponse> {
    let params = new HttpParams().set('size', size.toString());

    if (pageToken) params = params.set('pageToken', pageToken);
    if (query) params = params.set('query', query);

    return this.#http.get<DatabaseOrgResponse>(`${this.#API_URL}/all-paged`, {
      params: params,
      withCredentials: true,
    });
  }
}
