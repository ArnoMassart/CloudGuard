import { Component, signal, computed, inject, OnInit } from '@angular/core';
import { PageHeader } from '../../../components/page-header/page-header';
import { SectionTopCard } from '../../../components/section-top-card/section-top-card';
import { Domain, DomainService } from '../../../services/domain-service';
import { AppIcons } from '../../../shared/AppIcons';
import { LucideAngularModule } from 'lucide-angular';
import { DnsRecord, DnsService } from '../../../services/dns-service';

@Component({
  selector: 'app-domain-dns',
  imports: [PageHeader, SectionTopCard, LucideAngularModule],
  templateUrl: './domain-dns.html',
  styleUrl: './domain-dns.css',
})
export class DomainDns implements OnInit {
  readonly domains = signal<Domain[]>([]);
  readonly isLoading = signal(true);
  readonly isRefreshing = signal(false);
  readonly #domainService = inject(DomainService);
  readonly Icons = AppIcons;

  private readonly dnsService = inject(DnsService);
  readonly rows = signal<DnsRecord[]>([]);
  readonly isLoadingDns = signal(false);
  readonly selectedDnsDomain = signal<string | null>(null);
  readonly expandedDnsRow = signal<string | null>(null);

  readonly totalDomains = computed(() => this.domains().length);
  readonly validDnsRecords = computed(() =>
    this.rows().filter((r) => r.status === 'VALID' || r.status === 'OK').length
  );
  readonly securityScore = 100;

  ngOnInit() {
    this.#loadDomains();
  }

  selectDnsDomain(domain: string) {
    this.selectedDnsDomain.set(domain);
    this.#loadDns(domain);
  }

  toggleExpandDnsRow(key: string) {
    this.expandedDnsRow.update((current) => (current === key ? null : key));
  }

  getDnsStatusLabel(status: string): string {
    if (status === 'VALID' || status === 'OK') return 'Geldig';
    if (status === 'ATTENTION' || status === 'MISSING') return 'Aandacht';
    if (status === 'ERROR') return 'Fout';
    return status;
  }

  getDnsStatusClass(status: string): 'valid' | 'attention' | 'error' | 'neutral' {
    if (status === 'VALID' || status === 'OK') return 'valid';
    if (status === 'ATTENTION' || status === 'MISSING') return 'attention';
    if (status === 'ERROR') return 'error';
    return 'neutral';
  }

  refreshData() {
    if (this.isRefreshing()) return;
    this.isRefreshing.set(true);
    this.#domainService.refreshCache().subscribe({
      next: () => {
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
      },
      error: (err) => {
        console.error('Failed to refresh cache', err);
        this.isRefreshing.set(false);
      },
    });
  }

  #loadDomains() {
    this.#domainService.getDomains().subscribe({
      next: (domains) => {
        this.domains.set(domains);
        this.isLoading.set(false);
        const primary = domains.find((d) => d.domainType === 'Primary Domain');
        if (primary && !this.selectedDnsDomain()) {
          this.selectDnsDomain(primary.domainName);
        }
      },
      error: (err) => {
        console.error('Failed to load domains', err);
        this.isLoading.set(false);
      },
    });
  }

  #loadDns(domain: string) {
    this.isLoadingDns.set(true);

    this.dnsService.getDnsRecords(domain).subscribe({
      next: (res) => {
        this.rows.set(res.rows);
        this.isLoadingDns.set(false);
      },
      error: (err) => {
        console.error('DNS load failed', err);
        this.isLoadingDns.set(false);
      }
    });
  }
}
