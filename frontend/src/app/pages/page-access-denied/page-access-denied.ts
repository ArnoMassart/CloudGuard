import { Component } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';
import { StatusLayout } from '../../components/status-layout/status-layout';
import { StatusErrorCard } from '../../components/status-error-card/status-error-card';

@Component({
  selector: 'app-page-access-denied',
  imports: [LucideAngularModule, StatusLayout, StatusErrorCard],
  templateUrl: './page-access-denied.html',
  styleUrl: './page-access-denied.css',
})
export class PageAccessDenied {}
