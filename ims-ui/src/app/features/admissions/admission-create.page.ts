import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { UiCard } from '../../shared/ui-card';
import { UiButton } from '../../shared/ui-button';
import { UiInput } from '../../shared/ui-input';
import { LoadingState } from '../../shared/loading-state';
import { ErrorState } from '../../shared/error-state';
import { httpErrorMessage, httpLoadError } from '../../core/http-error';
import { CodeService } from '../../shared/code.service';
import { AcademicService, Course } from '../academic/academic.service';
import { AdmissionService } from './admission.service';

@Component({
  selector: 'app-admission-create-page',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    UiCard,
    UiButton,
    UiInput,
    LoadingState,
    ErrorState,
  ],
  templateUrl: './admission-create.page.html',
  styleUrl: './admission-create.page.scss',
})
export class AdmissionCreatePage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly admissionsApi = inject(AdmissionService);
  private readonly academicApi = inject(AcademicService);
  private readonly codesApi = inject(CodeService);
  private readonly router = inject(Router);

  readonly loading = signal(true);
  readonly loadError = signal<string | null>(null);
  readonly submitting = signal(false);
  readonly generating = signal(false);
  readonly error = signal<string | null>(null);
  readonly courses = signal<Course[]>([]);

  readonly form = this.fb.nonNullable.group({
    applicationNo: [''],
    courseId: ['', Validators.required],
    applicantName: ['', Validators.required],
    phone: [''],
    email: [''],
  });

  ngOnInit(): void {
    this.academicApi.listCourses('', 0, 100).subscribe({
      next: (page) => {
        this.courses.set(page.content);
        if (page.content.length > 0) {
          this.form.controls.courseId.setValue(String(page.content[0].id));
        }
        this.loading.set(false);
      },
      error: (err) => {
        this.loadError.set(httpLoadError(err, 'courses'));
        this.loading.set(false);
      },
    });
  }

  generateApplicationNo(): void {
    if (this.generating()) {
      return;
    }
    this.generating.set(true);
    this.error.set(null);
    this.codesApi.next('ADMISSION').subscribe({
      next: (res) => {
        this.form.controls.applicationNo.setValue(res.code);
        this.generating.set(false);
      },
      error: (err) => {
        this.generating.set(false);
        this.error.set(httpErrorMessage(err, 'Could not generate application number.'));
      },
    });
  }

  submit(): void {
    if (this.form.invalid || this.submitting() || this.courses().length === 0) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const courseId = Number(v.courseId);
    if (!Number.isFinite(courseId) || courseId <= 0) {
      this.error.set('Select a course.');
      return;
    }

    this.submitting.set(true);
    this.error.set(null);
    this.admissionsApi
      .create({
        applicationNo: v.applicationNo.trim() || undefined,
        courseId,
        applicantName: v.applicantName.trim(),
        phone: v.phone.trim() || undefined,
        email: v.email.trim() || undefined,
      })
      .subscribe({
        next: () => {
          this.submitting.set(false);
          void this.router.navigateByUrl('/admissions');
        },
        error: (err) => {
          this.submitting.set(false);
          this.error.set(httpErrorMessage(err, 'Could not create admission.'));
        },
      });
  }
}
