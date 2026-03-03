import { Component, Input } from '@angular/core';
import { LucideIconData, LucideAngularModule } from 'lucide-angular';
import { AppIcons } from '../../shared/AppIcons';

@Component({
  selector: 'app-section-top-card',
  imports: [LucideAngularModule],
  templateUrl: './section-top-card.html',
  styleUrl: './section-top-card.css',
})
export class SectionTopCard {
  readonly Icons = AppIcons;

  @Input() Title: string = '';
  @Input() Value: string | number | undefined = 0;
  @Input() Icon: LucideIconData = this.Icons.Shield;
  @Input() BackgroundColor: string = '#dbeafe';
  @Input() IconColor: string = '#155dfc';
  @Input() TextColor: string = 'black';
  @Input() IsPercentage: boolean = false;
  @Input() IsCurrency: boolean = false;

  getValue(): string {
    let result = '';

    if (this.Value === undefined || this.Value === null) {
      result = '0';
    } else if (this.IsCurrency) {
      result = '€';
      if (typeof this.Value === 'number') {
        result += this.Value.toFixed(2);
      } else {
        result += this.Value;
      }
    } else {
      result = this.Value.toString();
    }

    if (this.IsPercentage) {
      result += '%';
    }

    return result;
  }

  /*
  {{IsCurrency ? "€" : ""}}{{Value !=
            undefined ? IsCurrency && Value typeof number ? Value.toFixed(2) : Value :
            0}}{{IsPercentage ? "%" : ""}}
  */

  getTextColor(): string {
    if (!this.IsPercentage) return this.TextColor;

    const num = typeof this.Value === 'number' ? this.Value : undefined;
    if (num != undefined) {
      if (num < 50) return '#e7000b';
      else if (num < 75) return '#d38700';
      else return '#3abfad';
    }

    return this.TextColor;
  }

  getIconColor(): string {
    if (!this.IsPercentage) return this.IconColor;

    const num = typeof this.Value === 'number' ? this.Value : undefined;
    if (num != undefined) {
      if (num < 50) return '#f54a00';
      else if (num < 75) return '#d38700';
      else return '#3abfad';
    }

    return this.IconColor;
  }

  getBackgroundColor(): string {
    if (!this.IsPercentage) return this.BackgroundColor;

    const num = typeof this.Value === 'number' ? this.Value : undefined;
    if (num != undefined) {
      if (num < 50) return '#ffe2e2';
      else if (num < 75) return '#fef9c2';
      else return '#d8f2ef';
    }

    return this.BackgroundColor;
  }
}
