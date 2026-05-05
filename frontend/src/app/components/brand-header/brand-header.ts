import { Component } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';
import { AppIcons } from '../../shared/AppIcons';

@Component({
  selector: 'app-brand-header',
  standalone: true,
  imports: [LucideAngularModule],
  templateUrl: './brand-header.html',
  styleUrl: './brand-header.css',
})
export class BrandHeader {
  readonly Icons = AppIcons;
}
