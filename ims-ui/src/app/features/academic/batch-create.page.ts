import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { UiCard } from '../../shared/ui-card';
import { UiButton } from '../../shared/ui-button';
import { UiInput } from '../../shared/ui-input';
import { UiDatepicker } from '../../shared/ui-datepicker';
import { LoadingState } from '../../shared/loading-state';
import { ErrorState } from '../../shared/error-state';
import { ToastService } from '../../shared/toast.service';
import { httpErrorMessage, httpLoadError } from '../../core/http-error';
import { CodeService } from '../../shared/code.service';
import { AcademicService, AcademicYear, Course } from './academic.service';

@Component({
  selector: 'app-batch-create-page',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    UiCard,
    UiButton,
    UiInput,
    UiDatepicker,
    LoadingState,
    ErrorState,
  ],
  templateUrl: './batch-create.page.html',
  styleUrl: './batch-create.page.scss',
})
export class BatchCreatePage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly academicApi = inject(AcademicService);
  private readonly codesApi = inject(CodeService);
  private readonly router = inject(Router);
  private readonly toasts = inject(ToastService);

  readonly loading = signal(true);
  readonly loadError = signal<string | null>(null);
  readonly submitting = signal(false);
  readonly yearSubmitting = signal(false);
  readonly generatingBatch = signal(false);
  readonly generatingYear = signal(false);

  readonly courses = signal<Course[]>([]);
  readonly years = signal<AcademicYear[]>([]);

  readonly form = this.fb.nonNullable.group({
    courseId: ['', Validators.required],
    academicYearId: ['', Validators.required],
    code: [''],
    name: ['', Validators.required],
    capacity: ['30', Validators.required],
    startDate: [''],
    endDate: [''],
  });

  readonly yearForm = this.fb.nonNullable.group({
    code: [''],
    name: ['', Validators.required],
    startDate: ['', Validators.required],
    endDate: ['', Validators.required],
  });

  ngOnInit(): void {
    this.reloadLookups();
  }

  reloadLookups(): void {
    this.loading.set(true);
    this.loadError.set(null);
    forkJoin({
      courses: this.academicApi.listCourses('', 0, 100),
      years: this.academicApi.listAcademicYears(),
    }).subscribe({
      next: ({ courses, years }) => {
        this.courses.set(courses.content);
        this.years.set(years);
        if (courses.content.length > 0 && !this.form.controls.courseId.value) {
          this.form.controls.courseId.setValue(String(courses.content[0].id));
        }
        if (years.length > 0 && !this.form.controls.academicYearId.value) {
          this.form.controls.academicYearId.setValue(String(years[0].id));
        }
        this.loading.set(false);
      },
      error: (err) => {
        this.loadError.set(httpLoadError(err, 'courses or academic years'));
        this.loading.set(false);
      },
    });
  }

  generateBatchCode(): void {
    if (this.generatingBatch()) {
      return;
    }
    this.generatingBatch.set(true);
    this.codesApi.next('BATCH').subscribe({
      next: (res) => {
        this.form.controls.code.setValue(res.code);
        this.generatingBatch.set(false);
      },
      error: (err) => {
        this.generatingBatch.set(false);
        this.toasts.danger(httpErrorMessage(err, 'Could not generate batch code.'));
      },
    });
  }

  generateYearCode(): void {
    if (this.generatingYear()) {
      return;
    }
    this.generatingYear.set(true);
    this.codesApi.next('ACADEMIC_YEAR').subscribe({
      next: (res) => {
        this.yearForm.controls.code.setValue(res.code);
        this.generatingYear.set(false);
      },
      error: (err) => {
        this.generatingYear.set(false);
        this.toasts.danger(httpErrorMessage(err, 'Could not generate academic year code.'));
      },
    });
  }

  createYear(): void {
    if (this.yearForm.invalid || this.yearSubmitting()) {
      this.yearForm.markAllAsTouched();
      return;
    }
    this.yearSubmitting.set(true);
    const v = this.yearForm.getRawValue();
    this.academicApi
      .createAcademicYear({
        code: v.code.trim() || undefined,
        name: v.name.trim(),
        startDate: v.startDate,
        endDate: v.endDate,
      })
      .subscribe({
        next: (year) => {
          this.years.update((list) => [...list, year]);
          this.form.controls.academicYearId.setValue(String(year.id));
          this.yearForm.reset({ code: '', name: '', startDate: '', endDate: '' });
          this.yearSubmitting.set(false);
        },
        error: (err) => {
          this.yearSubmitting.set(false);
          if (err?.status === 409) {
            this.toasts.danger('Academic year code already exists.');
          } else {
            this.toasts.danger(httpErrorMessage(err, 'Could not create academic year.'));
          }
        },
      });
  }

  submit(): void {
    if (this.form.invalid || this.submitting() || this.years().length === 0 || this.courses().length === 0) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const courseId = Number(v.courseId);
    const academicYearId = Number(v.academicYearId);
    const capacity = Number(v.capacity);
    if (!Number.isFinite(courseId) || courseId <= 0) {
      this.toasts.warning('Select a course.');
      return;
    }
    if (!Number.isFinite(academicYearId) || academicYearId <= 0) {
      this.toasts.warning('Select an academic year.');
      return;
    }
    if (!Number.isFinite(capacity) || capacity < 1) {
      this.toasts.warning('Capacity must be at least 1.');
      return;
    }

    this.submitting.set(true);
    this.academicApi
      .createBatch({
        courseId,
        academicYearId,
        code: v.code.trim() || undefined,
        name: v.name.trim(),
        capacity,
        startDate: v.startDate || undefined,
        endDate: v.endDate || undefined,
      })
      .subscribe({
        next: () => {
          this.submitting.set(false);
          void this.router.navigateByUrl('/batches');
        },
        error: (err) => {
          this.submitting.set(false);
          if (err?.status === 409) {
            this.toasts.danger('Batch code already exists for this course/year.');
          } else {
            this.toasts.danger(httpErrorMessage(err, 'Could not create batch.'));
          }
        },
      });
  }
}
