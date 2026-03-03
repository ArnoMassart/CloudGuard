import { Component, signal, inject, OnInit } from '@angular/core';
import { PageHeader } from '../../../components/page-header/page-header';
import { Domain, DomainService } from '../../../services/domain-service';

@Component({
  selector: 'app-domain-dns',
  imports: [PageHeader],
  templateUrl: './domain-dns.html',
  styleUrl: './domain-dns.css',
})
export class DomainDns implements OnInit {
  readonly domains = signal<Domain[]>([]);
  readonly #domainService = inject(DomainService);

  ngOnInit() {
    this.#loadDomains();
  }

  #loadDomains(){
    this.#domainService.getDomains().subscribe({
      next: (domains) => this.domains.set(domains),
      error: (err) => console.error('Failed to load domains', err),
    });
  }

}
