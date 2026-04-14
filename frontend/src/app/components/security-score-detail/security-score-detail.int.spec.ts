import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { By } from '@angular/platform-browser';
import { provideTransloco, TranslocoLoader } from '@jsverse/transloco';
import {
  LucideAngularModule,
  Shield,
  X,
  CircleCheck,
  TriangleAlert,
  CircleX,
  EyeOff,
} from 'lucide-angular';
import { of } from 'rxjs';
import { SecurityScoreDetailComponent } from './security-score-detail';
import { SecurityScoreBreakdown } from '../../models/password/PasswordSettings';
import { provideTranslocoTesting } from '../../testing/transloco-testing'; // <--- Pas dit pad aan indien nodig

describe('SecurityScoreDetailComponent Integration', () => {
  let component: SecurityScoreDetailComponent;
  let fixture: ComponentFixture<SecurityScoreDetailComponent>;
  let dialogRefSpy: any;

  const mockBreakdown: SecurityScoreBreakdown = {
    totalScore: 45, // Bad score
    status: 'bad',
    factors: [
      {
        title: 'Factor Success',
        description: 'Description Success',
        score: 10,
        maxScore: 10,
        severity: 'success',
        muted: false,
      },
      {
        title: 'Factor Muted',
        description: 'Description Muted',
        score: 0,
        maxScore: 10,
        severity: 'error',
        muted: true,
      },
    ],
  };

  beforeEach(async () => {
    dialogRefSpy = { close: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [
        SecurityScoreDetailComponent,
        // We laden de echte iconen module om te testen of de juiste iconen in de DOM komen
        LucideAngularModule.pick({ Shield, X, CircleCheck, TriangleAlert, CircleX, EyeOff }),
      ],
      providers: [
        {
          provide: MAT_DIALOG_DATA,
          useValue: { breakdown: mockBreakdown, subtitle: 'test.subtitle' },
        },
        { provide: MatDialogRef, useValue: dialogRefSpy },
        provideTranslocoTesting(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SecurityScoreDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should render the score and status correctly in the UI', () => {
    const scoreElement = fixture.debugElement.query(By.css('.text-3xl')).nativeElement;
    const statusElement = fixture.debugElement.query(By.css('.inline-flex')).nativeElement;

    expect(scoreElement.textContent).toContain('45%');
    // Check of de kleur voor 'bad' status is toegepast (getScoreColor)
    expect(scoreElement.style.color).toBe('rgb(231, 0, 11)'); // #e7000b
    expect(statusElement.textContent).toContain('Bad');
  });

  it('should render all factors from the breakdown', () => {
    const factorElements = fixture.debugElement.queryAll(By.css('.rounded-lg.border-2'));
    expect(factorElements.length).toBe(2);
  });

  describe('Factor Rendering Logic', () => {
    it('should apply specific styles and icons for a success factor', () => {
      const factorEl = fixture.debugElement.queryAll(By.css('.rounded-lg.border-2'))[0];
      const titleEl = factorEl.query(By.css('h3')).nativeElement;

      // Check border kleur (success)
      expect(factorEl.nativeElement.style.borderColor).toBe('rgba(58, 191, 173, 0.3)');
      // Check title class (niet muted)
      expect(titleEl.classList).toContain('text-gray-900');
      // Check of muted tekst NIET aanwezig is
      expect(factorEl.nativeElement.textContent).not.toContain('Niet meegenomen');
    });

    it('should show specific "muted" UI elements when a factor is muted', () => {
      const factorEl = fixture.debugElement.queryAll(By.css('.rounded-lg.border-2'))[1];
      const titleEl = factorEl.query(By.css('h3')).nativeElement;
      const mutedNotice = factorEl.query(By.css('.italic'));

      // Check muted styling
      expect(titleEl.classList).toContain('text-gray-400');
      expect(mutedNotice).toBeTruthy();
      expect(mutedNotice.nativeElement.textContent).toContain(
        'Niet meegenomen in de security score'
      );
    });

    it('should calculate the progress bar width correctly', () => {
      const factorEl = fixture.debugElement.queryAll(By.css('.rounded-lg.border-2'))[0];
      const progressBar = factorEl.query(By.css('.h-full')).nativeElement;

      // Score is 10/10, dus width moet 100% zijn
      expect(progressBar.style.width).toBe('100%');
    });
  });

  describe('Interaction', () => {
    it('should call dialogRef.close when the X button is clicked', () => {
      // Verander "Sluiten" naar "Close" (of de exacte waarde uit je I18N_MOCK)
      const closeBtn = fixture.debugElement.query(By.css('button[aria-label="Close"]'));

      // Veiligheidscheck: als de knop null is, krijg je nu een duidelijkere foutmelding
      if (!closeBtn) {
        throw new Error('Sluitknop niet gevonden! Check of aria-label="Close" matcht met je mock.');
      }

      closeBtn.nativeElement.click();

      expect(dialogRefSpy.close).toHaveBeenCalled();
    });
  });
});
