import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { UiCard } from '../../shared/ui-card';
import { UiButton } from '../../shared/ui-button';
import { UiInput } from '../../shared/ui-input';
import { ErrorState } from '../../shared/error-state';
import { httpErrorMessage } from '../../core/http-error';
import { CodeService } from '../../shared/code.service';
import { InstituteService } from './institute.service';

@Component({
  selector: 'app-institute-create-page',
  imports: [ReactiveFormsModule, RouterLink, UiCard, UiButton, UiInput, ErrorState],
  templateUrl: './institute-create.page.html',
  styleUrl: './institute-create.page.scss',
})
export class InstituteCreatePage {
  private readonly fb = inject(FormBuilder);
  private readonly institutesApi = inject(InstituteService);
  private readonly codesApi = inject(CodeService);
  private readonly router = inject(Router);

  readonly submitting = signal(false);
  readonly generating = signal(false);
  readonly error = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    code: ['', [Validators.maxLength(32)]],
    name: ['', [Validators.required, Validators.maxLength(255)]],
    timezone: ['Asia/Kolkata'],
    mobile: [''],
    contactEmail: ['', Validators.email],
    website: [''],
    addressLine1: [''],
    addressLine2: [''],
    city: [''],
    state: [''],
    postalCode: [''],
    country: ['India'],
    iconUrl: [''],
    adminUsername: ['admin', Validators.required],
    adminEmail: ['', [Validators.required, Validators.email]],
    adminPassword: ['Password@123', [Validators.required, Validators.minLength(8)]],
  });

  generateCode(): void {
    if (this.generating()) {
      return;
    }
    this.generating.set(true);
    this.error.set(null);
    this.codesApi.next('INSTITUTE').subscribe({
      next: (res) => {
        this.form.controls.code.setValue(res.code);
        this.generating.set(false);
      },
      error: (err) => {
        this.generating.set(false);
        this.error.set(httpErrorMessage(err, 'Could not generate institute code.'));
      },
    });
  }

  submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.error.set(null);
    const v = this.form.getRawValue();
    const opt = (s: string) => (s.trim().length > 0 ? s.trim() : undefined);

    this.institutesApi
      .create({
        code: v.code.trim() || undefined,
        name: v.name.trim(),
        timezone: v.timezone.trim() || undefined,
        profile: {
          mobile: opt(v.mobile),
          adminEmail: opt(v.contactEmail),
          website: opt(v.website),
          addressLine1: opt(v.addressLine1),
          addressLine2: opt(v.addressLine2),
          city: opt(v.city),
          state: opt(v.state),
          postalCode: opt(v.postalCode),
          country: opt(v.country),
          iconUrl: opt(v.iconUrl),
        },
        initialAdmin: {
          username: v.adminUsername.trim(),
          email: v.adminEmail.trim(),
          password: v.adminPassword,
        },
      })
      .subscribe({
        next: () => {
          this.submitting.set(false);
          void this.router.navigateByUrl('/institutes');
        },
        error: (err) => {
          this.submitting.set(false);
          const status = err?.status;
          if (status === 409) {
            this.error.set('Institute code already exists.');
          } else {
            this.error.set(httpErrorMessage(err, 'Could not create institute.'));
          }
        },
      });
  }
}
