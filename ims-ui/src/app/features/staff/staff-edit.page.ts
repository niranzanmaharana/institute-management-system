import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { UiCard } from '../../shared/ui-card';
import { UiButton } from '../../shared/ui-button';
import { UiInput } from '../../shared/ui-input';
import { ErrorState } from '../../shared/error-state';
import { LoadingState } from '../../shared/loading-state';
import { httpErrorMessage, httpLoadError } from '../../core/http-error';
import { StaffService } from './staff.service';

@Component({
  selector: 'app-staff-edit-page',
  imports: [ReactiveFormsModule, RouterLink, UiCard, UiButton, UiInput, ErrorState, LoadingState],
  templateUrl: './staff-edit.page.html',
  styleUrl: './staff-edit.page.scss',
})
export class StaffEditPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly staffApi = inject(StaffService);

  readonly loading = signal(true);
  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);
  staffId = 0;
  staffCode = '';

  readonly form = this.fb.nonNullable.group({
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
    phone: [''],
    email: [''],
    designation: [''],
  });

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isFinite(id) || id <= 0) {
      this.error.set('Invalid staff id.');
      this.loading.set(false);
      return;
    }
    this.staffId = id;
    this.staffApi.get(id).subscribe({
      next: (row) => {
        this.staffCode = row.staffCode;
        this.form.patchValue({
          firstName: row.firstName,
          lastName: row.lastName,
          phone: row.phone || '',
          email: row.email || '',
          designation: row.designation || '',
        });
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(httpLoadError(err, 'staff'));
        this.loading.set(false);
      },
    });
  }

  submit(): void {
    if (this.form.invalid || this.submitting() || !this.staffId) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.error.set(null);
    const v = this.form.getRawValue();

    this.staffApi
      .update(this.staffId, {
        firstName: v.firstName.trim(),
        lastName: v.lastName.trim(),
        phone: v.phone.trim() || undefined,
        email: v.email.trim() || undefined,
        designation: v.designation.trim() || undefined,
      })
      .subscribe({
        next: () => {
          this.submitting.set(false);
          void this.router.navigate(['/staff', this.staffId]);
        },
        error: (err) => {
          this.submitting.set(false);
          this.error.set(httpErrorMessage(err, 'Could not save staff.'));
        },
      });
  }
}
