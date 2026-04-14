import { HttpClient } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { RouteService } from '../services/route-service';
import { BehaviorSubject, Observable, of, ReplaySubject } from 'rxjs';
import { Router } from '@angular/router';
import { catchError, map, tap, timeout } from 'rxjs/operators';
import { User } from '../models/users/User';
import { AuthService } from '@auth0/auth0-angular';
import { WarmupCacheService } from '../services/warmup-cache-service';

@Injectable({
  providedIn: 'root',
})
export class CustomAuthService {
  readonly #API_URL = RouteService.getBackendUrl('/auth');
  readonly #http = inject(HttpClient);
  readonly #router = inject(Router);

  // BehaviorSubject(false): app start altijd op login tot sessie bevestigd is
  readonly #loggedInStatus = new ReplaySubject<boolean>(1);
  // BehaviorSubject voor de @if check in de HTML
  readonly #initializedStatus = new BehaviorSubject<boolean>(false);

  readonly #auth0 = inject(AuthService);
  readonly #warmupCacheService = inject(WarmupCacheService);

  readonly currentUser = signal<User | null>(null);

  #isChecking = false;

  checkServerSession(): Observable<boolean> {
    if (this.currentUser()) return of(true); // Al ingelogd? Klaar.
    if (this.#isChecking) return this.isLoggedIn$; // Al bezig? Wacht op resultaat.

    this.#isChecking = true;
    return this.#http
      .get(`${this.#API_URL}/check-session`, {
        withCredentials: true,
        observe: 'response',
      })
      .pipe(
        timeout(3000),
        map((res) => res.ok),
        catchError(() => of(false)),
        tap((isAuthenticated) => {
          this.#loggedInStatus.next(isAuthenticated);
          this.#isChecking = false;

          if (isAuthenticated) {
            this.#fetchCurrentUser();
          } else {
            this.#initializedStatus.next(true);
          }
        })
      );
  }

  #fetchCurrentUser(): void {
    this.#http
      .get<User>(`${this.#API_URL}/me`, { withCredentials: true })
      .pipe(catchError(() => of(null)))
      .subscribe((user) => {
        if (user) {
          this.currentUser.set(user);
        }
        // BELANGRIJK: Pas als de data in de signal zit, mag de guard doorlopen!
        this.#initializedStatus.next(true);
      });
  }

  logout(): void {
    this.#http.post(`${this.#API_URL}/logout`, {}, { withCredentials: true }).subscribe({
      next: () => {
        this.#performAuth0Logout();
      },
      error: () => {
        this.#performAuth0Logout();
      },
    });
  }

  #performAuth0Logout() {
    this.#completeLogout();

    this.#auth0.logout({
      logoutParams: {
        returnTo: globalThis.location.origin + '/login',
      },
    });
  }

  #completeLogout(): void {
    this.#loggedInStatus.next(false);
    this.currentUser.set(null);
    localStorage.clear();
    sessionStorage.removeItem('auth0_redirect_pending');
    sessionStorage.removeItem('user-group-section');
  }

  get isLoggedIn$(): Observable<boolean> {
    return this.#loggedInStatus.asObservable();
  }

  get isInitialized$(): Observable<boolean> {
    return this.#initializedStatus.asObservable();
  }

  /**
   * 1. Receives the ID Token from Auth0/Google
   * 2. Sends it to Backend
   * 3. Backend validates & sets HttpOnly Cookie
   * 4. Frontend updates state
   */
  loginWithGoogle(idToken: string): Observable<User> {
    return this.#http
      .post<User>(`${this.#API_URL}/login`, { token: idToken }, { withCredentials: true })
      .pipe(
        tap((user) => {
          this.currentUser.set(user);
          this.#loggedInStatus.next(true);
          this.#initializedStatus.next(true);
          this.#fetchCurrentUser();

          // this.#warmupCacheService.triggerWarmup();
        })
      );
  }
}
