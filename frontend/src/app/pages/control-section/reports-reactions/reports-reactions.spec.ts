import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTransloco, TranslocoLoader } from '@jsverse/transloco';
import { of, throwError } from 'rxjs';
import { Notification } from '../../../models/notification/Notification';
import { DismissedNotificationService } from '../../../services/dismissed-notification-service';
import { NotificationFeedbackService } from '../../../services/notification-feedback-service';
import { NotificationService } from '../../../services/notification-service';
import { ReportsReactions } from './reports-reactions';

const FB_I18N: Record<string, string> = {
  'feedback.description': 'Desc',
  total: 'Total',
  'feedback.new': 'New',
  'feedback.critical': 'Critical',
  'feedback.processing': 'Processing',
  'critical-warning': 'crit',
  'critical-warnings': 'crits',
  'feedback.warning': 'Warn',
  'feedback.loading': 'Loading',
  'feedback.none': 'None',
  'feedback.severity.all': 'All',
  'feedback.severity.dismissed': 'Dismissed',
  'feedback.severity.processing': 'Proc',
  'feedback.severity.none': 'None sev',
  'critical-2': 'Critical',
  'warning-2': 'Warning',
  Info: 'Info',
  dismissed: 'Dismissed',
  today: 'Today',
  'hide-details': 'Hide',
  'view-details': 'View',
  'loading-details': 'Loading det',
  'feedback.items': 'Items',
  'feedback.items.none': 'No items',
  'feedback.items.recommended': 'Rec',
  'feedback.your-feedback': 'Yours',
  'feedback.describe-feedback': 'Describe',
  send: 'Send',
  sent: 'Sent',
  cancel: 'Cancel',
  'feedback.automatic': 'Auto',
  'feedback.send': 'Send fb',
  busy: 'Busy',
  'ignore-30-days': 'Ignore',
  resolved: 'Resolved',
  repairing: 'Repair',
  all: 'All',
  'notifications-feedback': 'Notifications',
  'to-admin-console': 'Admin',
  info: 'Info',
  refresh: 'Refresh',
};

class ReportsReactionsTranslocoLoader implements TranslocoLoader {
  getTranslation() {
    return of(FB_I18N);
  }
}

function makeNotification(overrides?: Partial<Notification>): Notification {
  return {
    id: 'n1',
    severity: 'warning',
    title: 'Title',
    description: 'Desc',
    notificationType: 'user-control',
    source: 'src',
    sourceLabel: 'Label',
    sourceRoute: '/r',
    hasReported: false,
    supportsDetails: true,
    ...overrides,
  };
}

describe('ReportsReactions', () => {
  let component: ReportsReactions;
  let fixture: ComponentFixture<ReportsReactions>;
  let notificationServiceMock: {
    getNotificationsAndDismissed: ReturnType<typeof vi.fn>;
    getNotificationDetails: ReturnType<typeof vi.fn>;
  };
  let dismissedServiceMock: {
    markAsDismissed: ReturnType<typeof vi.fn>;
    unDismiss: ReturnType<typeof vi.fn>;
  };
  let feedbackServiceMock: { submitFeedback: ReturnType<typeof vi.fn> };

  const activeList = [
    makeNotification({ id: 'a', severity: 'critical' }),
    makeNotification({ id: 'b', severity: 'warning', supportsDetails: false }),
    makeNotification({ id: 'c', severity: 'info', hasReported: true }),
  ];

  beforeEach(async () => {
    notificationServiceMock = {
      getNotificationsAndDismissed: vi.fn(() => of({ active: activeList, dismissed: [] })),
      getNotificationDetails: vi.fn(() => of(['line one'])),
    };
    dismissedServiceMock = {
      markAsDismissed: vi.fn(() => of(void 0)),
      unDismiss: vi.fn(() => of(void 0)),
    };
    feedbackServiceMock = {
      submitFeedback: vi.fn(() => of(void 0)),
    };

    await TestBed.configureTestingModule({
      imports: [ReportsReactions],
      providers: [
        { provide: NotificationService, useValue: notificationServiceMock },
        { provide: DismissedNotificationService, useValue: dismissedServiceMock },
        { provide: NotificationFeedbackService, useValue: feedbackServiceMock },
        provideTransloco({
          config: {
            availableLangs: ['en'],
            defaultLang: 'en',
            reRenderOnLangChange: true,
          },
          loader: ReportsReactionsTranslocoLoader,
        }),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ReportsReactions);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('loads active and dismissed notifications on init', () => {
    expect(notificationServiceMock.getNotificationsAndDismissed).toHaveBeenCalled();
    expect(component.isLoading()).toBe(false);
    expect(component.notifications().length).toBe(3);
    expect(component.totalCount()).toBe(3);
    expect(component.criticalCount()).toBe(1);
    expect(component.warningCount()).toBe(1);
    expect(component.infoCount()).toBe(1);
    expect(component.inBehandelingCount()).toBe(1);
  });

  it('filteredNotifications respects severity and dismissed filters', () => {
    component.setFilter('critical');
    expect(component.filteredNotifications().every((n) => n.severity === 'critical')).toBe(true);

    component.setFilter('all');
    expect(component.filteredNotifications().length).toBe(3);

    notificationServiceMock.getNotificationsAndDismissed.mockReturnValue(
      of({
        active: [makeNotification({ id: 'x' })],
        dismissed: [makeNotification({ id: 'd', severity: 'info' })],
      }),
    );
    fixture = TestBed.createComponent(ReportsReactions);
    component = fixture.componentInstance;
    fixture.detectChanges();
    component.setFilter('dismissed');
    expect(component.filteredNotifications().length).toBe(1);
    expect(component.filteredNotifications()[0].id).toBe('d');
  });

  it('in-behandeling filter lists only reported items', () => {
    component.setFilter('in-behandeling');
    expect(component.filteredNotifications().length).toBe(1);
    expect(component.filteredNotifications()[0].id).toBe('c');
  });

  it('toggleExpand fetches details once and uses cache on re-expand', async () => {
    const n = activeList[0];
    component.toggleExpand(n);
    await fixture.whenStable();
    expect(notificationServiceMock.getNotificationDetails).toHaveBeenCalledWith(n);
    expect(component.getDetails(n.id)).toEqual(['line one']);

    notificationServiceMock.getNotificationDetails.mockClear();
    component.toggleExpand(n);
    expect(component.isExpanded(n.id)).toBe(false);

    component.toggleExpand(n);
    await fixture.whenStable();
    expect(notificationServiceMock.getNotificationDetails).not.toHaveBeenCalled();
    expect(component.isExpanded(n.id)).toBe(true);
  });

  it('getSeverityIcon and getSeverityLabel', () => {
    expect(component.getSeverityIcon('critical')).toBe(component.Icons.CircleX);
    expect(component.getSeverityLabel('critical')).toBe('critical-2');
    expect(component.getSeverityLabel('info')).toBe('Info');
  });

  it('toggleExpanded flips warning section', () => {
    const v = component.isWarningExpanded();
    component.toggleExpanded();
    expect(component.isWarningExpanded()).toBe(!v);
  });

  it('feedback form state and submitFeedback', async () => {
    const n = makeNotification({ id: 'fb1' });
    component.notifications.set([n]);
    component.openFeedbackForm(n);
    expect(component.isFeedbackFormOpen(n.id)).toBe(true);
    component.setFeedbackText(n.id, '  my text  ');
    component.submitFeedback(n);
    await fixture.whenStable();

    expect(feedbackServiceMock.submitFeedback).toHaveBeenCalledWith('src', 'user-control', 'my text');
    const updated = component.notifications().find((x) => x.id === 'fb1');
    expect(updated?.hasReported).toBe(true);
  });

  it('submitFeedback does nothing when text empty', () => {
    const n = makeNotification({ id: 'fb2' });
    component.notifications.set([n]);
    component.openFeedbackForm(n);
    component.setFeedbackText(n.id, '   ');
    component.submitFeedback(n);
    expect(feedbackServiceMock.submitFeedback).not.toHaveBeenCalled();
  });

  it('getFeedbackText defaults to empty and setFeedbackText stores value', () => {
    expect(component.getFeedbackText('missing')).toBe('');
    component.setFeedbackText('x', 'hello');
    expect(component.getFeedbackText('x')).toBe('hello');
  });

  it('closeFeedbackForm closes and clears draft text', () => {
    const n = makeNotification({ id: 'fb-close' });
    component.openFeedbackForm(n);
    component.setFeedbackText(n.id, 'draft');
    component.closeFeedbackForm(n);
    expect(component.isFeedbackFormOpen(n.id)).toBe(false);
    expect(component.getFeedbackText(n.id)).toBe('');
  });

  it('submitFeedback clears submitting state on API error', async () => {
    feedbackServiceMock.submitFeedback.mockReturnValue(throwError(() => new Error('network')));
    const n = makeNotification({ id: 'fb-err' });
    component.notifications.set([n]);
    component.openFeedbackForm(n);
    component.setFeedbackText(n.id, 'text');
    component.submitFeedback(n);
    await fixture.whenStable();

    expect(component.isSubmittingFeedback(n.id)).toBe(false);
    expect(component.notifications().find((x) => x.id === 'fb-err')?.hasReported).not.toBe(true);
  });

  it('refresh closes open feedback forms', () => {
    const n = makeNotification({ id: 'fb-refresh' });
    component.notifications.set([n]);
    component.openFeedbackForm(n);
    expect(component.isFeedbackFormOpen(n.id)).toBe(true);
    component.refresh();
    expect(component.isFeedbackFormOpen(n.id)).toBe(false);
  });

  it('markAsDismissed calls API and refreshes list', async () => {
    const n = activeList[0];
    const callsBefore = notificationServiceMock.getNotificationsAndDismissed.mock.calls.length;
    component.markAsDismissed(n);
    await fixture.whenStable();

    expect(dismissedServiceMock.markAsDismissed).toHaveBeenCalled();
    expect(notificationServiceMock.getNotificationsAndDismissed.mock.calls.length).toBeGreaterThan(
      callsBefore,
    );
  });

  it('unDismiss calls API and refreshes', async () => {
    const n = makeNotification();
    const callsBefore = notificationServiceMock.getNotificationsAndDismissed.mock.calls.length;
    component.unDismiss(n);
    await fixture.whenStable();
    expect(dismissedServiceMock.unDismiss).toHaveBeenCalledWith('src', 'user-control');
    expect(notificationServiceMock.getNotificationsAndDismissed.mock.calls.length).toBeGreaterThan(
      callsBefore,
    );
  });

  it('clears lists when load fails', async () => {
    notificationServiceMock.getNotificationsAndDismissed.mockReturnValue(throwError(() => new Error('x')));
    fixture = TestBed.createComponent(ReportsReactions);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();

    expect(component.notifications()).toEqual([]);
    expect(component.dismissedNotifications()).toEqual([]);
    expect(component.isLoading()).toBe(false);
  });
});
