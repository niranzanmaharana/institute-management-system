import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { UiCard } from '../../shared/ui-card';
import { UiButton } from '../../shared/ui-button';
import { UiInput } from '../../shared/ui-input';
import { ErrorState } from '../../shared/error-state';
import { LoadingState } from '../../shared/loading-state';
import { httpErrorMessage, httpLoadError } from '../../core/http-error';
import { InstituteService } from './institute.service';

@Component({
  selector: 'app-institute-edit-page',
  imports: [ReactiveFormsModule, RouterLink, UiCard, UiButton, UiInput, ErrorState, LoadingState],
  templateUrl: './institute-edit.page.html',
  styleUrl: './institute-edit.page.scss',
})
export class InstituteEditPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly institutesApi = inject(InstituteService);

  readonly loading = signal(true);
  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);
  instituteId = 0;
  code = '';

  readonly form = this.fb.nonNullable.group({
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
        this.form.patchValue({
          name: row.name,
          timezone: row.timezone || 'Asia/Kolkata',
          mobile: row.mobile || '',
          contactEmail: row.adminEmail || '',
          website: row.website || '',
          addressLine1: row.addressLine1 || '',
          addressLine2: row.addressLine2 || '',
          city: row.city || '',
          state: row.state || '',
          postalCode: row.postalCode || '',
          country: row.country || 'India',
          iconUrl: row.iconUrl || '',
        });
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(httpLoadError(err, 'institute'));
        this.loading.set(false);
      },
    });
  }

  submit(): void {
    if (this.form.invalid || this.submitting() || !this.instituteId) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.error.set(null);
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
          iconUrl: opt(v.iconUrl),
        },
      })
      .subscribe({
        next: () => {
          this.submitting.set(false);
          void this.router.navigate(['/institutes', this.instituteId]);
        },
        error: (err) => {
          this.submitting.set(false);
          this.error.set(httpErrorMessage(err, 'Could not save institute.'));
        },
      });
  }
}
