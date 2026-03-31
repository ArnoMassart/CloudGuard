import { inject, Injectable, signal } from '@angular/core';
import { forkJoin, Observable, of } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';
import { SecurityPreferencesService } from './security-preferences-service';

@Injectable({
  providedIn: 'root',
})
export class SecurityPreferencesFacade {
  readonly #prefs = inject(SecurityPreferencesService);

  /** Full keys as returned by the API: `section:preferenceKey` */
  readonly disabledKeys = signal<ReadonlySet<string>>(new Set());

  /** True when the last refresh of disabled keys failed (e.g. after saving a preference). */
  readonly disabledKeysRefreshFailed = signal(false);

  loadDisabled$(): Observable<void> {
    return this.#prefs.getDisabledKeys().pipe(
      tap((keys) => {
        this.disabledKeys.set(new Set(keys));
        this.disabledKeysRefreshFailed.set(false);
      }),
      map(() => undefined),
      catchError(() => {
        this.disabledKeys.set(new Set());
        this.disabledKeysRefreshFailed.set(true);
        return of(undefined);
      }),
    );
  }

  /** Loads overview data alongside disabled preferences in a single forkJoin. */
  loadWithPrefs$<T>(overview$: Observable<T>): Observable<T> {
    return forkJoin({ data: overview$, _: this.loadDisabled$() }).pipe(
      map(({ data }) => data),
    );
  }

  isDisabled(section: string, preferenceKey: string): boolean {
    return this.disabledKeys().has(`${section}:${preferenceKey}`);
  }

  refresh(): void {
    this.#prefs.getDisabledKeys().subscribe({
      next: (keys) => {
        this.disabledKeys.set(new Set(keys));
        this.disabledKeysRefreshFailed.set(false);
      },
      error: () => {
        this.disabledKeys.set(new Set());
        this.disabledKeysRefreshFailed.set(true);
      },
    });
  }
}
