import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { BACKEND_URL } from '../../../env';
import { Observable } from 'rxjs';
import { RouteService } from './route-service';

@Injectable({
  providedIn: 'root',
})
export class ApiService {
  private http = inject(HttpClient);

  public getTest() {
    return this.http.get(RouteService.getBackendUrl('/test'), {
      responseType: 'text',
    }) as Observable<string>;
  }
}
