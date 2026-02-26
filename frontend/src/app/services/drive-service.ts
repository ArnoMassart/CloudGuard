import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { RouteService } from './route-service';
import { Observable } from 'rxjs';
import { SharedDrivePageResponse } from '../models/drives/SharedDrivePageResponse';
import { SharedDriveOverviewResponse } from '../models/drives/SharedDriveOverviewResponse';

@Injectable({
  providedIn: 'root',
})
export class DriveService {
  readonly #API_URL = RouteService.getBackendUrl('/google/drives');
  readonly #http = inject(HttpClient);

  getDrives(size: number, pageToken?: string, query?: string): Observable<SharedDrivePageResponse> {
    let params = new HttpParams().set('size', size.toString());
    if (pageToken) params = params.set('pageToken', pageToken);
    if (query) params = params.set('query', query);

    return this.#http.get<SharedDrivePageResponse>(this.#API_URL, {
      params,
      withCredentials: true,
    });
  }

  getDrivesPageOverview(): Observable<SharedDriveOverviewResponse> {
    return this.#http.get<SharedDriveOverviewResponse>(`${this.#API_URL}/overview`, {
      withCredentials: true,
    });
  }

  refreshDriveCache(): Observable<string> {
    return this.#http.post(
      `${this.#API_URL}/refresh`,
      {
        withCredentials: true,
      },
      { responseType: 'text' }
    );
  }
}
