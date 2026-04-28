import { CommonModule } from '@angular/common';
import { Component, EventEmitter, inject, OnDestroy, OnInit, Output, signal } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';
import { CustomAuthService } from '../../auth/custom-auth-service';
import { AppIcons } from '../../shared/AppIcons';
import { AuthService } from '@auth0/auth0-angular';
import { filter, Subscription, take } from 'rxjs';

@Component({
  selector: 'app-splash-screen',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './splash-screen.html',
  styleUrl: './splash-screen.css',
})
export class SplashScreen implements OnInit, OnDestroy {
  readonly Icons = AppIcons;

  readonly #authService: CustomAuthService = inject(CustomAuthService);
  readonly #auth0: AuthService = inject(AuthService);

  @Output() animationFinished = new EventEmitter<void>();

  progress = signal(0);
  #intervalId: any;
  #authSub?: Subscription;

  ngOnInit() {
    const startTime = Date.now();
    const minDurationMs = 2000;

    this.startLoadingSimulation();

    // 1. Luister naar jouw CustomAuthService (voor als ze WEL ingelogd zijn)
    this.#authSub = this.#authService.isLoggedIn$
      .pipe(
        filter((isReady) => isReady === true),
        take(1)
      )
      .subscribe(() => {
        this.triggerFinish(startTime, minDurationMs);
      });

    // 2. Luister naar Auth0 (voor als ze NIET ingelogd zijn bij de eerste laadactie)
    this.#auth0.isLoading$.subscribe((isLoading) => {
      if (!isLoading) {
        // Auth0 is klaar met de check. Is de gebruiker niet ingelogd? Haal dan het scherm weg!
        this.#auth0.isAuthenticated$.pipe(take(1)).subscribe((isAuth) => {
          if (!isAuth) {
            this.triggerFinish(startTime, minDurationMs);
          }
        });
      }
    });
  }

  // Helper functie om dubbele code te voorkomen
  private triggerFinish(startTime: number, minDurationMs: number) {
    const elapsed = Date.now() - startTime;
    const remaining = Math.max(0, minDurationMs - elapsed);
    setTimeout(() => this.finishLoading(), remaining);
  }

  startLoadingSimulation() {
    this.#intervalId = setInterval(() => {
      this.progress.update((current) => {
        if (current >= 99) {
          return 99; // Blijf hangen op 99% tot backend klaar is
        }
        return current + Math.random() * 3;
      });
    }, 20);
  }

  finishLoading() {
    setTimeout(() => {
      this.animationFinished.emit();
    }, 1200);
  }

  ngOnDestroy() {
    if (this.#intervalId) clearInterval(this.#intervalId);
  }
}
