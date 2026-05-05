import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-security-gauge',
  imports: [],
  templateUrl: './security-gauge.html',
  styleUrl: './security-gauge.css',
})
export class SecurityGauge {
  @Input() score: number | null = null;
  maxScore: number = 100;
  radius: number = 40;

  get totalLength(): number {
    return Math.PI * this.radius;
  }

  get dashArray(): string {
    if (this.score == null) {
      return `0 ${this.totalLength}`;
    }
    const progress = (this.score / this.maxScore) * this.totalLength;
    return `${progress} ${this.totalLength}`;
  }

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
