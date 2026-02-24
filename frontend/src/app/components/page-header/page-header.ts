import { Component, Input } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';
import { AppIcons } from '../../shared/app-icons';

@Component({
  selector: 'app-page-header',
  imports: [LucideAngularModule],
  templateUrl: './page-header.html',
  styleUrl: './page-header.css',
})
export class PageHeader {
  readonly Icons = AppIcons;

  @Input() Title: string = '';
  @Input() Description: string = '';
  @Input() NeedAdminLink: boolean = true;
  @Input() AdminLink: string = '';
  openAdminPage() {
    window.open(`https://admin.google.com/ac/${this.AdminLink}`);
  }
}
