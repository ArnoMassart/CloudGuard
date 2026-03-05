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

@Component({
  selector: 'app-reports-reactions',
  imports: [PageHeader, SectionTopCard, LucideAngularModule, RouterLink],
  templateUrl: './reports-reactions.html',
  styleUrl: './reports-reactions.css',
})
export class ReportsReactions implements OnInit {
  readonly Icons = AppIcons;
  readonly #notificationService = inject(NotificationService);

  readonly notifications = signal<Notification[]>([]);
  readonly isLoading = signal(true);
  readonly filterSeverity = signal<NotificationSeverity | 'all' | 'resolved'>('all');

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
    this.#notificationService.getNotifications().subscribe({
      next: (list) => {
        this.notifications.set(list);
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
}
