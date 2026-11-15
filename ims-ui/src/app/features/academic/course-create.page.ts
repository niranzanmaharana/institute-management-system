import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { UiCard } from '../../shared/ui-card';
import { UiButton } from '../../shared/ui-button';
import { UiInput } from '../../shared/ui-input';
import { ToastService } from '../../shared/toast.service';
import { httpErrorMessage } from '../../core/http-error';
import { CodeService } from '../../shared/code.service';
import { AcademicService } from './academic.service';

@Component({
  selector: 'app-course-create-page',
  imports: [ReactiveFormsModule, RouterLink, UiCard, UiButton, UiInput],
  templateUrl: './course-create.page.html',
  styleUrl: './course-create.page.scss',
})
export class CourseCreatePage {
  private readonly fb = inject(FormBuilder);
  private readonly academicApi = inject(AcademicService);
  private readonly codesApi = inject(CodeService);
  private readonly router = inject(Router);
  private readonly toasts = inject(ToastService);

  readonly submitting = signal(false);
  readonly generating = signal(false);

  readonly form = this.fb.nonNullable.group({
    code: [''],
    name: ['', Validators.required],
    description: [''],
  });

  generateCode(): void {
    if (this.generating()) {
      return;
    }
    this.generating.set(true);
    this.codesApi.next('COURSE').subscribe({
      next: (res) => {
        this.form.controls.code.setValue(res.code);
        this.generating.set(false);
      },
      error: (err) => {
        this.generating.set(false);
        this.toasts.danger(httpErrorMessage(err, 'Could not generate course code.'));
      },
    });
  }

  submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    const v = this.form.getRawValue();
    this.academicApi
      .createCourse({
        code: v.code.trim() || undefined,
        name: v.name.trim(),
        description: v.description.trim() || undefined,
      })
      .subscribe({
        next: (course) => {
          this.submitting.set(false);
          void this.router.navigate(['/courses', course.id]);
        },
        error: (err) => {
          this.submitting.set(false);
          const status = err?.status;
          if (status === 409) {
            this.toasts.danger('Course code already exists in this institute.');
          } else {
            this.toasts.danger(httpErrorMessage(err, 'Could not create course.'));
          }
        },
      });
  }
}
