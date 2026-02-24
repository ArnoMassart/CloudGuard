import { Component, Input } from '@angular/core';
import { Shield, LucideIconData, LucideAngularModule } from 'lucide-angular';

@Component({
  selector: 'app-section-top-card',
  imports: [LucideAngularModule],
  templateUrl: './section-top-card.html',
  styleUrl: './section-top-card.css',
})
export class SectionTopCard {
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
      if (this.Value < 50) return '#e7000b';
      else if (this.Value < 75) return '#d38700';
      else return '#3abfad';
    }

    return this.TextColor;
  }

  getIconColor(): string {
    if (!this.IsPercentage) return this.IconColor;

    if (this.Value != undefined) {
      if (this.Value < 50) return '#f54a00';
      else if (this.Value < 75) return '#d38700';
      else return '#3abfad';
    }

    return this.IconColor;
  }

  getBackgroundColor(): string {
    if (!this.IsPercentage) return this.BackgroundColor;

    if (this.Value != undefined) {
      if (this.Value < 50) return '#ffe2e2';
      else if (this.Value < 75) return '#fef9c2';
      else return '#d8f2ef';
    }

    return this.BackgroundColor;
  }
}
