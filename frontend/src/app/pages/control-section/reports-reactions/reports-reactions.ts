import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { PageHeader } from '../../../components/page-header/page-header';
import { SectionTopCard } from '../../../components/section-top-card/section-top-card';
import { LucideAngularModule } from 'lucide-angular';
import { AppIcons } from '../../../shared/AppIcons';
import {
  NotificationService,
  Notification,
  NotificationSeverity,
} from '../../../services/notification-service';
import { NotificationFeedbackService } from '../../../services/notification-feedback-service';

@Component({
  selector: 'app-reports-reactions',
  imports: [PageHeader, SectionTopCard, LucideAngularModule, RouterLink],
  templateUrl: './reports-reactions.html',
  styleUrl: './reports-reactions.css',
})
export class ReportsReactions implements OnInit {
  readonly Icons = AppIcons;
  readonly #notificationService = inject(NotificationService);
  readonly #notificationFeedbackService = inject(NotificationFeedbackService);

  readonly notifications = signal<Notification[]>([]);
  readonly isLoading = signal(true);
  readonly filterSeverity = signal<NotificationSeverity | 'all' | 'resolved'>('all');
  readonly expandedIds = signal<Set<string>>(new Set());
  readonly detailsCache = signal<Record<string, string[]>>({});
  readonly loadingDetailsIds = signal<Set<string>>(new Set());

  readonly feedbackModalOpen = signal<string | null>(null);  // notification id when open
  readonly feedbackText = signal('');
  readonly feedbackSubmitted = signal<Set<string>>(new Set());  // ids that have feedback 
  readonly isSubmitting = signal(false);

  readonly filteredNotifications = computed(() => {
    const list = this.notifications();
    const filter = this.filterSeverity();
    if (filter === 'all') return list;
    if (filter === 'resolved') return [];
    return list.filter((n) => n.severity === filter);
  });

  readonly totalCount = computed(() => this.notifications().length);
  readonly criticalCount = computed(() =>
    this.notifications().filter((n) => n.severity === 'critical').length
  );
  readonly warningCount = computed(() =>
    this.notifications().filter((n) => n.severity === 'warning').length
  );
  readonly infoCount = computed(() =>
    this.notifications().filter((n) => n.severity === 'info').length
  );

  ngOnInit() {
    this.#loadNotifications();
  }

  setFilter(filter: NotificationSeverity | 'all' | 'resolved') {
    this.filterSeverity.set(filter);
  }

  #loadNotifications() {
    this.isLoading.set(true);
    this.expandedIds.set(new Set());
    this.detailsCache.set({});
    this.loadingDetailsIds.set(new Set());
    this.feedbackSubmitted.set(new Set());
    this.#notificationService.getNotifications().subscribe({
      next: (list) => {
        this.notifications.set(list);
        this.#loadFeedbackStatus(list);
        this.isLoading.set(false);
      },
      error: () => {
        this.notifications.set([]);
        this.isLoading.set(false);
      },
    });
  }

  refresh() {
    this.#loadNotifications();
  }

  getSeverityIcon(severity: NotificationSeverity) {
    if (severity === 'critical') return this.Icons.CircleX;
    if (severity === 'warning') return this.Icons.TriangleAlert;
    return this.Icons.CircleHelp;
  }

  getSeverityLabel(severity: NotificationSeverity): string {
    if (severity === 'critical') return 'Kritiek';
    if (severity === 'warning') return 'Waarschuwing';
    return 'Info';
  }

  supportsDetails(notificationType: string): boolean {
    return [
      'user-control',
      'group-external',
      'oauth-high-risk',
      'drive-orphan',
      'drive-external',
      'device-lockscreen',
      'device-encryption',
      'device-os',
      'device-integrity',
    ].includes(notificationType);
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

  openFeedbackModal(n: Notification) {
    this.feedbackModalOpen.set(n.id);
    this.feedbackText.set('');
  }

  closeFeedbackModal() {
    this.feedbackModalOpen.set(null);
  }

  #loadFeedbackStatus(list: Notification[]) {
    list.forEach((n) => {
      this.#notificationFeedbackService.hasFeedback(n.source, n.notificationType).subscribe({
        next: (has) => {
          if (has) {
            this.feedbackSubmitted.update((s) => new Set(s).add(n.id));
          }
        },
      });
    });
  }

  submitFeedback(n: Notification) {
    const text = this.feedbackText().trim();
    if (!text) return;
    this.isSubmitting.set(true);

    this.#notificationFeedbackService.submitFeedback(n.source,n.notificationType, text).subscribe({
      next: () => {
        this.feedbackSubmitted.update(s => new Set(s).add(n.id));
        this.closeFeedbackModal();
        this.isSubmitting.set(false);
      },
      error: () => {
        this.isSubmitting.set(false);
      }
    });
  }
}
