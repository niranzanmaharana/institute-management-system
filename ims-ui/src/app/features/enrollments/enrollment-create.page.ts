import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { UiCard } from '../../shared/ui-card';
import { UiButton } from '../../shared/ui-button';
import { LoadingState } from '../../shared/loading-state';
import { ErrorState } from '../../shared/error-state';
import { StatusBadge } from '../../shared/status-badge';
import { httpErrorMessage, httpLoadError } from '../../core/http-error';
import { AcademicService, Batch, FeePlan } from '../academic/academic.service';
import { Student, StudentService } from '../students/student.service';
import { ActivationResult, Enrollment, EnrollmentService } from './enrollment.service';

@Component({
  selector: 'app-enrollment-create-page',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    UiCard,
    UiButton,
    LoadingState,
    ErrorState,
    StatusBadge,
  ],
  templateUrl: './enrollment-create.page.html',
  styleUrl: './enrollment-create.page.scss',
})
export class EnrollmentCreatePage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly enrollmentsApi = inject(EnrollmentService);
  private readonly studentsApi = inject(StudentService);
  private readonly academicApi = inject(AcademicService);

  readonly loading = signal(true);
  readonly loadError = signal<string | null>(null);
  readonly submitting = signal(false);
  readonly activating = signal(false);
  readonly error = signal<string | null>(null);
  readonly activateError = signal<string | null>(null);

  readonly students = signal<Student[]>([]);
  readonly batches = signal<Batch[]>([]);
  readonly feePlans = signal<FeePlan[]>([]);

  readonly created = signal<Enrollment | null>(null);
  readonly activation = signal<ActivationResult | null>(null);

  readonly form = this.fb.nonNullable.group({
    studentId: ['', Validators.required],
    batchId: ['', Validators.required],
    feePlanId: ['', Validators.required],
  });

  ngOnInit(): void {
    this.form.controls.batchId.valueChanges.subscribe((batchId) => {
      this.onBatchChanged(Number(batchId));
    });
    this.reloadLookups();
  }

  reloadLookups(): void {
    this.loading.set(true);
    this.loadError.set(null);
    forkJoin({
      students: this.studentsApi.list('', 0, 100),
      batches: this.academicApi.listBatches(),
    }).subscribe({
      next: ({ students, batches }) => {
        this.students.set(students.content);
        this.batches.set(batches);
        if (students.content.length > 0) {
          this.form.controls.studentId.setValue(String(students.content[0].id));
        }
        if (batches.length > 0) {
          this.form.controls.batchId.setValue(String(batches[0].id));
        } else {
          this.loading.set(false);
        }
      },
      error: (err) => {
        this.loadError.set(httpLoadError(err, 'students or batches'));
        this.loading.set(false);
      },
    });
  }

  private onBatchChanged(batchId: number): void {
    if (!Number.isFinite(batchId) || batchId <= 0) {
      this.feePlans.set([]);
      this.form.controls.feePlanId.setValue('');
      this.loading.set(false);
      return;
    }
    const batch = this.batches().find((b) => b.id === batchId);
    if (!batch) {
      this.feePlans.set([]);
      this.form.controls.feePlanId.setValue('');
      this.loading.set(false);
      return;
    }
    this.loading.set(true);
    this.academicApi.listFeePlans(batch.courseId).subscribe({
      next: (plans) => {
        this.feePlans.set(plans);
        if (plans.length > 0) {
          this.form.controls.feePlanId.setValue(String(plans[0].id));
        } else {
          this.form.controls.feePlanId.setValue('');
        }
        this.loading.set(false);
      },
      error: (err) => {
        this.feePlans.set([]);
        this.form.controls.feePlanId.setValue('');
        this.loadError.set(httpLoadError(err, 'fee plans'));
        this.loading.set(false);
      },
    });
  }

  submit(): void {
    if (this.form.invalid || this.submitting() || this.created()) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const studentId = Number(v.studentId);
    const batchId = Number(v.batchId);
    const feePlanId = Number(v.feePlanId);
    if (!studentId || !batchId || !feePlanId) {
      this.error.set('Select student, batch, and fee plan.');
      return;
    }

    this.submitting.set(true);
    this.error.set(null);
    this.enrollmentsApi.create({ studentId, batchId, feePlanId }).subscribe({
      next: (enrollment) => {
        this.created.set(enrollment);
        this.submitting.set(false);
      },
      error: (err) => {
        this.submitting.set(false);
        if (err?.status === 409) {
          this.error.set('Enrollment already exists or capacity exceeded.');
        } else {
          this.error.set(httpErrorMessage(err, 'Could not create enrollment.'));
        }
      },
    });
  }

  activate(): void {
    const enrollment = this.created();
    if (!enrollment || this.activating() || this.activation()) {
      return;
    }
    this.activating.set(true);
    this.activateError.set(null);
    const key = crypto.randomUUID();
    this.enrollmentsApi.activate(enrollment.id, {}, key).subscribe({
      next: (result) => {
        this.activation.set(result);
        this.created.update((e) =>
          e ? { ...e, status: result.enrollmentStatus, activatedAt: result.activatedAt } : e,
        );
        this.activating.set(false);
      },
      error: (err) => {
        this.activating.set(false);
        this.activateError.set(httpErrorMessage(err, 'Could not activate enrollment.'));
      },
    });
  }

  reset(): void {
    this.created.set(null);
    this.activation.set(null);
    this.error.set(null);
    this.activateError.set(null);
  }
}
