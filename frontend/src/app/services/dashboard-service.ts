import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { RouteService } from './route-service';
import { DashboardPageResponse } from '../models/dashboard/DashboardPageResponse';
import { Observable } from 'rxjs';
import { DashboardOverviewResponse } from '../models/dashboard/DashboardOverviewResponse';

@Injectable({
  providedIn: 'root',
})
export class DashboardService {
  readonly #API_URL = RouteService.getBackendUrl('/dashboard');
  readonly #http = inject(HttpClient);

  getDashboardData(): Observable<DashboardPageResponse> {
    return this.#http.get<DashboardPageResponse>(this.#API_URL, {
      withCredentials: true,
    });
  }

  getDashboardPageOverview(): Observable<DashboardOverviewResponse> {
    return this.#http.get<DashboardOverviewResponse>(`${this.#API_URL}/overview`, {
      withCredentials: true,
    });
  }
}
