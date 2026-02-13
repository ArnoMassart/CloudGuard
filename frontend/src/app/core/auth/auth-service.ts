import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { RouteService } from '../../services/route-service';
import { BehaviorSubject, Observable, ReplaySubject, of } from 'rxjs';
import { Router } from '@angular/router';
import { catchError, tap, timeout } from 'rxjs/operators';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  readonly #API_URL = RouteService.getBackendUrl('/auth');
  readonly #http = inject(HttpClient);
  readonly #router = inject(Router);

  // ReplaySubject zorgt dat de Guard wacht op de waarde
  readonly #loggedInStatus = new ReplaySubject<boolean>(1);
  // BehaviorSubject voor de @if check in de HTML
  readonly #initializedStatus = new BehaviorSubject<boolean>(false);

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
        }),
      )
      .subscribe((res) => {
        // If res exists and status is 200-299, the user is authenticated
        const isAuthenticated = res !== null && res.ok;

        this.#loggedInStatus.next(isAuthenticated);
        this.#initializedStatus.next(true);

        if (isAuthenticated) {
          console.log('Session valid. Welcome back!');
          // If they are at login or the root, move them into the app
          if (this.#router.url === '/login' || this.#router.url === '/') {
            this.#router.navigate(['/']);
          }
        } else {
          console.warn('No session found. Redirecting to login.');
          this.#router.navigate(['/login']);
        }
      });
  }

  logout(): Observable<void> {
    return this.#http.post<void>(`${this.#API_URL}/logout`, {}, { withCredentials: true }).pipe(
      tap(() => {
        this.#completeLogout();
      }),
    );
  }

  #completeLogout(): void {
    this.#loggedInStatus.next(false);
    localStorage.clear();
    sessionStorage.clear();
    this.#router.navigate(['/login']);
  }

  get isLoggedIn$(): Observable<boolean> {
    return this.#loggedInStatus.asObservable();
  }

  get isInitialized$(): Observable<boolean> {
    return this.#initializedStatus.asObservable();
  }
}
