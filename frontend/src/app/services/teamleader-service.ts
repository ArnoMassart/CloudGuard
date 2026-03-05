import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { RouteService } from './route-service';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class TeamleaderService {
  readonly #API_URL = RouteService.getBackendUrl('/teamleader');
  readonly #http = inject(HttpClient);

  getTestCompanyData(): Observable<any> {
    return this.#http.get(`${this.#API_URL}/test-company`);
  }
}
