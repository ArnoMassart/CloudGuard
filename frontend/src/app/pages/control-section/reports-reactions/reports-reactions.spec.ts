import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTransloco, TranslocoLoader, TranslocoService } from '@jsverse/transloco';
import { firstValueFrom, of, throwError } from 'rxjs';
import { Notification } from '../../../models/notification/Notification';
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
  'feedback.severity.processing': 'Proc',
  'feedback.severity.none': 'None sev',
  'critical-2': 'Critical',
  'warning-2': 'Warning',
  Info: 'Info',
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
  all: 'All',
  'notifications-feedback': 'Notifications',
  'to-admin-console': 'Admin',
  info: 'Info',
  refresh: 'Refresh',
  'reports-reactions.error.load-failed': 'Load failed (i18n)',
  'reports-reactions.error.get-details-failed': 'Details failed (i18n)',
  'reports-reactions.error.submit-feedback-failed': 'Feedback failed (i18n)',
  'try-again': 'Try again',
  'feedback.last-sync-never': 'Never synced',
  'feedback.last-sync-at': 'Synced {{time}}',
  'feedback.notification-created-unknown': '—',
  'notification.created-at': 'Created',
  'feedback.solved': 'Solved',
  'feedback.solved-detail': 'Resolved detail',
  'feedback.severity.resolved': 'No resolved',
  'reports-reactions.error.sync-failed': 'Sync failed',
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
    getNotifications: ReturnType<typeof vi.fn>;
    getNotificationDetails: ReturnType<typeof vi.fn>;
    syncNotifications: ReturnType<typeof vi.fn>;
  };
  let feedbackServiceMock: { submitFeedback: ReturnType<typeof vi.fn> };

  const activeList = [
    makeNotification({ id: 'a', severity: 'critical' }),
    makeNotification({ id: 'b', severity: 'warning', supportsDetails: false }),
    makeNotification({ id: 'c', severity: 'info', hasReported: true }),
  ];

  beforeEach(async () => {
    notificationServiceMock = {
      getNotifications: vi.fn(() => of({ active: activeList, solved: [] })),
      getNotificationDetails: vi.fn(() => of(['line one'])),
      syncNotifications: vi.fn(() => of(void 0)),
    };
    feedbackServiceMock = {
      submitFeedback: vi.fn(() => of(void 0)),
    };

    await TestBed.configureTestingModule({
      imports: [ReportsReactions],
      providers: [
        { provide: NotificationService, useValue: notificationServiceMock },
        { provide: NotificationFeedbackService, useValue: feedbackServiceMock },
        provideTransloco({
          config: {
            availableLangs: ['en', 'nl'],
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

  it('renders translated notifications page title in the header', () => {
    const h1 = fixture.nativeElement.querySelector('h1');
    expect(h1?.textContent?.trim()).toBe('Notifications');
  });

  it('lastSyncedLine uses last-sync-never when never synced', () => {
    expect(component.lastNotificationSyncAt()).toBeNull();
    expect(component.lastSyncedLine()).toBe('Never synced');
  });

  it('loads notifications on init', () => {
    expect(notificationServiceMock.getNotifications).toHaveBeenCalled();
    expect(component.isLoading()).toBe(false);
    expect(component.notifications().length).toBe(3);
    expect(component.totalCount()).toBe(3);
    expect(component.criticalCount()).toBe(1);
    expect(component.warningCount()).toBe(1);
    expect(component.infoCount()).toBe(1);
    expect(component.inBehandelingCount()).toBe(1);
  });

  it('merges active and solved notifications with instanceStatus', async () => {
    notificationServiceMock.getNotifications.mockReturnValue(
      of({
        active: [makeNotification({ id: 'open-n' })],
        solved: [makeNotification({ id: 'solved-n' })],
      }),
    );
    fixture = TestBed.createComponent(ReportsReactions);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();

    expect(component.notifications().length).toBe(2);
    expect(component.notifications().find((x) => x.id === 'solved-n')?.instanceStatus).toBe('solved');
    expect(component.solvedCount()).toBe(1);
  });

  it('filteredNotifications respects severity filters', () => {
    component.setFilter('critical');
    expect(component.filteredNotifications().every((n) => n.severity === 'critical')).toBe(true);

    component.setFilter('all');
    expect(component.filteredNotifications().length).toBe(3);
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

  it('clears lists and sets listLoadError when load fails', async () => {
    notificationServiceMock.getNotifications.mockReturnValue(throwError(() => new Error('boom')));
    fixture = TestBed.createComponent(ReportsReactions);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();

    expect(component.notifications()).toEqual([]);
    expect(component.isLoading()).toBe(false);
    expect(component.listLoadError()).toBeTruthy();
    expect(component.listLoadError() ?? '').toContain('boom');
  });

  it('uses API error body for list load when HttpErrorResponse has string error', async () => {
    notificationServiceMock.getNotifications.mockReturnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 503,
            error: ' maintenance ',
          }),
      ),
    );
    fixture = TestBed.createComponent(ReportsReactions);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();

    expect(component.listLoadError()).toBe('maintenance');
  });

  it('falls back to i18n when list load HttpErrorResponse has no string body', async () => {
    notificationServiceMock.getNotifications.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 500, error: { code: 'x' } })),
    );
    fixture = TestBed.createComponent(ReportsReactions);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();

    expect(component.listLoadError()).toBe('Load failed (i18n)');
  });

  it('reloads notifications when language changes', async () => {
    const transloco = TestBed.inject(TranslocoService);
    await firstValueFrom(transloco.load('nl'));
    const callsAfterInit = notificationServiceMock.getNotifications.mock.calls.length;
    transloco.setActiveLang('nl');
    await fixture.whenStable();
    expect(notificationServiceMock.getNotifications.mock.calls.length).toBeGreaterThan(callsAfterInit);
  });

  it('refresh collapses expanded row, syncs, and refetches list', async () => {
    const n = activeList[0];
    component.toggleExpand(n);
    await fixture.whenStable();
    expect(component.isExpanded(n.id)).toBe(true);

    const callsBefore = notificationServiceMock.getNotifications.mock.calls.length;
    component.refresh();
    await fixture.whenStable();

    expect(notificationServiceMock.syncNotifications).toHaveBeenCalled();
    expect(component.isExpanded(n.id)).toBe(false);
    expect(notificationServiceMock.getNotifications.mock.calls.length).toBeGreaterThan(callsBefore);
  });

  it('toggleExpand sets actionError when details request fails', async () => {
    notificationServiceMock.getNotificationDetails.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 400, error: 'no details' })),
    );
    const n = activeList[0];
    component.toggleExpand(n);
    await fixture.whenStable();

    expect(component.actionError()).toBe('no details');
    expect(component.isExpanded(n.id)).toBe(true);
    expect(component.isLoadingDetails(n.id)).toBe(false);
  });

  it('submitFeedback surfaces HttpErrorResponse string body as actionError', async () => {
    feedbackServiceMock.submitFeedback.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 400, error: 'bad feedback' })),
    );
    const n = makeNotification({ id: 'fb-http' });
    component.notifications.set([n]);
    component.setFeedbackText(n.id, 'ok');
    component.submitFeedback(n);
    await fixture.whenStable();
    expect(component.actionError()).toBe('bad feedback');
  });

  it('falls back to i18n when submitFeedback error has no string body', async () => {
    feedbackServiceMock.submitFeedback.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 500, error: { x: 1 } })),
    );
    const n = makeNotification({ id: 'fb-i18n' });
    component.notifications.set([n]);
    component.setFeedbackText(n.id, 'ok');
    component.submitFeedback(n);
    await fixture.whenStable();
    expect(component.actionError()).toBe('Feedback failed (i18n)');
  });
});
