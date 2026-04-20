import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTransloco } from '@jsverse/transloco';
import { of } from 'rxjs';
import { NotificationFeedbackService } from '../../../services/notification-feedback-service';
import { NotificationService } from '../../../services/notification-service';
import { TranslocoTestLoader } from '../../../testing/transloco-testing';
import { ReportsReactions } from './reports-reactions';

describe('ReportsReactions integration (notifications UI)', () => {
  let fixture: ComponentFixture<ReportsReactions>;

  beforeEach(async () => {
    TestBed.resetTestingModule();

    await TestBed.configureTestingModule({
      imports: [ReportsReactions],
      providers: [
        {
          provide: NotificationService,
          useValue: {
            getNotifications: () => of({ active: [], solved: [], lastNotificationSyncAt: null }),
            getNotificationDetails: () => of([]),
            syncNotifications: () => of(void 0),
          },
        },
        { provide: NotificationFeedbackService, useValue: { submitFeedback: () => of(void 0) } },
        provideTransloco({
          config: {
            availableLangs: ['en'],
            defaultLang: 'en',
            reRenderOnLangChange: true,
          },
          loader: TranslocoTestLoader,
        }),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ReportsReactions);
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('shows translated notifications page title and description', () => {
    const root = fixture.nativeElement as HTMLElement;
    expect(root.textContent).toContain('Notifications & Feedback');
    expect(root.textContent).toContain(
      'Real-time notifications about security issues and the possibility of feedback',
    );
  });

  it('shows empty state copy when there are no notifications', () => {
    const root = fixture.nativeElement as HTMLElement;
    expect(root.textContent).toContain('No notifications');
  });
});
