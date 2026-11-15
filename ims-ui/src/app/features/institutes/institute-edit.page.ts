import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { UiCard } from '../../shared/ui-card';
import { UiButton } from '../../shared/ui-button';
import { UiInput } from '../../shared/ui-input';
import { UiSelect, UiSelectOption } from '../../shared/ui-select';
import { ErrorState } from '../../shared/error-state';
import { LoadingState } from '../../shared/loading-state';
import { ToastService } from '../../shared/toast.service';
import { httpErrorMessage, httpLoadError } from '../../core/http-error';
import { InstituteLogoComponent } from './institute-logo.component';
import { InstituteService, normalizeLogoType } from './institute.service';
import { DEFAULT_INSTITUTE_TIMEZONE, timezoneSelectOptions } from './timezones';

@Component({
  selector: 'app-institute-edit-page',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    UiCard,
    UiButton,
    UiInput,
    UiSelect,
    ErrorState,
    LoadingState,
    InstituteLogoComponent,
  ],
  templateUrl: './institute-edit.page.html',
  styleUrl: './institute-edit.page.scss',
})
export class InstituteEditPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly institutesApi = inject(InstituteService);
  private readonly toasts = inject(ToastService);

  readonly loading = signal(true);
  readonly submitting = signal(false);
  readonly logoUploading = signal(false);
  readonly logoPreviewUrl = signal<string | null>(null);
  readonly error = signal<string | null>(null);
  instituteId = 0;
  code = '';
  timezoneOptions: UiSelectOption[] = timezoneSelectOptions();

  readonly form = this.fb.nonNullable.group({
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
  });

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isFinite(id) || id <= 0) {
      this.error.set('Invalid institute id.');
      this.loading.set(false);
      return;
    }
    this.instituteId = id;
    this.institutesApi.get(id).subscribe({
      next: (row) => {
        this.code = row.code;
        const timezone = row.timezone || DEFAULT_INSTITUTE_TIMEZONE;
        this.timezoneOptions = timezoneSelectOptions(timezone);
        this.logoPreviewUrl.set(row.iconUrl || null);
        this.form.patchValue({
          name: row.name,
          timezone,
          mobile: row.mobile || '',
          contactEmail: row.adminEmail || '',
          website: row.website || '',
          addressLine1: row.addressLine1 || '',
          addressLine2: row.addressLine2 || '',
          city: row.city || '',
          state: row.state || '',
          postalCode: row.postalCode || '',
          country: row.country || 'India',
        });
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(httpLoadError(err, 'institute'));
        this.loading.set(false);
      },
    });
  }

  async onLogoSelected(file: File): Promise<void> {
    if (!this.instituteId || this.logoUploading()) {
      return;
    }
    if (!normalizeLogoType(file)) {
      this.toasts.warning('Logo must be JPEG or PNG.');
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      this.toasts.warning('Logo exceeds the maximum size of 5 MB.');
      return;
    }
    this.logoUploading.set(true);
    try {
      const updated = await this.institutesApi.uploadLogo(this.instituteId, file);
      this.logoPreviewUrl.set(updated.iconUrl || null);
      this.toasts.success('Logo updated.');
    } catch (err) {
      this.toasts.danger(logoUploadError(err));
    } finally {
      this.logoUploading.set(false);
    }
  }

  submit(): void {
    if (this.form.invalid || this.submitting() || !this.instituteId) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    const v = this.form.getRawValue();
    const opt = (s: string) => (s.trim().length > 0 ? s.trim() : undefined);

    this.institutesApi
      .update(this.instituteId, {
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
      })
      .subscribe({
        next: () => {
          this.submitting.set(false);
          void this.router.navigate(['/institutes', this.instituteId]);
        },
        error: (err) => {
          this.submitting.set(false);
          this.toasts.danger(httpErrorMessage(err, 'Could not save institute.'));
        },
      });
  }
}

function logoUploadError(err: unknown): string {
  if (err instanceof TypeError) {
    return 'Could not reach object storage. Start MinIO and retry.';
  }
  if (err instanceof Error && err.message) {
    return err.message;
  }
  return httpErrorMessage(err, 'Could not upload logo.');
}
