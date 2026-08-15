import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { UiCard } from '../../shared/ui-card';
import { UiButton } from '../../shared/ui-button';
import { UiInput } from '../../shared/ui-input';
import { ErrorState } from '../../shared/error-state';
import { httpErrorMessage } from '../../core/http-error';
import { CodeService } from '../../shared/code.service';
import { StaffService } from './staff.service';

@Component({
  selector: 'app-staff-create-page',
  imports: [ReactiveFormsModule, RouterLink, UiCard, UiButton, UiInput, ErrorState],
  templateUrl: './staff-create.page.html',
  styleUrl: './staff-create.page.scss',
})
export class StaffCreatePage {
  private readonly fb = inject(FormBuilder);
  private readonly staffApi = inject(StaffService);
  private readonly codesApi = inject(CodeService);
  private readonly router = inject(Router);

  readonly submitting = signal(false);
  readonly generating = signal(false);
  readonly error = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    staffCode: [''],
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
    phone: [''],
    email: [''],
    designation: [''],
  });

  generateCode(): void {
    if (this.generating()) {
      return;
    }
    this.generating.set(true);
    this.error.set(null);
    this.codesApi.next('STAFF').subscribe({
      next: (res) => {
        this.form.controls.staffCode.setValue(res.code);
        this.generating.set(false);
      },
      error: (err) => {
        this.generating.set(false);
        this.error.set(httpErrorMessage(err, 'Could not generate staff code.'));
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

    this.staffApi
      .create({
        staffCode: v.staffCode.trim() || undefined,
        firstName: v.firstName,
        lastName: v.lastName,
        phone: v.phone || undefined,
        email: v.email || undefined,
        designation: v.designation.trim() || undefined,
      })
      .subscribe({
        next: () => {
          this.submitting.set(false);
          void this.router.navigateByUrl('/staff');
        },
        error: (err) => {
          this.submitting.set(false);
          const status = err?.status;
          if (status === 409) {
            this.error.set('Staff code already exists in this institute.');
          } else {
            this.error.set(httpErrorMessage(err, 'Could not create staff.'));
          }
        },
      });
  }
}
