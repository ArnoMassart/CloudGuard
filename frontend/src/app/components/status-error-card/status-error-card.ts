import { Component, Input } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';
import { LucideAngularModule, LucideIconData } from 'lucide-angular';
import { AppIcons } from '../../shared/AppIcons';

@Component({
  selector: 'app-status-error-card',
  imports: [LucideAngularModule, TranslocoPipe],
  templateUrl: './status-error-card.html',
  styleUrl: './status-error-card.css',
})
export class StatusErrorCard {
  @Input() icon: LucideIconData = AppIcons.CircleX;
}
