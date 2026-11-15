import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { UiCard } from '../../shared/ui-card';
import { UiButton } from '../../shared/ui-button';
import { UiInput } from '../../shared/ui-input';
import { UiSelect } from '../../shared/ui-select';
import { ToastService } from '../../shared/toast.service';
import { httpErrorMessage } from '../../core/http-error';
import { CodeService } from '../../shared/code.service';
import { InstituteLogoComponent } from './institute-logo.component';
import { InstituteService, normalizeLogoType } from './institute.service';
import { DEFAULT_INSTITUTE_TIMEZONE, timezoneSelectOptions } from './timezones';

@Component({
  selector: 'app-institute-create-page',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    UiCard,
    UiButton,
    UiInput,
    UiSelect,
    InstituteLogoComponent,
  ],
  templateUrl: './institute-create.page.html',
  styleUrl: './institute-create.page.scss',
})
export class InstituteCreatePage {
  private readonly fb = inject(FormBuilder);
  private readonly institutesApi = inject(InstituteService);
  private readonly codesApi = inject(CodeService);
  private readonly router = inject(Router);
  private readonly toasts = inject(ToastService);

  readonly submitting = signal(false);
  readonly generating = signal(false);
  readonly logoUploading = signal(false);
  readonly logoPreviewUrl = signal<string | null>(null);
  private logoFile: File | null = null;
  private logoObjectUrl: string | null = null;
  readonly timezoneOptions = timezoneSelectOptions();

  readonly form = this.fb.nonNullable.group({
    code: ['', [Validators.maxLength(32)]],
    name: ['', [Validators.required, Validators.maxLength(255)]],
    timezone: [DEFAULT_INSTITUTE_TIMEZONE, Validators.required],
    mobile: [''],
    contactEmail: ['', Validators.email],
    website: [''],
    addressLine1: [''],
    addressLine2: [''],
    city: [''],
    state: [''],
    postalCode: [''],
    country: ['India'],
    adminUsername: ['admin', Validators.required],
    adminEmail: ['', [Validators.required, Validators.email]],
    adminPassword: ['Password@123', [Validators.required, Validators.minLength(8)]],
  });

  generateCode(): void {
    if (this.generating()) {
      return;
    }
    this.generating.set(true);
    this.codesApi.next('INSTITUTE').subscribe({
      next: (res) => {
        this.form.controls.code.setValue(res.code);
        this.generating.set(false);
      },
      error: (err) => {
        this.generating.set(false);
        this.toasts.danger(httpErrorMessage(err, 'Could not generate institute code.'));
      },
    });
  }

  onLogoSelected(file: File): void {
    if (!normalizeLogoType(file)) {
      this.toasts.warning('Logo must be JPEG or PNG.');
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      this.toasts.warning('Logo exceeds the maximum size of 5 MB.');
      return;
    }
    this.logoFile = file;
    if (this.logoObjectUrl) {
      URL.revokeObjectURL(this.logoObjectUrl);
    }
    this.logoObjectUrl = URL.createObjectURL(file);
    this.logoPreviewUrl.set(this.logoObjectUrl);
  }

  submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
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
        },
        initialAdmin: {
          username: v.adminUsername.trim(),
          email: v.adminEmail.trim(),
          password: v.adminPassword,
        },
      })
      .subscribe({
        next: async (created) => {
          if (this.logoFile) {
            this.logoUploading.set(true);
            try {
              await this.institutesApi.uploadLogo(created.id, this.logoFile);
            } catch (err) {
              this.logoUploading.set(false);
              this.submitting.set(false);
              this.toasts.danger(logoUploadError(err));
              void this.router.navigate(['/institutes', created.id, 'edit']);
              return;
            }
            this.logoUploading.set(false);
          }
          this.submitting.set(false);
          void this.router.navigateByUrl('/institutes');
        },
        error: (err) => {
          this.submitting.set(false);
          const status = err?.status;
          if (status === 409) {
            this.toasts.danger('Institute code already exists.');
          } else {
            this.toasts.danger(httpErrorMessage(err, 'Could not create institute.'));
          }
        },
      });
  }
}

function logoUploadError(err: unknown): string {
  if (err instanceof TypeError) {
    return 'Institute created, but logo upload failed — start MinIO and retry from Edit.';
  }
  if (err instanceof Error && err.message) {
    return `Institute created, but logo upload failed: ${err.message}`;
  }
  return httpErrorMessage(err, 'Institute created, but logo upload failed. Retry from Edit.');
}
