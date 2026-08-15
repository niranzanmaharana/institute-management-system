import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { UiCard } from '../../shared/ui-card';
import { UiButton } from '../../shared/ui-button';
import { UiInput } from '../../shared/ui-input';
import { ErrorState } from '../../shared/error-state';
import { LoadingState } from '../../shared/loading-state';
import { httpErrorMessage, httpLoadError } from '../../core/http-error';
import { AcademicService } from './academic.service';

@Component({
  selector: 'app-course-edit-page',
  imports: [ReactiveFormsModule, RouterLink, UiCard, UiButton, UiInput, ErrorState, LoadingState],
  templateUrl: './course-edit.page.html',
  styleUrl: './course-edit.page.scss',
})
export class CourseEditPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly academicApi = inject(AcademicService);

  readonly loading = signal(true);
  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);
  courseId = 0;
  code = '';

  readonly form = this.fb.nonNullable.group({
    name: ['', Validators.required],
    description: [''],
  });

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isFinite(id) || id <= 0) {
      this.error.set('Invalid course id.');
      this.loading.set(false);
      return;
    }
    this.courseId = id;
    this.academicApi.getCourse(id).subscribe({
      next: (row) => {
        this.code = row.code;
        this.form.patchValue({
          name: row.name,
          description: row.description || '',
        });
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(httpLoadError(err, 'course'));
        this.loading.set(false);
      },
    });
  }

  submit(): void {
    if (this.form.invalid || this.submitting() || !this.courseId) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.error.set(null);
    const v = this.form.getRawValue();
    this.academicApi
      .updateCourse(this.courseId, {
        name: v.name.trim(),
        description: v.description.trim() || undefined,
      })
      .subscribe({
        next: () => {
          this.submitting.set(false);
          void this.router.navigate(['/courses', this.courseId]);
        },
        error: (err) => {
          this.submitting.set(false);
          this.error.set(httpErrorMessage(err, 'Could not save course.'));
        },
      });
  }
}
