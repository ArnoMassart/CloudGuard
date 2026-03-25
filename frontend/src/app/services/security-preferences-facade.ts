import { inject, Injectable, signal } from '@angular/core';
import { Observable, of } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';
import { SecurityPreferencesService } from './security-preferences-service';

@Injectable({
  providedIn: 'root',
})
export class SecurityPreferencesFacade {
  readonly #prefs = inject(SecurityPreferencesService);

  /** Full keys as returned by the API: `section:preferenceKey` */
  readonly disabledKeys = signal<ReadonlySet<string>>(new Set());

  loadDisabled$(): Observable<void> {
    return this.#prefs.getDisabledKeys().pipe(
      tap((keys) => this.disabledKeys.set(new Set(keys))),
      map(() => undefined),
      catchError(() => {
        this.disabledKeys.set(new Set());
        return of(undefined);
      }),
    );
  }

  isDisabled(section: string, preferenceKey: string): boolean {
    return this.disabledKeys().has(`${section}:${preferenceKey}`);
  }

  refresh(): void {
    this.#prefs.getDisabledKeys().subscribe({
      next: (keys) => this.disabledKeys.set(new Set(keys)),
      error: () => this.disabledKeys.set(new Set()),
    });
  }
}
