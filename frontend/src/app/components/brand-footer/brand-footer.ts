import { Component } from '@angular/core';
import { LanguageBar } from '../language-bar/language-bar';

@Component({
  selector: 'app-brand-footer',
  standalone: true,
  imports: [LanguageBar],
  templateUrl: './brand-footer.html',
  styleUrl: './brand-footer.css',
})
export class BrandFooter {}
