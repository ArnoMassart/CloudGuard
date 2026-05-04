import { Component, inject, OnInit, signal } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';
import { Navbar } from './navbar/navbar';
import { CustomAuthService } from './auth/custom-auth-service';
import { AuthService } from '@auth0/auth0-angular';
import { CommonModule } from '@angular/common';
import { SplashScreen } from './components/splash-screen/splash-screen';

type NavBarRoute = {
  route: string;
  preciseCheck: boolean;
};

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Navbar, CommonModule, SplashScreen],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App implements OnInit {
  protected readonly title = signal('CloudGuard');
  readonly #router = inject(Router);
  readonly #auth0 = inject(AuthService);
  readonly authService = inject(CustomAuthService);

  showNavbar: boolean = true;
  showSplash = true;

  hasSeenSplash = signal(
    sessionStorage.getItem('has_seen_splash') === 'true' ||
      window.location.pathname.includes('/callback')
  );

  onSplashEnded() {
    sessionStorage.setItem('has_seen_splash', 'true');
    this.hasSeenSplash.set(true);
  }

  readonly noNavBarRoutes: NavBarRoute[] = [
    { route: '/login', preciseCheck: false },
    { route: '/callback', preciseCheck: false },
    { route: '/forbidden', preciseCheck: true },
    { route: '/access-denied', preciseCheck: true },
    { route: '/no-access', preciseCheck: true },
    { route: '/server-error', preciseCheck: true },
    { route: '/request-access', preciseCheck: true },
    { route: '/no-organization', preciseCheck: true },
    { route: '/request-role', preciseCheck: true },
    { route: '/setup', preciseCheck: true },
    { route: '/no-page-access', preciseCheck: true },
    { route: '/denied', preciseCheck: true },
  ];

  ngOnInit(): void {
    this.#router.events.subscribe(() => {
      this.showNavbar = !this.noNavBarRoutes.some((item) =>
        item.preciseCheck ? this.checkForPreciseRoute(item.route) : this.checkForRoute(item.route)
      );
    });

    this.#auth0.error$.subscribe(() => {
      this.#router.navigate(['/forbidden']);
    });
  }

  checkForRoute(route: string): boolean {
    return this.#router.url.includes(route);
  }

  checkForPreciseRoute(route: string): boolean {
    return this.#router.url === route;
  }
}
