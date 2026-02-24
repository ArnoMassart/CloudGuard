import { Component, inject, signal } from '@angular/core';
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
export class App {
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

  // Deze methode wordt aangeroepen als de Splash 'emit' doet
  onSplashEnded() {
    // 1. Update the session storage so it doesn't show again this session
    sessionStorage.setItem('has_seen_splash', 'true');
    // 2. Update the local state to trigger the UI switch
    this.hasSeenSplash.set(true);
  }

  ngOnInit(): void {
    this.#router.events.subscribe(() => {
      this.showNavbar =
        !this.#router.url.includes('/login') &&
        !this.#router.url.includes('/forbidden') &&
        !this.#router.url.includes('/callback');
    });

    this.#auth0.error$.subscribe(() => {
      this.#router.navigate(['/forbidden']);
    });
  }
}
