import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { UiCard } from '../../shared/ui-card';
import { UiButton } from '../../shared/ui-button';
import { UiInput } from '../../shared/ui-input';
import { LoadingState } from '../../shared/loading-state';
import { ErrorState } from '../../shared/error-state';
import { httpErrorMessage, httpLoadError } from '../../core/http-error';
import { AcademicService, Course } from '../academic/academic.service';
import { EnquiryService } from './enquiry.service';

@Component({
  selector: 'app-enquiry-create-page',
  imports: [ReactiveFormsModule, RouterLink, UiCard, UiButton, UiInput, LoadingState, ErrorState],
  templateUrl: './enquiry-create.page.html',
  styleUrl: './enquiry-create.page.scss',
})
export class EnquiryCreatePage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly enquiriesApi = inject(EnquiryService);
  private readonly academicApi = inject(AcademicService);
  private readonly router = inject(Router);

  readonly loading = signal(true);
  readonly submitting = signal(false);
  readonly loadError = signal<string | null>(null);
  readonly error = signal<string | null>(null);
  readonly courses = signal<Course[]>([]);

  readonly form = this.fb.nonNullable.group({
    name: ['', Validators.required],
    phone: [''],
    email: [''],
    interestedCourseId: [''],
    notes: [''],
  });

  ngOnInit(): void {
    this.academicApi.listCourses('', 0, 100).subscribe({
      next: (page) => {
        this.courses.set(page.content);
        this.loading.set(false);
      },
      error: (err) => {
        this.loadError.set(httpLoadError(err, 'courses'));
        this.loading.set(false);
      },
    });
  }

  submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const courseId = Number(v.interestedCourseId);
    this.submitting.set(true);
    this.error.set(null);
    this.enquiriesApi
      .create({
        name: v.name.trim(),
        phone: v.phone.trim() || undefined,
        email: v.email.trim() || undefined,
        interestedCourseId: Number.isFinite(courseId) && courseId > 0 ? courseId : undefined,
        notes: v.notes.trim() || undefined,
      })
      .subscribe({
        next: (created) => {
          this.submitting.set(false);
          void this.router.navigate(['/enquiries', created.id]);
        },
        error: (err) => {
          this.submitting.set(false);
          this.error.set(httpErrorMessage(err, 'Could not create enquiry.'));
        },
      });
  }
}
