import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { RouteService } from './route-service';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class ReportService {
  readonly #API_URL = RouteService.getBackendUrl('/report');
  readonly #http = inject(HttpClient);

  downloadSecurityRapport(): Observable<Blob> {
    return this.#http.get(this.#API_URL, {
      responseType: 'blob',
    });
  }
}
