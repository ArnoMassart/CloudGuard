import { Component } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';
import { StatusLayout } from '../../components/status-layout/status-layout';
import { StatusErrorCard } from '../../components/status-error-card/status-error-card';

@Component({
  selector: 'app-denied',
  imports: [LucideAngularModule, StatusLayout, StatusErrorCard],
  templateUrl: './denied.html',
  styleUrl: './denied.css',
})
export class Denied {}
