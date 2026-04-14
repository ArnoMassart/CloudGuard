import { Component, inject, OnInit, signal } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';
import { Navbar } from './navbar/navbar';
import { CustomAuthService } from './auth/custom-auth-service';
import { AuthService } from '@auth0/auth0-angular';
import { CommonModule } from '@angular/common';
import { SplashScreen } from './components/splash-screen/splash-screen';

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
      globalThis.location.pathname.includes('/callback')
  );

  onSplashEnded() {
    sessionStorage.setItem('has_seen_splash', 'true');
    this.hasSeenSplash.set(true);
  }

  ngOnInit(): void {
    this.#router.events.subscribe(() => {
      this.showNavbar =
        !this.#router.url.includes('/login') &&
        !this.#router.url.includes('/forbidden') &&
        !this.#router.url.includes('/access-denied') &&
        !this.#router.url.includes('/no-access') &&
        !this.#router.url.includes('/server-error') &&
        !this.#router.url.includes('/request-access') &&
        !this.#router.url.includes('/callback');
    });

    this.#auth0.error$.subscribe(() => {
      this.#router.navigate(['/forbidden']);
    });
  }
}
