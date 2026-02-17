import { CommonModule } from '@angular/common';
import { Component, EventEmitter, inject, Output, signal } from '@angular/core';
import { Shield, Code, LucideAngularModule, ShieldCheck } from 'lucide-angular';
import { CustomAuthService } from '../../core/auth/custom-auth-service';

@Component({
  selector: 'app-splash-screen',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './splash-screen.html',
  styleUrl: './splash-screen.css',
})
export class SplashScreen {
  readonly shieldIcon = ShieldCheck;
  readonly codeIcon = Code;

  #authService: CustomAuthService = inject(CustomAuthService);

  @Output() animationFinished = new EventEmitter<void>();

  progress = signal(0);
  #intervalId: any;

  ngOnInit() {
    const startTime = Date.now();
    const minDurationMs = 2000;

    this.startLoadingSimulation();

    this.#authService.isInitialized$.subscribe((isReady) => {
      if (isReady) {
        const elapsed = Date.now() - startTime;
        const remaining = Math.max(0, minDurationMs - elapsed);
        setTimeout(()=> this.finishLoading(), remaining);
      }
    });
  }

  startLoadingSimulation() {
    this.#intervalId = setInterval(() => {
      this.progress.update((current) => {
        if (current >= 90) {
          return 90; // Blijf hangen op 90% tot backend klaar is
        }
        return current + Math.random() * 3;
      });
    }, 20);
  }

  finishLoading() {
    setTimeout(() => {
      this.animationFinished.emit();
    }, 1300);
  }

  ngOnDestroy() {
    if (this.#intervalId) clearInterval(this.#intervalId);
  }
}
