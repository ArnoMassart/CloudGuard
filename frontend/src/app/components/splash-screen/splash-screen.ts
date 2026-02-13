import { CommonModule } from '@angular/common';
import { Component, EventEmitter, inject, Output, signal } from '@angular/core';
import { Shield, Code, LucideAngularModule } from 'lucide-angular';
import { AuthService } from '../../core/auth/auth-service';

@Component({
  selector: 'app-splash-screen',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './splash-screen.html',
  styleUrl: './splash-screen.css',
})
export class SplashScreen {
  readonly shieldIcon = Shield;
  readonly codeIcon = Code;

  #authService: AuthService = inject(AuthService);

  @Output() animationFinished = new EventEmitter<void>();

  progress = signal(0);
  #intervalId: any;

  ngOnInit() {
    this.startLoadingSimulation();

    this.#authService.isInitialized$.subscribe((isReady) => {
      if (isReady) {
        this.finishLoading();
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
