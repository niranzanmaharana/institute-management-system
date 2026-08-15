import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { UiCard } from '../../shared/ui-card';
import { UiButton } from '../../shared/ui-button';
import { UiInput } from '../../shared/ui-input';
import { ErrorState } from '../../shared/error-state';
import { LoadingState } from '../../shared/loading-state';
import { httpErrorMessage, httpLoadError } from '../../core/http-error';
import { StudentService } from './student.service';

@Component({
  selector: 'app-student-edit-page',
  imports: [ReactiveFormsModule, RouterLink, UiCard, UiButton, UiInput, ErrorState, LoadingState],
  templateUrl: './student-edit.page.html',
  styleUrl: './student-edit.page.scss',
})
export class StudentEditPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly studentsApi = inject(StudentService);

  readonly loading = signal(true);
  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);
  studentId = 0;
  studentCode = '';

  readonly form = this.fb.nonNullable.group({
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

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isFinite(id) || id <= 0) {
      this.error.set('Invalid student id.');
      this.loading.set(false);
      return;
    }
    this.studentId = id;
    this.studentsApi.get(id).subscribe({
      next: (row) => {
        this.studentCode = row.studentCode;
        const g = row.guardians.find((x) => x.primaryGuardian) ?? row.guardians[0];
        const a = row.addresses.find((x) => x.primaryAddress) ?? row.addresses[0];
        this.form.patchValue({
          firstName: row.firstName,
          lastName: row.lastName,
          phone: row.phone || '',
          email: row.email || '',
          guardianName: g?.name || '',
          guardianPhone: g?.phone || '',
          guardianRelation: g?.relation || 'Parent',
          addressLine1: a?.line1 || '',
          city: a?.city || '',
          state: a?.state || '',
          postalCode: a?.postalCode || '',
        });
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(httpLoadError(err, 'student'));
        this.loading.set(false);
      },
    });
  }

  submit(): void {
    if (this.form.invalid || this.submitting() || !this.studentId) {
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
      .update(this.studentId, {
        firstName: v.firstName.trim(),
        lastName: v.lastName.trim(),
        phone: v.phone.trim() || undefined,
        email: v.email.trim() || undefined,
        guardians,
        addresses,
      })
      .subscribe({
        next: () => {
          this.submitting.set(false);
          void this.router.navigate(['/students', this.studentId]);
        },
        error: (err) => {
          this.submitting.set(false);
          this.error.set(httpErrorMessage(err, 'Could not save student.'));
        },
      });
  }
}
