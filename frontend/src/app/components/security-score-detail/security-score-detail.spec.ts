import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { provideTransloco, TranslocoLoader } from '@jsverse/transloco';
import { of } from 'rxjs';
import {
  SecurityScoreBreakdown,
  SecurityScoreFactor,
} from '../../models/password/PasswordSettings';
import { AppIcons } from '../../shared/AppIcons';
import { SecurityScoreDetailComponent, SecurityScoreDetailData } from './security-score-detail';

const I18N_MOCK = { 'score-detail.title': 'Score Details' };

class TestLoader implements TranslocoLoader {
  getTranslation() {
    return of(I18N_MOCK);
  }
}

describe('SecurityScoreDetail', () => {
  let component: SecurityScoreDetailComponent;
  let fixture: ComponentFixture<SecurityScoreDetailComponent>;

  // Mock data voor de dialoog
  const mockBreakdown: SecurityScoreBreakdown = {
    totalScore: 80,
    status: 'good',
    factors: [
      {
        title: 'Factor 1',
        description: 'Desc 1',
        score: 80,
        maxScore: 100,
        severity: 'success',
        muted: false,
      },
      {
        title: 'Factor 2',
        description: 'Desc 2',
        score: 40,
        maxScore: 100,
        severity: 'error',
        muted: true,
      },
    ],
  };

  const dialogDataMock: SecurityScoreDetailData = {
    breakdown: mockBreakdown,
    subtitle: 'Test Subtitle',
  };

  const dialogRefMock = {
    close: vi.fn(),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SecurityScoreDetailComponent],
      providers: [
        { provide: MAT_DIALOG_DATA, useValue: dialogDataMock },
        { provide: MatDialogRef, useValue: dialogRefMock },
        provideTransloco({
          config: { availableLangs: ['en'], defaultLang: 'en' },
          loader: TestLoader,
        }),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SecurityScoreDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should get breakdown from injected data', () => {
    expect(component.breakdown).toEqual(mockBreakdown);
  });

  describe('Status and Score Colors', () => {
    it('getStatusColor returns green for good status', () => {
      expect(component.getStatusColor()).toBe('#3abfad');
    });

    it('getStatusBgColor returns yellow for average status', () => {
      // Tijdelijk status aanpassen voor de test
      (component.breakdown as any).status = 'average';
      expect(component.getStatusBgColor()).toBe('rgba(214, 149, 24, 0.08)');
    });

    it('getScoreColor returns red for low scores', () => {
      (component.breakdown as any).totalScore = 40;
      expect(component.getScoreColor()).toBe('#e7000b');
    });
  });

  describe('Factor Helpers', () => {
    const successFactor = mockBreakdown.factors[0];
    const mutedFactor = mockBreakdown.factors[1];

    it('getFactorStyles returns muted styles when factor is muted', () => {
      const styles = component.getFactorStyles(mutedFactor);
      expect(styles.borderColor).toContain('rgba(107, 114, 128');
    });

    it('getFactorStyles returns success colors for success severity', () => {
      const styles = component.getFactorStyles(successFactor);
      expect(styles.borderColor).toBe('rgba(58, 191, 173, 0.3)');
    });

    it('getFactorIcon returns EyeOff for muted factors', () => {
      expect(component.getFactorIcon(mutedFactor)).toBe(AppIcons.EyeOff);
    });

    it('getFactorIcon returns CircleCheck for success factors', () => {
      expect(component.getFactorIcon(successFactor)).toBe(AppIcons.CircleCheck);
    });

    it('getProgressColor returns correct colors based on severity', () => {
      expect(component.getProgressColor(successFactor)).toBe('#3abfad');
      expect(component.getProgressColor(mutedFactor)).toBe('#9ca3af');

      const warnFactor = { severity: 'warning', muted: false } as SecurityScoreFactor;
      expect(component.getProgressColor(warnFactor)).toBe('#d69518');
    });
  });

  describe('CSS Class helpers', () => {
    it('returns gray text classes for muted factors', () => {
      const mutedFactor = mockBreakdown.factors[1];
      expect(component.factorTitleClass(mutedFactor)).toBe('text-gray-400');
      expect(component.factorDescClass(mutedFactor)).toBe('text-gray-400');
    });

    it('returns dark text classes for active factors', () => {
      const activeFactor = mockBreakdown.factors[0];
      expect(component.factorTitleClass(activeFactor)).toBe('text-gray-900');
      expect(component.factorDescClass(activeFactor)).toBe('text-gray-600');
    });
  });

  it('close() should call dialogRef.close', () => {
    component.close();
    expect(dialogRefMock.close).toHaveBeenCalled();
  });
});
