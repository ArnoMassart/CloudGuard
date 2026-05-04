import { HttpClient } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { RouteService } from '../services/route-service';
import { BehaviorSubject, Observable, of, ReplaySubject } from 'rxjs';
import { Router } from '@angular/router';
import { catchError, map, tap, timeout, finalize, switchMap } from 'rxjs/operators';
import { User } from '../models/users/User';
import { AuthService } from '@auth0/auth0-angular';
import { WarmupCacheService } from '../services/warmup-cache-service';
import { UserService } from '../services/user-service';

@Injectable({
  providedIn: 'root',
})
export class CustomAuthService {
  readonly #API_URL = RouteService.getBackendUrl('/auth');
  readonly #http = inject(HttpClient);

  // BehaviorSubject(false): app start altijd op login tot sessie bevestigd is
  readonly #loggedInStatus = new ReplaySubject<boolean>(1);
  // BehaviorSubject voor de @if check in de HTML
  readonly #initializedStatus = new BehaviorSubject<boolean>(false);

  readonly #auth0 = inject(AuthService);
  readonly #warmupCacheService = inject(WarmupCacheService);

  readonly isCloudmenStaff = signal(false);
  readonly userService = inject(UserService);

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
        switchMap((res)=>{
          if(!res.ok) {
            this.#loggedInStatus.next(false);
            this.#initializedStatus.next(true);
            return of(false);
          }
          return this.#fetchCurrentUser$().pipe(
            map((user) => {
              const ok = !!user;
              this.#loggedInStatus.next(ok);
              return ok;
            }),
          );
        }),
        catchError(() => {
          this.#loggedInStatus.next(false);
          this.#initializedStatus.next(true);
          return of(false);
        }),
        finalize(()=>{
          this.#isChecking = false;
        }),
      );
  }

  #fetchCurrentUser$(): Observable<User | null> {
    return this.#http.get<User>(`${this.#API_URL}/me`, { withCredentials: true }).pipe(
      catchError(() => of(null)),
      switchMap((user) => {
        if (!user) {
          this.currentUser.set(null);
          this.isCloudmenStaff.set(false);
          this.#initializedStatus.next(true);
          return of(null);
        }
        this.currentUser.set(user);
        return this.userService.isCloudmenStaff().pipe(
          tap((v) => this.isCloudmenStaff.set(v)),
          catchError(() => {
            this.isCloudmenStaff.set(false);
            return of(undefined);
          }),
          map(() => user),
          tap(() => this.#initializedStatus.next(true)),
        );
      }),
    );
  }

  fetchCurrentUser(): void {
    this.#fetchCurrentUser$().subscribe();
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
          this.fetchCurrentUser();
        }),
      );
  }
}
