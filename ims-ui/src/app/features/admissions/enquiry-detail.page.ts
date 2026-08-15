import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { UiCard } from '../../shared/ui-card';
import { UiButton } from '../../shared/ui-button';
import { UiInput } from '../../shared/ui-input';
import { LoadingState } from '../../shared/loading-state';
import { ErrorState } from '../../shared/error-state';
import { StatusBadge, BadgeTone } from '../../shared/status-badge';
import { AuthService } from '../../core/auth.service';
import { httpErrorMessage, httpLoadError } from '../../core/http-error';
import { AcademicService, Course } from '../academic/academic.service';
import { Enquiry, EnquiryService } from './enquiry.service';

@Component({
  selector: 'app-enquiry-detail-page',
  imports: [
    RouterLink,
    ReactiveFormsModule,
    UiCard,
    UiButton,
    UiInput,
    LoadingState,
    ErrorState,
    StatusBadge,
  ],
  templateUrl: './enquiry-detail.page.html',
  styleUrl: './enquiry-detail.page.scss',
})
export class EnquiryDetailPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly enquiriesApi = inject(EnquiryService);
  private readonly academicApi = inject(AcademicService);
  private readonly fb = inject(FormBuilder);
  readonly auth = inject(AuthService);

  readonly loading = signal(true);
  readonly acting = signal(false);
  readonly error = signal<string | null>(null);
  readonly enquiry = signal<Enquiry | null>(null);
  readonly courses = signal<Course[]>([]);
  readonly panel = signal<'convert' | 'close' | null>(null);

  readonly convertForm = this.fb.nonNullable.group({
    courseId: ['', Validators.required],
  });

  readonly closeForm = this.fb.nonNullable.group({
    notes: [''],
  });

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isFinite(id) || id <= 0) {
      this.error.set('Invalid enquiry id.');
      this.loading.set(false);
      return;
    }
    this.academicApi.listCourses('', 0, 100).subscribe({
      next: (page) => this.courses.set(page.content),
      error: () => this.courses.set([]),
    });
    this.load(id);
  }

  load(id: number): void {
    this.loading.set(true);
    this.error.set(null);
    this.enquiriesApi.get(id).subscribe({
      next: (row) => {
        this.enquiry.set(row);
        if (row.interestedCourseId) {
          this.convertForm.controls.courseId.setValue(String(row.interestedCourseId));
        }
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(httpLoadError(err, 'enquiry'));
        this.loading.set(false);
      },
    });
  }

  tone(status: string): BadgeTone {
    if (status === 'CONVERTED') return 'success';
    if (status === 'CLOSED') return 'neutral';
    return 'info';
  }

  courseLabel(courseId?: number): string {
    if (!courseId) {
      return '—';
    }
    const course = this.courses().find((c) => c.id === courseId);
    return course ? `${course.code} — ${course.name}` : `#${courseId}`;
  }

  openConvert(): void {
    this.panel.set('convert');
    this.error.set(null);
  }

  openClose(): void {
    this.panel.set('close');
    this.error.set(null);
    this.closeForm.reset({ notes: '' });
  }

  closePanel(): void {
    this.panel.set(null);
  }

  convert(): void {
    const row = this.enquiry();
    if (!row || this.convertForm.invalid || this.acting()) {
      this.convertForm.markAllAsTouched();
      return;
    }
    const courseId = Number(this.convertForm.getRawValue().courseId);
    if (!courseId) {
      this.error.set('Select a course to convert.');
      return;
    }
    this.acting.set(true);
    this.error.set(null);
    this.enquiriesApi.convert(row.id, { courseId }).subscribe({
      next: (application) => {
        this.acting.set(false);
        void this.router.navigate(['/admissions', application.id]);
      },
      error: (err) => {
        this.acting.set(false);
        this.error.set(httpErrorMessage(err, 'Could not convert enquiry.'));
      },
    });
  }

  closeEnquiry(): void {
    const row = this.enquiry();
    if (!row || this.acting()) {
      return;
    }
    this.acting.set(true);
    this.error.set(null);
    this.enquiriesApi.close(row.id, this.closeForm.getRawValue().notes.trim() || undefined).subscribe({
      next: (updated) => {
        this.enquiry.set(updated);
        this.acting.set(false);
        this.closePanel();
      },
      error: (err) => {
        this.acting.set(false);
        this.error.set(httpErrorMessage(err, 'Could not close enquiry.'));
      },
    });
  }
}
