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
  @Input() Value: number | undefined = 0;
  @Input() Icon: LucideIconData = Shield;
  @Input() BackgroundColor: string = '#dbeafe';
  @Input() IconColor: string = '#155dfc';
  @Input() TextColor: string = 'black';
  @Input() IsPercentage: boolean = false;

  getTextColor(): string {
    if (!this.IsPercentage) return this.TextColor;

    if (this.Value != undefined) {
      if (this.Value < 50) return '#f54a00';
      else if (this.Value < 75) return '#d38700';
      else return '#3abfad';
    }

    return this.TextColor;
  }
}
