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
      maxWidth: '95vw',
    });
  }

  /**
   * Creates a minimal breakdown from a single score (for pages that don't have detailed factor data).
   */
  createSimpleBreakdown(score: number, subtitle: string): SecurityScoreBreakdown {
    const status = score === 100 ? 'Perfect' : score >= 75 ? 'Goed' : score > 50 ? 'Matig' : 'Slecht';
    const severity = score >= 75 ? 'success' : score >= 50 ? 'warning' : 'error';
    return {
      totalScore: score,
      status,
      factors: [
        {
          title: 'Algemene score',
          description: 'Gedetailleerde berekeningsfactoren zijn niet beschikbaar voor dit onderdeel.',
          weightPercent: 100,
          score,
          maxScore: 100,
          severity: severity as 'success' | 'warning' | 'error',
        },
      ],
    };
  }
}
