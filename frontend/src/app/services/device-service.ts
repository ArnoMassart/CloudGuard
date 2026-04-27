import { inject, Injectable } from '@angular/core';
import { RouteService } from './route-service';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { DevicePageResponse } from '../models/devices/DevicePageResponse';
import { DevicesOverviewResponse } from '../models/devices/DevicesOverviewResponse';

const ALL_DEVICE_TYPES = 'all' as const;

@Injectable({
  providedIn: 'root',
})
export class DeviceService {
  readonly #API_URL = RouteService.getBackendUrl('/google/devices');
  readonly #http = inject(HttpClient);

  getDevices(
    pageToken?: string,
    status?: string,
    type?: string,
    size: number = 5,
  ): Observable<DevicePageResponse> {
    let params = new HttpParams().set('size', size.toString());
    if (pageToken) params = params.set('pageToken', pageToken);
    if (status) params = params.set('status', status);
    if (type && type !== ALL_DEVICE_TYPES) {
      params = params.set('deviceType', type);
    }

    return this.#http.get<DevicePageResponse>(this.#API_URL, {
      params: params,
      withCredentials: true,
    });
  }

  getUniqueDeviceTypes(): Observable<string[]> {
    return this.#http.get<string[]>(this.#API_URL + '/types', { withCredentials: true });
  }

  getDevicesPageOverview(): Observable<DevicesOverviewResponse> {
    return this.#http.get<DevicesOverviewResponse>(this.#API_URL + '/overview', {
      withCredentials: true,
    });
  }

  refreshDeviceCache(): Observable<string> {
    return this.#http.post(
      `${this.#API_URL}/refresh`,
      {
        withCredentials: true,
      },
      { responseType: 'text' },
    );
  }
}
