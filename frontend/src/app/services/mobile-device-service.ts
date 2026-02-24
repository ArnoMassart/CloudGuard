import { inject, Injectable } from '@angular/core';
import { RouteService } from './route-service';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { MobileDevicePageResponse } from '../models/devices/MobileDevicePageResponse';
import { MobileDevicesOverviewResponse } from '../models/devices/MobileDevicesOverviewResponse';

@Injectable({
  providedIn: 'root',
})
export class MobileDeviceService {
  readonly #API_URL = RouteService.getBackendUrl('/google/devices');
  readonly #http = inject(HttpClient);

  getDevices(
    pageToken?: string,
    status?: string,
    type?: string,
    size: number = 5
  ): Observable<MobileDevicePageResponse> {
    let params = new HttpParams().set('size', size.toString());
    if (pageToken) params = params.set('pageToken', pageToken);
    if (status) params = params.set('status', status);
    if (type) params = params.set('deviceType', type);

    return this.#http.get<MobileDevicePageResponse>(this.#API_URL, {
      params: params,
      withCredentials: true,
    });
  }

  getUniqueDeviceTypes(): Observable<string[]> {
    return this.#http.get<string[]>(this.#API_URL + '/types', { withCredentials: true });
  }

  getMobileDevicesPageOverview(): Observable<MobileDevicesOverviewResponse> {
    return this.#http.get<MobileDevicesOverviewResponse>(this.#API_URL + '/overview', {
      withCredentials: true,
    });
  }
}
