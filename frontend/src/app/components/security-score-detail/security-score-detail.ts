import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { AppIcons } from '../../shared/AppIcons';
import { LucideAngularModule, LucideIconData } from 'lucide-angular';
import { SecurityScoreBreakdown, SecurityScoreFactor } from '../../models/password/PasswordSettings';

export interface SecurityScoreDetailData {
  breakdown: SecurityScoreBreakdown;
  subtitle?: string;
}

@Component({
  selector: 'app-security-score-detail',
  imports: [MatButtonModule, MatDialogModule, LucideAngularModule],
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
    if (s === 'Goed') return '#3abfad';
    if (s === 'Zwak') return '#d38700';
    return '#e7000b';
  }

  getStatusBgColor(): string {
    const s = this.breakdown.status;
    if (s === 'Goed') return '#d8f2ef';
    if (s === 'Zwak') return '#fef9c2';
    return '#ffe2e2';
  }

  getScoreColor(): string {
    const n = this.breakdown.totalScore;
    if (n >= 75) return '#3abfad';
    if (n >= 50) return '#d38700';
    return '#e7000b';
  }

  getFactorStyles(factor: { severity: string }): { border: string; bg: string } {
    const s = factor.severity;
    if (s === 'success') return { border: '1px solid #3abfad', bg: '#d8f2ef' };
    if (s === 'warning') return { border: '1px solid #d38700', bg: '#fef9c2' };
    return { border: '1px solid #e7000b', bg: '#ffe2e2' };
  }

  getFactorIcon(factor: SecurityScoreFactor): LucideIconData {
    const s = factor.severity;
    if (s === 'success') return this.Icons.CircleCheck;
    if (s === 'warning') return this.Icons.TriangleAlert;
    return this.Icons.CircleX;
  }

  getProgressColor(factor: { severity: string }): string {
    const s = factor.severity;
    if (s === 'success') return '#3abfad';
    if (s === 'warning') return '#d38700';
    return '#e7000b';
  }

  close(): void {
    this.dialogRef.close();
  }
}
