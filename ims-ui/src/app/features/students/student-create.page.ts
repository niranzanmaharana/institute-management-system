import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { UiCard } from '../../shared/ui-card';
import { UiButton } from '../../shared/ui-button';
import { UiInput } from '../../shared/ui-input';
import { ErrorState } from '../../shared/error-state';
import { httpErrorMessage } from '../../core/http-error';
import { CodeService } from '../../shared/code.service';
import { StudentService } from './student.service';

@Component({
  selector: 'app-student-create-page',
  imports: [ReactiveFormsModule, RouterLink, UiCard, UiButton, UiInput, ErrorState],
  templateUrl: './student-create.page.html',
  styleUrl: './student-create.page.scss',
})
export class StudentCreatePage {
  private readonly fb = inject(FormBuilder);
  private readonly studentsApi = inject(StudentService);
  private readonly codesApi = inject(CodeService);
  private readonly router = inject(Router);

  readonly submitting = signal(false);
  readonly generating = signal(false);
  readonly error = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    studentCode: [''],
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
    phone: [''],
    email: [''],
    guardianName: [''],
    guardianPhone: [''],
    guardianRelation: ['Parent'],
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
    this.codesApi.next('STUDENT').subscribe({
      next: (res) => {
        this.form.controls.studentCode.setValue(res.code);
        this.generating.set(false);
      },
      error: (err) => {
        this.generating.set(false);
        this.error.set(httpErrorMessage(err, 'Could not generate student code.'));
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
    const guardians =
      v.guardianName.trim().length > 0
        ? [
            {
              name: v.guardianName.trim(),
              phone: v.guardianPhone.trim() || undefined,
              relation: v.guardianRelation.trim() || 'Parent',
              primaryGuardian: true,
            },
          ]
        : [];
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

    this.studentsApi
      .create({
        studentCode: v.studentCode.trim() || undefined,
        firstName: v.firstName,
        lastName: v.lastName,
        phone: v.phone || undefined,
        email: v.email || undefined,
        guardians,
        addresses,
      })
      .subscribe({
        next: () => {
          this.submitting.set(false);
          void this.router.navigateByUrl('/students');
        },
        error: (err) => {
          this.submitting.set(false);
          const status = err?.status;
          if (status === 409) {
            this.error.set('Student code already exists in this institute.');
          } else {
            this.error.set(httpErrorMessage(err, 'Could not create student.'));
          }
        },
      });
  }
}
