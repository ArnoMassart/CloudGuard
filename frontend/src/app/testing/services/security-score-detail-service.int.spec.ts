import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { SecurityScoreDetailComponent } from '../../components/security-score-detail/security-score-detail';
import { SecurityScoreBreakdown } from '../../models/password/PasswordSettings';
import { SecurityScoreDetailService } from '../../services/security-score-detail.service';

describe('SecurityScoreDetailService', () => {
  let service: SecurityScoreDetailService;

  // Mock voor MatDialog
  const matDialogMock = {
    open: vi.fn(),
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [SecurityScoreDetailService, { provide: MatDialog, useValue: matDialogMock }],
    });

    service = TestBed.inject(SecurityScoreDetailService);

    // Reset de mock voor elke test
    vi.clearAllMocks();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('open()', () => {
    it('should open the dialog with the correct component and configuration', () => {
      const mockBreakdown: SecurityScoreBreakdown = {
        totalScore: 85,
        status: 'good',
        factors: [],
      };
      const subtitle = 'Test Subtitle';

      service.open(mockBreakdown, subtitle);

      expect(matDialogMock.open).toHaveBeenCalledWith(SecurityScoreDetailComponent, {
        data: { breakdown: mockBreakdown, subtitle },
        panelClass: 'security-score-detail-panel',
        width: '32rem',
        maxWidth: '95vw',
      });
    });
  });

  describe('createSimpleBreakdown()', () => {
    it('should return a "perfect" status and "success" severity for score 100', () => {
      const result = service.createSimpleBreakdown(100, 'any');

      expect(result.status).toBe('perfect');
      expect(result.factors[0].severity).toBe('success');
    });

    it('should return a "good" status and "success" severity for score 75', () => {
      const result = service.createSimpleBreakdown(75, 'any');

      expect(result.status).toBe('good');
      expect(result.factors[0].severity).toBe('success');
    });

    it('should return an "average" status and "warning" severity for score 60', () => {
      const result = service.createSimpleBreakdown(60, 'any');

      expect(result.status).toBe('average');
      expect(result.factors[0].severity).toBe('warning');
    });

    it('should return a "bad" status and "warning" severity for score 50', () => {
      // Grenswaarde check: > 50 is average, <= 50 is bad.
      // Maar >= 50 is nog wel severity warning.
      const result = service.createSimpleBreakdown(50, 'any');

      expect(result.status).toBe('bad');
      expect(result.factors[0].severity).toBe('warning');
    });

    it('should return a "bad" status and "error" severity for score 49', () => {
      const result = service.createSimpleBreakdown(49, 'any');

      expect(result.status).toBe('bad');
      expect(result.factors[0].severity).toBe('error');
    });

    it('should correctly map the score and title', () => {
      const score = 82;
      const result = service.createSimpleBreakdown(score, 'devices');

      expect(result.totalScore).toBe(82);
      expect(result.factors[0].score).toBe(82);
      expect(result.factors[0].title).toBe('overall-score');
    });
  });
});
