import { Component } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';
import { StatusLayout } from '../../components/status-layout/status-layout';
import { StatusErrorCard } from '../../components/status-error-card/status-error-card';

@Component({
  selector: 'app-forbidden',
  imports: [LucideAngularModule, StatusLayout, StatusErrorCard],
  templateUrl: './forbidden.html',
  styleUrl: './forbidden.css',
})
export class Forbidden {}
