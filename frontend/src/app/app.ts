import { Component, inject, signal } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';
import { Navbar } from './navbar/navbar';
import { CustomAuthService } from './core/auth/custom-auth-service';
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
  readonly authService = inject(CustomAuthService);

  showNavbar: boolean = true;
  showSplash = true;

  hasSeenSplash = signal(sessionStorage.getItem('has_seen_splash') === 'true');

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
        !this.#router.url.includes('/login') && !this.#router.url.includes('/forbidden');
    });
  }
}
