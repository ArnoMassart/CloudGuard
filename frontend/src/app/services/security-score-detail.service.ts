import { inject, Injectable } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { SecurityScoreDetailComponent } from '../components/security-score-detail/security-score-detail';
import { SecurityScoreBreakdown } from '../models/password/PasswordSettings';

@Injectable({
  providedIn: 'root',
})
export class SecurityScoreDetailService {
  readonly #dialog = inject(MatDialog);

  open(breakdown: SecurityScoreBreakdown, subtitle?: string): void {
    this.#dialog.open(SecurityScoreDetailComponent, {
      data: { breakdown, subtitle },
      panelClass: 'security-score-detail-panel',
      width: '32rem',
      maxWidth: '95vw',
    });
  }

  /**
   * Creates a minimal breakdown from a single score (for pages that don't have detailed factor data).
   */
  createSimpleBreakdown(score: number, subtitle: string): SecurityScoreBreakdown {
    const status =
      score === 100 ? 'perfect' : score >= 75 ? 'good' : score > 50 ? 'average' : 'bad';
    const severity = score >= 75 ? 'success' : score >= 50 ? 'warning' : 'error';
    return {
      totalScore: score,
      status,
      factors: [
        {
          title: 'overall-score',
          description: 'score-detail.description.',
          score,
          maxScore: 100,
          severity: severity as 'success' | 'warning' | 'error',
          muted: false,
        },
      ],
    };
  }
}
