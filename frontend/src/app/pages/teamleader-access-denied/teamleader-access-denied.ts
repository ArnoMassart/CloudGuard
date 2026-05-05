import { Component } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';
import { StatusLayout } from '../../components/status-layout/status-layout';
import { StatusErrorCard } from '../../components/status-error-card/status-error-card';

@Component({
  selector: 'app-teamleader-access-denied',
  imports: [LucideAngularModule, StatusLayout, StatusErrorCard],
  templateUrl: './teamleader-access-denied.html',
  styleUrl: './teamleader-access-denied.css',
})
export class TeamleaderAccessDenied {}
