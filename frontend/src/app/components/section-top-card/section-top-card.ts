import { Component, Input, input } from '@angular/core';
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
  /** Signal input so template bindings accept `number | null` (e.g. dashboard score). */
  readonly Value = input<string | number | null | undefined>(0);
  @Input() Icon: LucideIconData = this.Icons.Shield;
  @Input() BackgroundColor: string = '#dbeafe';
  @Input() IconColor: string = '#155dfc';
  @Input() TextColor: string = 'black';
  @Input() IsPercentage: boolean = false;
  @Input() IsCurrency: boolean = false;

  private readonly naMuted = '#94a3b8';

  getValue(): string {
    const v = this.Value();
    if (this.IsPercentage && (v === undefined || v === null)) {
      return '—';
    }

    let result = '';

    if (v === undefined || v === null) {
      result = '0';
    } else if (this.IsCurrency) {
      result = '€';
      if (typeof v === 'number') {
        result += v.toFixed(2);
      } else {
        result += v;
      }
    } else {
      result = v.toString();
    }

    if (this.IsPercentage) {
      result += '%';
    }

    return result;
  }

  getTextColor(): string {
    if (!this.IsPercentage) return this.TextColor;

    const v = this.Value();
    if (v === undefined || v === null) {
      return this.naMuted;
    }

    const num = typeof v === 'number' ? v : undefined;
    if (num != undefined) {
      if (num < 50) return '#e7000b';
      else if (num < 75) return '#d38700';
      else return '#3abfad';
    }

    return this.TextColor;
  }

  getIconColor(): string {
    if (!this.IsPercentage) return this.IconColor;

    const v = this.Value();
    if (v === undefined || v === null) {
      return this.naMuted;
    }

    const num = typeof v === 'number' ? v : undefined;
    if (num != undefined) {
      if (num < 50) return '#f54a00';
      else if (num < 75) return '#d38700';
      else return '#3abfad';
    }

    return this.IconColor;
  }

  getBackgroundColor(): string {
    if (!this.IsPercentage) return this.BackgroundColor;

    const v = this.Value();
    if (v === undefined || v === null) {
      return '#f1f5f9';
    }

    const num = typeof v === 'number' ? v : undefined;
    if (num != undefined) {
      if (num < 50) return '#ffe2e2';
      else if (num < 75) return '#fef9c2';
      else return '#d8f2ef';
    }

    return this.BackgroundColor;
  }
}
