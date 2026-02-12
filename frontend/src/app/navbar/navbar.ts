import { Component } from '@angular/core';
import { LucideAngularModule, Shield } from 'lucide-angular';

@Component({
  standalone: true,
  selector: 'app-navbar',
  imports: [LucideAngularModule],
  templateUrl: './navbar.html',
  styleUrl: './navbar.css',
})
export class Navbar {
  readonly ShieldIcon = Shield;
}
