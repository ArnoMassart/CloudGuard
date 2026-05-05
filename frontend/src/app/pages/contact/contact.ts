import { HttpClient } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslocoPipe } from '@jsverse/transloco';
import { AppIcons } from '../../shared/AppIcons';
import { RouteService } from '../../services/route-service';
import { LucideAngularModule } from 'lucide-angular';
import { PageHeader } from '../../components/page-header/page-header';
import { PageContentWrapper } from '../../components/page-content-wrapper/page-content-wrapper';

@Component({
  selector: 'app-contact',
  standalone: true,
  imports: [
    TranslocoPipe,
    ReactiveFormsModule,
    LucideAngularModule,
    PageHeader,
    PageContentWrapper,
  ],
  templateUrl: './contact.html',
  styleUrl: './contact.css',
})
export class Contact {
  #fb = inject(FormBuilder);
  #http = inject(HttpClient);

  readonly Icons = AppIcons; // Voor de Lucide icons in de HTML

  contactForm: FormGroup = this.#fb.group({
    topic: ['support', Validators.required],
    subject: ['', [Validators.required, Validators.minLength(3)]],
    message: ['', [Validators.required, Validators.minLength(10)]],
  });

  isSubmitting = false;
  isSuccess = false;
  errorMessage = '';

  topics = [
    { value: 'support', label: 'contact.subject.technical-support' },
    { value: 'account', label: 'contact.subject.account-problems' },
    { value: 'feedback', label: 'contact.subject.feedback-suggestion' },
    { value: 'other', label: 'contact.subject.other' },
  ];

  onSubmit() {
    if (this.contactForm.invalid) {
      this.contactForm.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;
    this.errorMessage = '';

    const url = RouteService.getBackendUrl('/contact/send');

    this.#http
      .post(url, this.contactForm.value, { responseType: 'text', withCredentials: true })
      .subscribe({
        next: () => {
          this.isSubmitting = false;
          this.isSuccess = true;
          this.contactForm.reset({ topic: 'support' }); // Reset form na succes

          // Optioneel: Verberg de succesmelding na 5 seconden
          setTimeout(() => (this.isSuccess = false), 5000);
        },
        error: (err) => {
          this.isSubmitting = false;
          console.error('Fout bij verzenden contactformulier:', err);
          this.errorMessage =
            'Er is een fout opgetreden bij het verzenden van uw bericht. Probeer het later opnieuw.';
        },
      });
  }
}
