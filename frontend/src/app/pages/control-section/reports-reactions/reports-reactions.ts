import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { PageHeader } from '../../../components/page-header/page-header';
import { SectionTopCard } from '../../../components/section-top-card/section-top-card';
import { FilterChips } from '../../../components/filter-chips/filter-chips';
import { LucideAngularModule } from 'lucide-angular';
import { AppIcons } from '../../../shared/AppIcons';
import { FilterOption } from '../../../models/FilterOption';
import { NotificationService } from '../../../services/notification-service';
import { Notification, NotificationSeverity } from '../../../models/notification/Notification';
import { NotificationFeedbackService } from '../../../services/notification-feedback-service';
import { ResolvedNotificationService } from '../../../services/resolved-notification-service';
import { PageWarnings } from '../../../components/page-warnings/page-warnings';
import { PageWarningsItem } from '../../../components/page-warnings/page-warnings-item/page-warnings-item';

@Component({
  selector: 'app-reports-reactions',
  imports: [
    PageHeader,
    SectionTopCard,
    LucideAngularModule,
    PageWarnings,
    PageWarningsItem,
    FilterChips,
  ],
  templateUrl: './reports-reactions.html',
  styleUrl: './reports-reactions.css',
})
export class ReportsReactions implements OnInit {
  readonly Icons = AppIcons;
  readonly #notificationService = inject(NotificationService);
  readonly #notificationFeedbackService = inject(NotificationFeedbackService);
  readonly #resolvedService = inject(ResolvedNotificationService);

  readonly notifications = signal<Notification[]>([]);
  readonly resolvedNotifications = signal<Notification[]>([]);
  readonly isLoading = signal(true);
  readonly filterSeverity = signal<NotificationSeverity | 'all' | 'dismissed' | 'in-behandeling'>(
    'all',
  );
  readonly expandedIds = signal<Set<string>>(new Set());
  readonly detailsCache = signal<Record<string, string[]>>({});
  readonly loadingDetailsIds = signal<Set<string>>(new Set());

  readonly feedbackTextById = signal<Record<string, string>>({});
  readonly submittingIds = signal<Set<string>>(new Set());
  readonly feedbackFormOpenIds = signal<Set<string>>(new Set());
  readonly resolvingIds = signal<Set<string>>(new Set());
  readonly unDismissingIds = signal<Set<string>>(new Set());

  readonly filteredNotifications = computed(() => {
    const filter = this.filterSeverity();
    if (filter === 'dismissed') return this.resolvedNotifications();
    const list = this.notifications();
    if (filter === 'in-behandeling') return list.filter((n) => n.hasReported);
    if (filter === 'all') return list;
    return list.filter((n) => n.severity === filter);
  });

  readonly totalCount = computed(() => this.notifications().length);
  readonly resolvedCount = computed(() => this.resolvedNotifications().length);
  readonly criticalCount = computed(
    () => this.notifications().filter((n) => n.severity === 'critical').length,
  );
  readonly warningCount = computed(
    () => this.notifications().filter((n) => n.severity === 'warning').length,
  );
  readonly infoCount = computed(
    () => this.notifications().filter((n) => n.severity === 'info').length,
  );
  readonly inBehandelingCount = computed(
    () => this.notifications().filter((n) => n.hasReported).length,
  );

  readonly isWarningExpanded = signal(true);

  toggleExpanded() {
    this.isWarningExpanded.update((v) => !v);
  }
  readonly filterOptions = computed<FilterOption[]>(() => [
    {
      value: 'all',
      label: 'Alle',
      count: this.totalCount(),
      activeClass: 'bg-primary text-white',
      inactiveClass: '',
    },
    {
      value: 'critical',
      label: 'Kritiek',
      count: this.criticalCount(),
      activeClass: 'bg-red-100 text-red-800',
      inactiveClass: '',
    },
    {
      value: 'warning',
      label: 'Waarschuwing',
      count: this.warningCount(),
      activeClass: 'bg-amber-100 text-amber-800',
      inactiveClass: '',
    },
    {
      value: 'info',
      label: 'Info',
      count: this.infoCount(),
      activeClass: 'bg-blue-100 text-blue-800',
      inactiveClass: '',
    },
    {
      value: 'in-behandeling',
      label: 'In behandeling',
      count: this.inBehandelingCount(),
      activeClass: 'bg-teal-100 text-teal-800',
      inactiveClass: '',
    },
    {
      value: 'dismissed',
      label: 'Genegeerd',
      count: this.resolvedCount(),
      activeClass: 'bg-gray-400 text-white',
      inactiveClass: '',
    },
  ]);

  ngOnInit() {
    this.#loadNotifications();
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
    this.filterSeverity.set(filter as NotificationSeverity | 'all' | 'dismissed' | 'in-behandeling');
  }

  #loadNotifications() {
    this.isLoading.set(true);
    this.expandedIds.set(new Set());
    this.detailsCache.set({});
    this.loadingDetailsIds.set(new Set());
    this.feedbackFormOpenIds.set(new Set());
    this.unDismissingIds.set(new Set());
    this.#notificationService.getNotificationsAndResolved().subscribe({
      next: ({ active, resolved }) => {
        this.notifications.set(active);
        this.resolvedNotifications.set(resolved);
        this.isLoading.set(false);
      },
      error: () => {
        this.notifications.set([]);
        this.resolvedNotifications.set([]);
        this.isLoading.set(false);
      },
    });
  }

  markAsResolved(n: Notification) {
    const key = `${n.source}:${n.notificationType}`;
    if (this.resolvingIds().has(key)) return;
    this.resolvingIds.update((s) => new Set(s).add(key));
    this.#resolvedService
      .markAsResolved({
        source: n.source,
        notificationType: n.notificationType,
        sourceLabel: n.sourceLabel,
        sourceRoute: n.sourceRoute,
        title: n.title,
        description: n.description,
        severity: n.severity,
        recommendedActions: n.recommendedActions,
      })
      .subscribe({
        next: () => {
          this.resolvingIds.update((s) => {
            const next = new Set(s);
            next.delete(key);
            return next;
          });
          this.refresh();
        },
        error: () => {
          this.resolvingIds.update((s) => {
            const next = new Set(s);
            next.delete(key);
            return next;
          });
        },
      });
  }

  isResolving(n: Notification): boolean {
    return this.resolvingIds().has(`${n.source}:${n.notificationType}`);
  }

  unDismiss(n: Notification) {
    const key = `${n.source}:${n.notificationType}`;
    if (this.unDismissingIds().has(key)) return;
    this.unDismissingIds.update((s) => new Set(s).add(key));
    this.#resolvedService.unDismiss(n.source, n.notificationType).subscribe({
      next: () => {
        this.unDismissingIds.update((s) => {
          const next = new Set(s);
          next.delete(key);
          return next;
        });
        this.refresh();
      },
      error: () => {
        this.unDismissingIds.update((s) => {
          const next = new Set(s);
          next.delete(key);
          return next;
        });
      },
    });
  }

  isUnDismissing(n: Notification): boolean {
    return this.unDismissingIds().has(`${n.source}:${n.notificationType}`);
  }

  refresh() {
    this.#loadNotifications();
  }

  getSeverityIcon(severity: NotificationSeverity) {
    if (severity === 'critical') return this.Icons.CircleX;
    if (severity === 'warning') return this.Icons.TriangleAlert;
    return this.Icons.CircleQuestionMark;
  }

  getSeverityLabel(severity: NotificationSeverity): string {
    if (severity === 'critical') return 'Kritiek';
    if (severity === 'warning') return 'Waarschuwing';
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
          },
          error: () => {
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
          list.map((item) =>
            item.id === n.id ? { ...item, hasReported: true } : item,
          ),
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
      },
      error: () => {
        this.submittingIds.update((s) => {
          const next = new Set(s);
          next.delete(n.id);
          return next;
        });
      },
    });
  }
}
