import { HttpClient } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { RouteService } from '../../services/route-service';
import { BehaviorSubject, Observable, of, ReplaySubject } from 'rxjs';
import { Router } from '@angular/router';
import { catchError, tap, timeout } from 'rxjs/operators';
import { User } from '../../models/User';
import { AuthService } from '@auth0/auth0-angular';

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

  readonly currentUser = signal<User | null>(null);

  constructor() {
    this.#checkServerSession();
  }

  #checkServerSession() {
    this.#http
      .get(`${this.#API_URL}/check-session`, {
        withCredentials: true,
        observe: 'response', // <--- Crucial: Tells Angular we want the full response object
      })
      .pipe(
        timeout(3000), // Bumped slightly for safety
        catchError((err) => {
          // If it's a 401, catchError triggers. We return 'null' to signal "not logged in"
          return of(null);
        })
      )
      .subscribe((res) => {
        // If res exists and status is 200-299, the user is authenticated
        const isAuthenticated = res !== null && res.ok;

        this.#loggedInStatus.next(isAuthenticated);
        this.#initializedStatus.next(true);

        if (isAuthenticated) {
          console.log('Session valid. Welcome back!');
          this.#fetchCurrentUser();
          if (this.#router.url === '/login' || this.#router.url === '/') {
            this.#router.navigate(['/']);
          }
        } else {
          const onCallbackPage = window.location.pathname.includes('/callback');
          const redirectPending = sessionStorage.getItem('auth0_redirect_pending') === '1';

          if (!onCallbackPage && !redirectPending) {
            console.warn('No session found. Redirecting to login.');
            this.#router.navigate(['/login']);
          }
        }
      });
  }

  #fetchCurrentUser(): void {
    this.#http
      .get<User>(`${this.#API_URL}/me`, { withCredentials: true })
      .pipe(catchError(() => of(null)))
      .subscribe((user) => {
        if (user) {
          this.currentUser.set(user);
        }
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
        returnTo: window.location.origin + '/login',
      },
    });
  }

  #completeLogout(): void {
    this.#loggedInStatus.next(false);
    this.currentUser.set(null);
    localStorage.clear();
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
        })
      );
  }
}
