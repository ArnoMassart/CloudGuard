import { Component, EventEmitter, inject, Input, Output } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';
import { AppIcons } from '../../shared/AppIcons';
import { Router } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';

@Component({
  selector: 'app-page-warnings',
  imports: [LucideAngularModule, TranslocoPipe],
  templateUrl: './page-warnings.html',
  styleUrl: './page-warnings.css',
  standalone: true,
})
export class PageWarnings {
  readonly Icons = AppIcons;
  readonly #router = inject(Router);

  @Input() isCritical: boolean = false;
  @Input() isDashboard: boolean = false;
  /** Info callout: same chrome as waarschuwing, blue palette, no toggle/chevron */
  @Input() isInfo: boolean = false;

  @Input() isExpanded: boolean = false;
  @Input() hasMultipleWarnings: boolean = false;

  @Input() title: string = '';

  // Output to tell the parent page to toggle the state
  @Output() toggle = new EventEmitter<void>();

  navigateToNotifications() {
    this.#router.navigate(['/reports-reactions']);
  }
}
