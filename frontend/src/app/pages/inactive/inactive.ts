import { Component } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';
import { AppIcons } from '../../shared/AppIcons';
import { StatusLayout } from '../../components/status-layout/status-layout';
import { StatusErrorCard } from '../../components/status-error-card/status-error-card';

@Component({
  selector: 'app-inactive',
  imports: [LucideAngularModule, StatusLayout, StatusErrorCard],
  templateUrl: './inactive.html',
  styleUrl: './inactive.css',
})
export class Inactive {
  readonly Icons = AppIcons;
}
