import { CommonModule } from '@angular/common';
import { Component, effect, input, signal, untracked } from '@angular/core';

@Component({
  selector: 'app-user-avatar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './user-avatar.html',
})
export class UserAvatar {
  readonly pictureUrl = input<string | null | undefined>(undefined);
  readonly initials = input.required<string>();
  /** Tailwind size classes, e.g. w-10 h-10 */
  readonly sizeClass = input<string>('w-10 h-10');
  /** Tailwind text size for initials, e.g. text-xs */
  readonly textSizeClass = input<string>('text-xs');

  readonly imageFailed = signal(false);

  constructor() {
    effect(() => {
      this.pictureUrl();
      untracked(() => this.imageFailed.set(false));
    });
  }

  onImgError(): void {
    this.imageFailed.set(true);
  }
}
