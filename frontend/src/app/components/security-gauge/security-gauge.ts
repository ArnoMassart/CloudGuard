import { Component, Input, OnChanges, signal, SimpleChanges } from '@angular/core';

@Component({
  selector: 'app-security-gauge',
  imports: [],
  templateUrl: './security-gauge.html',
  styleUrl: './security-gauge.css',
})
export class SecurityGauge implements OnChanges {
  @Input() score: number | null = null;
  maxScore: number = 100;
  radius: number = 40;

  /** 0–1 arc fill; animates once when a numeric score first appears */
  private readonly arcFill = signal(0);
  private introPlayed = false;

  // The total length of the semi-circle arc
  get totalLength(): number {
    return Math.PI * this.radius;
  }

  // Calculate how much of that arc should be colored
  get dashArray(): string {
    if (this.score == null) {
      return `0 ${this.totalLength}`;
    }
    const progress = this.arcFill() * this.totalLength;
    return `${progress} ${this.totalLength}`;
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (!changes['score']) return;

    const s = this.score;
    if (s == null) {
      this.arcFill.set(0);
      this.introPlayed = false;
      return;
    }

    const target = Math.min(1, Math.max(0, s / this.maxScore));

    if (this.introPlayed) {
      this.arcFill.set(target);
      return;
    }

    this.introPlayed = true;
    this.arcFill.set(0);
    requestAnimationFrame(() => {
      requestAnimationFrame(() => {
        this.arcFill.set(target);
      });
    });
  }

  // Optional: Dynamic color based on score
  get strokeColor(): string {
    if (this.score == null) return '#e2e8f0';
    if (this.score >= 75) return '#22c55e';
    if (this.score > 50) return '#f59e0b';
    return '#ef4444';
  }

  get displayScore(): string {
    return this.score == null ? '—' : String(this.score);
  }
}
