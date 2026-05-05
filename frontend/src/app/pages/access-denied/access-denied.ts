import { Component, inject } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';
import { CustomAuthService } from '../../auth/custom-auth-service';
import { AppIcons } from '../../shared/AppIcons';
import { StatusLayout } from '../../components/status-layout/status-layout';
import { StatusErrorCard } from '../../components/status-error-card/status-error-card';

@Component({
  selector: 'app-access-denied',
  imports: [LucideAngularModule, StatusLayout, StatusErrorCard],
  templateUrl: './access-denied.html',
  styleUrl: './access-denied.css',
})
export class AccessDenied {
  readonly Icons = AppIcons;
}
