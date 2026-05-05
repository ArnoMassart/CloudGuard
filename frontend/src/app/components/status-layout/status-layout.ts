import { Component } from '@angular/core';
import { BrandHeader } from '../brand-header/brand-header';
import { BrandFooter } from '../brand-footer/brand-footer';

@Component({
  selector: 'app-status-layout',
  standalone: true,
  imports: [BrandHeader, BrandFooter],
  templateUrl: './status-layout.html',
  styleUrl: './status-layout.css',
})
export class StatusLayout {}
