import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-security-gauge',
  imports: [],
  templateUrl: './security-gauge.html',
  styleUrl: './security-gauge.css',
})
export class SecurityGauge {
  @Input() score: number = 73; // This can come from an API
  maxScore: number = 100;
  radius: number = 40;

  // The total length of the semi-circle arc
  get totalLength(): number {
    return Math.PI * this.radius; // Approximately 125.66
  }

  // Calculate how much of that arc should be colored
  get dashArray(): string {
    const progress = (this.score / this.maxScore) * this.totalLength;
    return `${progress} ${this.totalLength}`;
  }

  // Optional: Dynamic color based on score
  get strokeColor(): string {
    if (this.score >= 75) return '#22c55e'; // Green-500
    if (this.score > 50) return '#f59e0b'; // Amber-500
    return '#ef4444'; // Red-500
  }
}
