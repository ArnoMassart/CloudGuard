import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { AppIcons } from '../../shared/AppIcons';
import { LucideAngularModule, LucideIconData } from 'lucide-angular';
import { SecurityScoreBreakdown, SecurityScoreFactor } from '../../models/password/PasswordSettings';
import { TranslocoPipe } from '@jsverse/transloco';

export interface SecurityScoreDetailData {
  breakdown: SecurityScoreBreakdown;
  subtitle?: string;
}

@Component({
  selector: 'app-security-score-detail',
  imports: [MatButtonModule, MatDialogModule, LucideAngularModule, TranslocoPipe],
  templateUrl: './security-score-detail.html',
  styleUrl: './security-score-detail.css',
})
export class SecurityScoreDetailComponent {
  readonly Icons = AppIcons;
  readonly Math = Math;
  readonly data: SecurityScoreDetailData = inject(MAT_DIALOG_DATA);
  readonly dialogRef = inject(MatDialogRef<SecurityScoreDetailComponent>);

  get breakdown(): SecurityScoreBreakdown {
    return this.data.breakdown;
  }

  getStatusColor(): string {
    const s = this.breakdown.status;
    if (s === 'perfect' || s === 'good') return '#3abfad';
    if (s === 'average') return '#d69518';
    return '#e7000b';
  }

  getStatusBgColor(): string {
    const s = this.breakdown.status;
    if (s === 'perfect' || s === 'good') return 'rgba(58, 191, 173, 0.08)';
    if (s === 'average') return 'rgba(214, 149, 24, 0.08)';
    return 'rgba(231, 0, 11, 0.08)';
  }

  getScoreColor(): string {
    const n = this.breakdown.totalScore;
    if (n === 100 || n >= 75) return '#3abfad';
    if (n > 50) return '#d69518';
    return '#e7000b';
  }

  getFactorStyles(factor: SecurityScoreFactor): { borderColor: string; bg: string } {
    if (factor.muted) {
      return { borderColor: 'rgba(107, 114, 128, 0.35)', bg: 'rgba(156, 163, 175, 0.08)' };
    }
    const s = factor.severity;
    if (s === 'success') return { borderColor: 'rgba(58, 191, 173, 0.3)', bg: 'rgba(58, 191, 173, 0.06)' };
    if (s === 'warning') return { borderColor: 'rgba(214, 149, 24, 0.3)', bg: 'rgba(214, 149, 24, 0.06)' };
    return { borderColor: 'rgba(231, 0, 11, 0.3)', bg: 'rgba(231, 0, 11, 0.06)' };
  }

  getFactorIcon(factor: SecurityScoreFactor): LucideIconData {
    if (factor.muted) return this.Icons.EyeOff;
    const s = factor.severity;
    if (s === 'success') return this.Icons.CircleCheck;
    if (s === 'warning') return this.Icons.TriangleAlert;
    return this.Icons.CircleX;
  }

  getProgressColor(factor: SecurityScoreFactor): string {
    if (factor.muted) return '#9ca3af';
    const s = factor.severity;
    if (s === 'success') return '#3abfad';
    if (s === 'warning') return '#d69518';
    return '#FF3B3B';
  }

  factorTitleClass(factor: SecurityScoreFactor): string {
    return factor.muted ? 'text-gray-400' : 'text-gray-900';
  }

  factorDescClass(factor: SecurityScoreFactor): string {
    return factor.muted ? 'text-gray-400' : 'text-gray-600';
  }

  close(): void {
    this.dialogRef.close();
  }
}
