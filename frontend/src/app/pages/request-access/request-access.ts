import { Component, inject, OnInit, signal } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';
import { AppIcons } from '../../shared/AppIcons';
import { UserService } from '../../services/user-service';
import { StatusLayout } from '../../components/status-layout/status-layout';
import { StatusRequestCard } from '../../components/status-request-card/status-request-card';

@Component({
  selector: 'app-request-access',
  imports: [LucideAngularModule, StatusLayout, StatusRequestCard],
  templateUrl: './request-access.html',
  styleUrl: './request-access.css',
})
export class RequestAccess implements OnInit {
  readonly Icons = AppIcons;

  readonly #userService = inject(UserService);

  requestSent = signal(false);

  ngOnInit(): void {
    this.getRequestSent();
  }

  requestAccess() {
    this.#userService.requestAccess('/request-access').subscribe({
      next: () => {
        this.requestSent.set(true);
      },
      error: (err) => console.error('Fout bij aanvraag', err),
    });
  }

  getRequestSent() {
    this.#userService.getRequestSent('/request-access').subscribe({
      next: (val) => {
        this.requestSent.set(val);
      },
      error: (err) => console.error('Fout bij ophalen request', err),
    });
  }
}
