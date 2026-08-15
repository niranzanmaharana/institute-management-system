import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { UiCard } from '../../shared/ui-card';
import { UiButton } from '../../shared/ui-button';
import { UiInput } from '../../shared/ui-input';
import { ErrorState } from '../../shared/error-state';
import { LoadingState } from '../../shared/loading-state';
import { httpErrorMessage, httpLoadError } from '../../core/http-error';
import { FacultyService } from './faculty.service';

@Component({
  selector: 'app-faculty-edit-page',
  imports: [ReactiveFormsModule, RouterLink, UiCard, UiButton, UiInput, ErrorState, LoadingState],
  templateUrl: './faculty-edit.page.html',
  styleUrl: './faculty-edit.page.scss',
})
export class FacultyEditPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly facultyApi = inject(FacultyService);

  readonly loading = signal(true);
  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);
  facultyId = 0;
  facultyCode = '';

  readonly form = this.fb.nonNullable.group({
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

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isFinite(id) || id <= 0) {
      this.error.set('Invalid faculty id.');
      this.loading.set(false);
      return;
    }
    this.facultyId = id;
    this.facultyApi.get(id).subscribe({
      next: (row) => {
        this.facultyCode = row.facultyCode;
        const a = row.addresses.find((x) => x.primaryAddress) ?? row.addresses[0];
        this.form.patchValue({
          firstName: row.firstName,
          lastName: row.lastName,
          phone: row.phone || '',
          email: row.email || '',
          department: row.department || '',
          addressLine1: a?.line1 || '',
          city: a?.city || '',
          state: a?.state || '',
          postalCode: a?.postalCode || '',
        });
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(httpLoadError(err, 'faculty'));
        this.loading.set(false);
      },
    });
  }

  submit(): void {
    if (this.form.invalid || this.submitting() || !this.facultyId) {
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
      .update(this.facultyId, {
        firstName: v.firstName.trim(),
        lastName: v.lastName.trim(),
        phone: v.phone.trim() || undefined,
        email: v.email.trim() || undefined,
        department: v.department.trim() || undefined,
        addresses,
      })
      .subscribe({
        next: () => {
          this.submitting.set(false);
          void this.router.navigate(['/faculty', this.facultyId]);
        },
        error: (err) => {
          this.submitting.set(false);
          this.error.set(httpErrorMessage(err, 'Could not save faculty.'));
        },
      });
  }
}
