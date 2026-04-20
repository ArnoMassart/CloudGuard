import { Component, computed, input, Input } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';
import { AppIcons } from '../../shared/AppIcons';
import { TranslocoPipe } from '@jsverse/transloco';

@Component({
  selector: 'app-api-error',
  imports: [LucideAngularModule, TranslocoPipe],
  templateUrl: './api-error.html',
  styleUrl: './api-error.css',
})
export class ApiError {
  readonly Icons = AppIcons;

  readonly errorMessage = input<string | null>(null);

  readonly descriptionKey = input<string>('error.general_description');

  @Input() fullBorder: boolean = true;

  readonly isNoAdminError = computed(() => {
    const msg = this.errorMessage();
    if (!msg) return false;

    return msg.includes('No Admin email configured');
  });
}
