import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { RouteService } from '../../services/route-service';
import { BehaviorSubject, Observable, ReplaySubject, tap } from 'rxjs';
import { Router } from '@angular/router';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  readonly #API_URL = RouteService.getBackendUrl('/auth');
  readonly #http = inject(HttpClient);
  readonly #router = inject(Router);

  #loggedInStatus = new ReplaySubject<boolean>(1);

  constructor() {
    this.#checkServerSession();
  }

  #checkServerSession() {
    this.#http.get(`${this.#API_URL}/check-session`, { withCredentials: true }).subscribe({
      next: () => this.#loggedInStatus.next(true),
      error: () => this.#loggedInStatus.next(false),
    });
  }

  logout(): Observable<void> {
    return this.#http
      .post<void>(
        `${this.#API_URL}/logout`,
        {},
        {
          withCredentials: true,
        },
      )
      .pipe(
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

  #hasToken(): boolean {
    return !!localStorage.getItem('user_is_logged_in');
  }
}
