import { Component, Input } from '@angular/core';
import { ExternalLink, LucideAngularModule } from 'lucide-angular';

@Component({
  selector: 'app-page-header',
  imports: [LucideAngularModule],
  templateUrl: './page-header.html',
  styleUrl: './page-header.css',
})
export class PageHeader {
  @Input() Title: string = '';
  @Input() Description: string = '';
  @Input() NeedAdminLink: boolean = true;
  @Input() AdminLink: string = '';

  readonly externalLink = ExternalLink;

  openAdminPage() {
    window.open(`https://admin.google.com/ac/${this.AdminLink}`);
  }
}
