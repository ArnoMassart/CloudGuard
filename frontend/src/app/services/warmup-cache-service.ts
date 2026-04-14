import { inject, Injectable } from '@angular/core';
import { RouteService } from './route-service';
import { HttpClient } from '@angular/common/http';

@Injectable({
  providedIn: 'root',
})
export class WarmupCacheService {
  readonly #API_URL = RouteService.getBackendUrl('/cache-warmup');
  readonly #http = inject(HttpClient);

  triggerWarmup(): void {
    this.#http
      .post(
        `${this.#API_URL}`,
        {},
        {
          withCredentials: true,
        }
      )
      .subscribe({
        next: () => console.log('Backend is op de achtergrond de data aan het inladen...'),
        error: (err) => console.error('Warmup trigger mislukt', err),
      });
  }
}
