import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { RouteService } from './route-service';
import { catchError, map, Observable, of, shareReplay } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class TeamleaderService {
  readonly #API_URL = RouteService.getBackendUrl('/teamleader');
  readonly #http = inject(HttpClient);

  #accessCache$: Observable<boolean> | null = null;

  verifyCloudGuardAccess(): Observable<boolean> {
    if (!this.#accessCache$) {
      this.#accessCache$ = this.#http.get<{ hasAccess: boolean }>(`${this.#API_URL}/check`).pipe(
        map((response) => response.hasAccess),
        catchError(() => {
          this.clearCache();
          return of(false);
        }),
        shareReplay(1)
      );
    }

    return this.#accessCache$;
  }

  clearCache() {
    this.#accessCache$ = null;
  }
}
