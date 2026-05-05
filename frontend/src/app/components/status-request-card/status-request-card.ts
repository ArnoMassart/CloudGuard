import { Component, inject, input, Input, output } from '@angular/core';
import { LucideIconData, LucideAngularModule } from 'lucide-angular';
import { AppIcons } from '../../shared/AppIcons';
import { TranslocoPipe } from '@jsverse/transloco';
import { CustomAuthService } from '../../auth/custom-auth-service';

@Component({
  selector: 'app-status-request-card',
  imports: [LucideAngularModule, TranslocoPipe],
  templateUrl: './status-request-card.html',
  styleUrl: './status-request-card.css',
})
export class StatusRequestCard {
  readonly Icons = AppIcons;
  readonly #authService = inject(CustomAuthService);

  Icon = input.required<LucideIconData>();
  TitleKey = input<string>('unassigned.title');
  DescriptionKey = input.required<string>();
  RequestSent = input.required<boolean>();
  RequestAccess = output<void>();

  tryAgain() {
    this.#authService.logout();
  }
}
