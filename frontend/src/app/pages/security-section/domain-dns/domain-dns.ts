import { Component, signal, inject, OnInit } from '@angular/core';
import { PageHeader } from '../../../components/page-header/page-header';
import { Domain, DomainService } from '../../../services/domain-service';
import { AppIcons } from '../../../shared/AppIcons';
import { LucideAngularModule } from 'lucide-angular';

@Component({
  selector: 'app-domain-dns',
  imports: [PageHeader, LucideAngularModule],
  templateUrl: './domain-dns.html',
  styleUrl: './domain-dns.css',
})
export class DomainDns implements OnInit {
  readonly domains = signal<Domain[]>([]);
  readonly isLoading = signal(true);
  readonly isRefreshing = signal(false);
  readonly #domainService = inject(DomainService);
  readonly Icons = AppIcons;

  ngOnInit() {
    this.#loadDomains();
  }

  refreshData() {
    if (this.isRefreshing()) return;
    this.isRefreshing.set(true);
    this.#domainService.getDomains().subscribe({
      next: (domains) => {
        this.domains.set(domains);
        this.isRefreshing.set(false);
      },
      error: (err) => {
        console.error('Failed to load domains', err);
        this.isRefreshing.set(false);
      },
    });
  }

  #loadDomains() {
    this.#domainService.getDomains().subscribe({
      next: (domains) => {
        this.domains.set(domains);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Failed to load domains', err);
        this.isLoading.set(false);
      },
    });
  }
}
