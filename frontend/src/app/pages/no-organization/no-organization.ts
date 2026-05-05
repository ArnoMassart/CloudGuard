import { Component, inject, signal } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';
import { UserService } from '../../services/user-service';
import { AppIcons } from '../../shared/AppIcons';
import { StatusLayout } from '../../components/status-layout/status-layout';
import { StatusRequestCard } from '../../components/status-request-card/status-request-card';

@Component({
  selector: 'app-no-organization',
  imports: [LucideAngularModule, StatusLayout, StatusRequestCard],
  templateUrl: './no-organization.html',
  styleUrl: './no-organization.css',
})
export class NoOrganization {
  readonly Icons = AppIcons;

  readonly #userService = inject(UserService);

  requestSent = signal(false);

  ngOnInit(): void {
    this.getRequestSent();
  }

  requestAccess() {
    this.#userService.requestAccess('/no-organization').subscribe({
      next: () => {
        this.requestSent.set(true);
      },
      error: (err) => console.error('Fout bij aanvraag', err),
    });
  }

  getRequestSent() {
    this.#userService.getRequestSent('/no-organization').subscribe({
      next: (val) => {
        this.requestSent.set(val);
      },
      error: (err) => console.error('Fout bij ophalen request', err),
    });
  }
}
