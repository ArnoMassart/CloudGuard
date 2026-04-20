import { Component, inject, signal, computed, OnInit, OnDestroy } from '@angular/core';
import { PageHeader } from '../../../components/page-header/page-header';
import { SectionTopCard } from '../../../components/section-top-card/section-top-card';
import { FilterChips } from '../../../components/filter-chips/filter-chips';
import { LucideAngularModule } from 'lucide-angular';
import { AppIcons } from '../../../shared/AppIcons';
import { FilterOption } from '../../../models/FilterOption';
import { NotificationService } from '../../../services/notification-service';
import { Notification, NotificationSeverity } from '../../../models/notification/Notification';
import { NotificationFeedbackService } from '../../../services/notification-feedback-service';
import { PageWarnings } from '../../../components/page-warnings/page-warnings';
import { PageWarningsItem } from '../../../components/page-warnings/page-warnings-item/page-warnings-item';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { HttpErrorResponse } from '@angular/common/http';

import { Observable, Subscription, of } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';

@Component({
  selector: 'app-reports-reactions',
  imports: [
    PageHeader,
    SectionTopCard,
    LucideAngularModule,
    PageWarnings,
    PageWarningsItem,
    FilterChips,
    TranslocoPipe,
  ],
  templateUrl: './reports-reactions.html',
  styleUrl: './reports-reactions.css',
})
export class ReportsReactions implements OnInit, OnDestroy {
  readonly Icons = AppIcons;
  readonly #notificationService = inject(NotificationService);
  readonly #notificationFeedbackService = inject(NotificationFeedbackService);
  readonly #translocoService = inject(TranslocoService);

  readonly notifications = signal<Notification[]>([]);
  /** ISO instant from API; null if never synced */
  readonly lastNotificationSyncAt = signal<string | null>(null);
  readonly isLoading = signal(true);
  readonly listLoadError = signal<string | null>(null);
  readonly syncWarning = signal<string | null>(null);
  readonly actionError = signal<string | null>(null);
  readonly filterSeverity = signal<NotificationSeverity | 'all' | 'in-behandeling' | 'solved'>('all');
  readonly expandedIds = signal<Set<string>>(new Set());
  readonly detailsCache = signal<Record<string, string[]>>({});
  readonly loadingDetailsIds = signal<Set<string>>(new Set());

  readonly feedbackTextById = signal<Record<string, string>>({});
  readonly submittingIds = signal<Set<string>>(new Set());
  readonly feedbackFormOpenIds = signal<Set<string>>(new Set());

  readonly filteredNotifications = computed(() => {
    const filter = this.filterSeverity();
    const list = this.notifications();
    if (filter === 'solved') return list.filter((n) => n.instanceStatus === 'solved');
    if (filter === 'in-behandeling')
      return list.filter((n) => n.hasReported && n.instanceStatus !== 'solved');
    if (filter === 'all') return list;
    return list.filter(
      (n) => n.instanceStatus !== 'solved' && n.severity === filter,
    );
  });

  readonly totalCount = computed(() => this.notifications().length);
  /** Open (non-solved) items only — used for severity summary cards. */
  readonly criticalCount = computed(
    () =>
      this.notifications().filter((n) => n.instanceStatus !== 'solved' && n.severity === 'critical')
        .length,
  );
  readonly warningCount = computed(
    () =>
      this.notifications().filter((n) => n.instanceStatus !== 'solved' && n.severity === 'warning')
        .length,
  );
  readonly infoCount = computed(
    () =>
      this.notifications().filter((n) => n.instanceStatus !== 'solved' && n.severity === 'info').length,
  );
  readonly solvedCount = computed(
    () => this.notifications().filter((n) => n.instanceStatus === 'solved').length,
  );
  readonly inBehandelingCount = computed(
    () =>
      this.notifications().filter((n) => n.hasReported && n.instanceStatus !== 'solved').length,
  );

  readonly isWarningExpanded = signal(true);

  toggleExpanded() {
    this.isWarningExpanded.update((v) => !v);
  }
  readonly filterOptions = computed<FilterOption[]>(() => [
    {
      value: 'all',
      label: 'all',
      count: this.totalCount(),
      activeClass: 'bg-primary text-white',
      inactiveClass: '',
    },
    {
      value: 'critical',
      label: 'critical-2',
      count: this.criticalCount(),
      activeClass: 'bg-red-100 text-red-800',
      inactiveClass: '',
    },
    {
      value: 'warning',
      label: 'warning-2',
      count: this.warningCount(),
      activeClass: 'bg-amber-100 text-amber-800',
      inactiveClass: '',
    },
    {
      value: 'info',
      label: 'info',
      count: this.infoCount(),
      activeClass: 'bg-blue-100 text-blue-800',
      inactiveClass: '',
    },
    {
      value: 'in-behandeling',
      label: 'feedback.processing',
      count: this.inBehandelingCount(),
      activeClass: 'bg-teal-100 text-teal-800',
      inactiveClass: '',
    },
    {
      value: 'solved',
      label: 'feedback.solved',
      count: this.solvedCount(),
      activeClass: 'bg-emerald-100 text-emerald-800',
      inactiveClass: '',
    },
  ]);

  private langSubscription?: Subscription;

  ngOnInit() {
    this.langSubscription = this.#translocoService.langChanges$.subscribe(() => {
      this.#loadNotifications(false);
    });
  }

  ngOnDestroy(): void {
    if (this.langSubscription) {
      this.langSubscription.unsubscribe();
    }
  }

  getFeedbackText(id: string): string {
    return this.feedbackTextById()[id] ?? '';
  }

  setFeedbackText(id: string, text: string) {
    this.feedbackTextById.update((m) => ({ ...m, [id]: text }));
  }

  isSubmittingFeedback(id: string): boolean {
    return this.submittingIds().has(id);
  }

  openFeedbackForm(n: Notification) {
    this.feedbackFormOpenIds.update((s) => new Set(s).add(n.id));
  }

  closeFeedbackForm(n: Notification) {
    this.feedbackFormOpenIds.update((s) => {
      const next = new Set(s);
      next.delete(n.id);
      return next;
    });
    this.setFeedbackText(n.id, '');
  }

  isFeedbackFormOpen(id: string): boolean {
    return this.feedbackFormOpenIds().has(id);
  }

  setFilter(filter: string) {
    this.filterSeverity.set(filter as NotificationSeverity | 'all' | 'in-behandeling' | 'solved');
  }

  isSolvedNotification(n: Notification): boolean {
    return n.instanceStatus === 'solved';
  }

  /** Shown next to the clock icon for open items (org-wide projection sync time). */
  lastSyncedLine(): string {
    const relative = this.#formatRelativeAgo(this.lastNotificationSyncAt());
    if (relative === null) {
      return this.#translocoService.translate('feedback.last-sync-never');
    }
    return this.#translocoService.translate('feedback.last-sync-ago', { value: relative });
  }

  /** Creation time from `tbl_notifications.created_at` (next to clock on open cards). */
  formatNotificationCreatedAt(n: Notification): string {
    const formatted = this.#formatIsoInstant(n.createdAt ?? null);
    if (formatted === null) {
      return this.#translocoService.translate('feedback.notification-created-unknown');
    }
    return formatted;
  }

  #formatIsoInstant(iso: string | null): string | null {
    if (!iso) return null;
    const d = new Date(iso);
    if (Number.isNaN(d.getTime())) return null;
    const lang = this.#translocoService.getActiveLang() ?? 'en';
    return new Intl.DateTimeFormat(lang.replace('_', '-'), {
      dateStyle: 'short',
      timeStyle: 'short',
    }).format(d);
  }

  #formatRelativeAgo(iso: string | null): string | null {
    if (!iso) return null;
    const d = new Date(iso);
    if (Number.isNaN(d.getTime())) return null;

    const diffMs = Math.max(0, Date.now() - d.getTime());
    const minuteMs = 60_000;
    const hourMs = 60 * minuteMs;
    const dayMs = 24 * hourMs;

    if (diffMs < minuteMs) {
      return this.#translocoService.translate('feedback.last-sync-just-now');
    }

    if (diffMs < hourMs) {
      const minutes = Math.floor(diffMs / minuteMs);
      const unitKey = minutes === 1 ? 'feedback.last-sync-minute' : 'feedback.last-sync-minutes';
      return `${minutes} ${this.#translocoService.translate(unitKey)}`;
    }

    if (diffMs < dayMs) {
      const hours = Math.floor(diffMs / hourMs);
      const unitKey = hours === 1 ? 'feedback.last-sync-hour' : 'feedback.last-sync-hours';
      return `${hours} ${this.#translocoService.translate(unitKey)}`;
    }

    const days = Math.floor(diffMs / dayMs);
    const unitKey = days === 1 ? 'feedback.last-sync-day' : 'feedback.last-sync-days';
    return `${days} ${this.#translocoService.translate(unitKey)}`;
  }

  #loadNotifications(syncFirst = false) {
    this.listLoadError.set(null);
    this.syncWarning.set(null);
    this.isLoading.set(true);
    this.expandedIds.set(new Set());
    this.detailsCache.set({});
    this.loadingDetailsIds.set(new Set());
    this.feedbackFormOpenIds.set(new Set());

    type SyncMeta =
      | { mode: 'skipped' }
      | { mode: 'ok' }
      | { mode: 'failed'; detail: string };

    const load$ = this.#notificationService.getNotifications();
    const pipeline$ = syncFirst
      ? this.#notificationService.syncNotifications().pipe(
          map((): SyncMeta => ({ mode: 'ok' })),
          catchError((err): Observable<SyncMeta> => {
            console.error('Notification sync failed: ', err);
            return of({ mode: 'failed', detail: this.#httpErrorDetail(err) });
          }),
          switchMap((syncMeta) =>
            load$.pipe(
              map((data) => ({
                active: data.active,
                solved: data.solved,
                lastNotificationSyncAt: data.lastNotificationSyncAt,
                syncMeta,
              })),
            ),
          ),
        )
      : load$.pipe(
          map((data) => ({
            active: data.active,
            solved: data.solved,
            lastNotificationSyncAt: data.lastNotificationSyncAt,
            syncMeta: { mode: 'skipped' } as const,
          })),
        );
    pipeline$.subscribe({
      next: ({ active, solved, lastNotificationSyncAt, syncMeta }) => {
        const merged: Notification[] = [
          ...active.map((n) => ({ ...n, instanceStatus: 'active' as const })),
          ...solved.map((n) => ({ ...n, instanceStatus: 'solved' as const })),
        ];
        this.notifications.set(merged);
        this.lastNotificationSyncAt.set(lastNotificationSyncAt ?? null);
        this.isLoading.set(false);
        this.listLoadError.set(null);
        if (syncMeta.mode === 'failed') {
          this.syncWarning.set(
            syncMeta.detail ||
              this.#translocoService.translate('reports-reactions.error.sync-failed'),
          );
        } else {
          this.syncWarning.set(null);
        }
      },
      error: (err) => {
        console.error('Notifications load failed: ', err);
        this.notifications.set([]);
        this.lastNotificationSyncAt.set(null);
        const msg = this.#httpErrorDetail(err);
        this.listLoadError.set(
          msg || this.#translocoService.translate('reports-reactions.error.load-failed'),
        );
        this.isLoading.set(false);
      },
    });
  }

  refresh() {
    this.#loadNotifications(true);
  }

  getSeverityIcon(severity: NotificationSeverity) {
    if (severity === 'critical') return this.Icons.CircleX;
    if (severity === 'warning') return this.Icons.TriangleAlert;
    return this.Icons.CircleQuestionMark;
  }

  getSeverityLabel(severity: NotificationSeverity): string {
    if (severity === 'critical') return 'critical-2';
    if (severity === 'warning') return 'warning-2';
    return 'Info';
  }

  toggleExpand(n: Notification) {
    const id = n.id;
    const expanded = new Set(this.expandedIds());
    if (expanded.has(id)) {
      expanded.delete(id);
    } else {
      expanded.add(id);
      const cache = this.detailsCache();
      if (!(id in cache)) {
        const loading = new Set(this.loadingDetailsIds());
        loading.add(id);
        this.loadingDetailsIds.set(loading);
        this.#notificationService.getNotificationDetails(n).subscribe({
          next: (details) => {
            this.detailsCache.update((c) => ({ ...c, [id]: details }));
            this.loadingDetailsIds.update((s) => {
              const next = new Set(s);
              next.delete(id);
              return next;
            });
            this.actionError.set(null);
          },
          error: (err) => {
            console.error('Get details failed: ', err);
            const msg = this.#httpErrorDetail(err);
            this.actionError.set(
              msg || this.#translocoService.translate('reports-reactions.error.get-details-failed'),
            );
            this.loadingDetailsIds.update((s) => {
              const next = new Set(s);
              next.delete(id);
              return next;
            });
          },
        });
      }
    }
    this.expandedIds.set(expanded);
  }

  isExpanded(id: string): boolean {
    return this.expandedIds().has(id);
  }

  getDetails(id: string): string[] {
    return this.detailsCache()[id] ?? [];
  }

  isLoadingDetails(id: string): boolean {
    return this.loadingDetailsIds().has(id);
  }

  submitFeedback(n: Notification) {
    const text = this.getFeedbackText(n.id).trim();
    if (!text) return;
    this.submittingIds.update((s) => new Set(s).add(n.id));

    this.#notificationFeedbackService.submitFeedback(n.source, n.notificationType, text).subscribe({
      next: () => {
        this.notifications.update((list) =>
          list.map((item) => (item.id === n.id ? { ...item, hasReported: true } : item)),
        );
        this.feedbackFormOpenIds.update((s) => {
          const next = new Set(s);
          next.delete(n.id);
          return next;
        });
        this.feedbackTextById.update((m) => {
          const next = { ...m };
          delete next[n.id];
          return next;
        });
        this.submittingIds.update((s) => {
          const next = new Set(s);
          next.delete(n.id);
          return next;
        });
        this.actionError.set(null);
      },
      error: (err) => {
        console.error('Submit feedback failed: ', err);
        const msg = this.#httpErrorDetail(err);
        this.actionError.set(
          msg || this.#translocoService.translate('reports-reactions.error.submit-feedback-failed'),
        );
        this.submittingIds.update((s) => {
          const next = new Set(s);
          next.delete(n.id);
          return next;
        });
      },
    });
  }

  #httpErrorDetail(err: unknown): string {
    if (err instanceof HttpErrorResponse) {
      if (typeof err.error === 'string' && err.error.trim()) {
        return err.error.trim();
      }
    }
    if (err instanceof Error && err.message) {
      return err.message;
    }
    return '';
  }
}
