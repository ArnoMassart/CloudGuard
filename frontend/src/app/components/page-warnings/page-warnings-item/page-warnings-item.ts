import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-page-warnings-item',
  imports: [],
  templateUrl: './page-warnings-item.html',
  styleUrl: './page-warnings-item.css',
  standalone: true,
})
export class PageWarningsItem {
  @Input() showBullet: boolean = false;
  @Input() isCritical: boolean = false;
}
