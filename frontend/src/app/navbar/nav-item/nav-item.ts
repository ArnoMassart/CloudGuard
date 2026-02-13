import { Component, Input } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { LucideAngularModule, LucideIconData, Shield } from 'lucide-angular';

@Component({
  selector: 'app-nav-item',
  imports: [LucideAngularModule, RouterLinkActive, RouterLink],
  templateUrl: './nav-item.html',
  styleUrl: './nav-item.css',
})
export class NavItem {
  @Input({ required: true }) Icon: LucideIconData = Shield;
  @Input({ required: true }) Label: string = '';
  @Input({ required: true }) Route: string = '';
}
