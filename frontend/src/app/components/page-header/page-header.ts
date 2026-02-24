import { Component, Input } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';
import { AppIcons } from '../../shared/AppIcons';
import { UtilityMethods } from '../../shared/UtilityMethods';

@Component({
  selector: 'app-page-header',
  imports: [LucideAngularModule],
  templateUrl: './page-header.html',
  styleUrl: './page-header.css',
})
export class PageHeader {
  readonly Icons = AppIcons;
  readonly UtilityMethods = UtilityMethods;

  @Input() Title: string = '';
  @Input() Description: string = '';
  @Input() NeedAdminLink: boolean = true;
  @Input() AdminLink: string = '';
}
