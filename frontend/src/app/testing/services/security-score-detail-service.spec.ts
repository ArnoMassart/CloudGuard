import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { SecurityScoreDetailComponent } from '../../components/security-score-detail/security-score-detail';
import { SecurityScoreBreakdown } from '../../models/password/PasswordSettings';
import { SecurityScoreDetailService } from '../../services/security-score-detail.service';

describe('SecurityScoreDetailService', () => {
  let service: SecurityScoreDetailService;

  // Mock voor MatDialog
  let dialogMock: {
    open: ReturnType<typeof vi.fn>;
  };

  beforeEach(() => {
    dialogMock = {
      open: vi.fn(),
    };

    TestBed.configureTestingModule({
      providers: [SecurityScoreDetailService, { provide: MatDialog, useValue: dialogMock }],
    });

    service = TestBed.inject(SecurityScoreDetailService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('open', () => {
    it('should open the dialog with correct component and configuration', () => {
      const mockBreakdown = {
        totalScore: 85,
        status: 'good',
        factors: [],
      } as unknown as SecurityScoreBreakdown;
      const subtitle = 'Test Subtitle';

      service.open(mockBreakdown, subtitle);

      expect(dialogMock.open).toHaveBeenCalledWith(SecurityScoreDetailComponent, {
        data: { breakdown: mockBreakdown, subtitle },
        panelClass: 'security-score-detail-panel',
        width: '32rem',
        maxWidth: '95vw',
      });
    });

    it('should open the dialog even without a subtitle', () => {
      const mockBreakdown = {
        totalScore: 100,
        status: 'perfect',
        factors: [],
      } as unknown as SecurityScoreBreakdown;

      service.open(mockBreakdown);

      expect(dialogMock.open).toHaveBeenCalledWith(
        SecurityScoreDetailComponent,
        expect.objectContaining({
          data: { breakdown: mockBreakdown, subtitle: undefined },
        })
      );
    });
  });

  describe('createSimpleBreakdown', () => {
    it('should create a "perfect" status for score 100', () => {
      const result = service.createSimpleBreakdown(100, 'test');
      expect(result.status).toBe('perfect');
      expect(result.factors[0].severity).toBe('success');
    });

    it('should create a "good" status for score 75', () => {
      const result = service.createSimpleBreakdown(75, 'test');
      expect(result.status).toBe('good');
      expect(result.factors[0].severity).toBe('success');
    });

    it('should create an "average" status and "warning" severity for score 60', () => {
      const result = service.createSimpleBreakdown(60, 'test');
      expect(result.status).toBe('average');
      expect(result.factors[0].severity).toBe('warning');
    });

    it('should create a "bad" status and "error" severity for score 40', () => {
      const result = service.createSimpleBreakdown(40, 'test');
      expect(result.status).toBe('bad');
      expect(result.factors[0].severity).toBe('error');
    });

    it('should map the score and subtitle correctly to the breakdown object', () => {
      const score = 82;
      const subtitle = 'devices';
      const result = service.createSimpleBreakdown(score, subtitle);

      expect(result.totalScore).toBe(score);
      expect(result.factors[0].score).toBe(score);
      expect(result.factors[0].title).toBe('overall-score');
      // Subtitle wordt in de service momenteel niet in het object zelf opgeslagen,
      // maar de logica gebruikt het voor de status/severity bepaling.
    });
  });
});
