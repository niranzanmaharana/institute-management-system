import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { UiCard } from '../../shared/ui-card';
import { UiButton } from '../../shared/ui-button';
import { UiInput } from '../../shared/ui-input';
import { ErrorState } from '../../shared/error-state';
import { httpErrorMessage } from '../../core/http-error';
import { CodeService } from '../../shared/code.service';
import { FacultyService } from './faculty.service';

@Component({
  selector: 'app-faculty-create-page',
  imports: [ReactiveFormsModule, RouterLink, UiCard, UiButton, UiInput, ErrorState],
  templateUrl: './faculty-create.page.html',
  styleUrl: './faculty-create.page.scss',
})
export class FacultyCreatePage {
  private readonly fb = inject(FormBuilder);
  private readonly facultyApi = inject(FacultyService);
  private readonly codesApi = inject(CodeService);
  private readonly router = inject(Router);

  readonly submitting = signal(false);
  readonly generating = signal(false);
  readonly error = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    facultyCode: [''],
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
    phone: [''],
    email: [''],
    department: [''],
    addressLine1: [''],
    city: [''],
    state: [''],
    postalCode: [''],
  });

  generateCode(): void {
    if (this.generating()) {
      return;
    }
    this.generating.set(true);
    this.error.set(null);
    this.codesApi.next('FACULTY').subscribe({
      next: (res) => {
        this.form.controls.facultyCode.setValue(res.code);
        this.generating.set(false);
      },
      error: (err) => {
        this.generating.set(false);
        this.error.set(httpErrorMessage(err, 'Could not generate faculty code.'));
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
    const addresses =
      v.addressLine1.trim().length > 0 && v.city.trim().length > 0
        ? [
            {
              line1: v.addressLine1.trim(),
              city: v.city.trim(),
              state: v.state.trim() || undefined,
              postalCode: v.postalCode.trim() || undefined,
              country: 'India',
              primaryAddress: true,
            },
          ]
        : [];

    this.facultyApi
      .create({
        facultyCode: v.facultyCode.trim() || undefined,
        firstName: v.firstName,
        lastName: v.lastName,
        phone: v.phone || undefined,
        email: v.email || undefined,
        department: v.department.trim() || undefined,
        addresses,
      })
      .subscribe({
        next: () => {
          this.submitting.set(false);
          void this.router.navigateByUrl('/faculty');
        },
        error: (err) => {
          this.submitting.set(false);
          const status = err?.status;
          if (status === 409) {
            this.error.set('Faculty code already exists in this institute.');
          } else {
            this.error.set(httpErrorMessage(err, 'Could not create faculty.'));
          }
        },
      });
  }
}
