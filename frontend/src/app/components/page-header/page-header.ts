import { Component, Input } from '@angular/core';
import { ExternalLink, LucideAngularModule } from 'lucide-angular';

@Component({
  selector: 'app-page-header',
  imports: [LucideAngularModule],
  templateUrl: './page-header.html',
  styleUrl: './page-header.css',
})
export class PageHeader {
  @Input() title: string = '';
  @Input() description: string = '';
  @Input() needAdminLink: boolean = true;
  @Input() adminLink: string = '';

  readonly externalLink = ExternalLink;

  openAdminPage() {
    window.open(`https://admin.google.com/ac/${this.adminLink}`);
  }
}
