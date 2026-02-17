import { Component, Input } from '@angular/core';
import { LucideAngularModule, LucideIconData, Shield } from 'lucide-angular';

@Component({
  selector: 'app-users-section-top-card',
  imports: [LucideAngularModule],
  templateUrl: './users-section-top-card.html',
  styleUrl: './users-section-top-card.css',
})
export class UsersSectionTopCard {
  readonly shieldIcon = Shield;

  @Input() Title: string = '';
  @Input() Value: string = '';
  @Input() Icon: LucideIconData = Shield;
  @Input() BackgroundColor: string = '#dbeafe';
  @Input() IconColor: string = '#155dfc';
  @Input() TextColor: string = 'black';
}
