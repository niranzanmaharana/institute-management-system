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
import { CodeService } from '../../shared/code.service';
import { AcademicService, Course } from '../academic/academic.service';
import { Admission, AdmissionService } from './admission.service';

type Panel = 'approve' | 'reject' | 'cancel' | null;

@Component({
  selector: 'app-admission-detail-page',
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
  templateUrl: './admission-detail.page.html',
  styleUrl: './admission-detail.page.scss',
})
export class AdmissionDetailPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly admissionsApi = inject(AdmissionService);
  private readonly academicApi = inject(AcademicService);
  private readonly codesApi = inject(CodeService);
  private readonly fb = inject(FormBuilder);
  readonly auth = inject(AuthService);

  readonly loading = signal(true);
  readonly acting = signal(false);
  readonly generatingStudent = signal(false);
  readonly error = signal<string | null>(null);
  readonly admission = signal<Admission | null>(null);
  readonly course = signal<Course | null>(null);
  readonly panel = signal<Panel>(null);

  private admissionId = 0;

  readonly approveForm = this.fb.nonNullable.group({
    studentCode: [''],
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
    phone: [''],
    email: [''],
    reason: [''],
  });

  readonly reasonForm = this.fb.nonNullable.group({
    reason: ['', [Validators.required, Validators.minLength(5)]],
  });

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isFinite(id) || id <= 0) {
      this.error.set('Invalid admission id.');
      this.loading.set(false);
      return;
    }
    this.admissionId = id;
    this.load(id);
  }

  load(id: number): void {
    this.loading.set(true);
    this.error.set(null);
    this.admissionsApi.get(id).subscribe({
      next: (row) => {
        this.admission.set(row);
        this.loading.set(false);
        this.academicApi.getCourse(row.courseId).subscribe({
          next: (course) => this.course.set(course),
          error: () => this.course.set(null),
        });
      },
      error: (err) => {
        this.error.set(httpLoadError(err, 'admission'));
        this.loading.set(false);
      },
    });
  }

  tone(status: string): BadgeTone {
    if (status === 'APPROVED') return 'success';
    if (status === 'REJECTED' || status === 'CANCELLED') return 'danger';
    if (status === 'SUBMITTED') return 'info';
    return 'neutral';
  }

  courseLabel(): string {
    const course = this.course();
    const row = this.admission();
    if (course) {
      return `${course.code} — ${course.name}`;
    }
    return row ? `#${row.courseId}` : '—';
  }

  openPanel(next: Panel): void {
    const row = this.admission();
    this.panel.set(next);
    this.error.set(null);
    this.reasonForm.reset({ reason: '' });
    if (next === 'approve' && row) {
      const parts = row.applicantName.trim().split(/\s+/);
      this.approveForm.reset({
        studentCode: '',
        firstName: parts[0] ?? '',
        lastName: parts.length > 1 ? parts.slice(1).join(' ') : '',
        phone: row.phone ?? '',
        email: row.email ?? '',
        reason: '',
      });
    }
  }

  closePanel(): void {
    this.panel.set(null);
  }

  generateStudentCode(): void {
    if (this.generatingStudent()) {
      return;
    }
    this.generatingStudent.set(true);
    this.error.set(null);
    this.codesApi.next('STUDENT').subscribe({
      next: (res) => {
        this.approveForm.controls.studentCode.setValue(res.code);
        this.generatingStudent.set(false);
      },
      error: (err) => {
        this.generatingStudent.set(false);
        this.error.set(httpErrorMessage(err, 'Could not generate student code.'));
      },
    });
  }

  approve(): void {
    const row = this.admission();
    if (!row || this.approveForm.invalid || this.acting()) {
      this.approveForm.markAllAsTouched();
      return;
    }
    this.acting.set(true);
    this.error.set(null);
    const v = this.approveForm.getRawValue();
    this.admissionsApi
      .approve(row.id, {
        createStudent: {
          studentCode: v.studentCode.trim() || undefined,
          firstName: v.firstName.trim(),
          lastName: v.lastName.trim(),
          phone: v.phone.trim() || undefined,
          email: v.email.trim() || undefined,
        },
        reason: v.reason.trim() || undefined,
      })
      .subscribe({
        next: (updated) => {
          this.admission.set(updated);
          this.acting.set(false);
          this.closePanel();
        },
        error: (err) => {
          this.acting.set(false);
          this.error.set(httpErrorMessage(err, 'Could not approve admission.'));
        },
      });
  }

  reject(): void {
    this.runReasonAction('reject');
  }

  cancel(): void {
    this.runReasonAction('cancel');
  }

  goEnroll(): void {
    void this.router.navigateByUrl('/enrollments/new');
  }

  private runReasonAction(kind: 'reject' | 'cancel'): void {
    const row = this.admission();
    if (!row || this.reasonForm.invalid || this.acting()) {
      this.reasonForm.markAllAsTouched();
      return;
    }
    this.acting.set(true);
    this.error.set(null);
    const reason = this.reasonForm.getRawValue().reason.trim();
    const req =
      kind === 'reject'
        ? this.admissionsApi.reject(row.id, { reason })
        : this.admissionsApi.cancel(row.id, { reason });
    req.subscribe({
      next: (updated) => {
        this.admission.set(updated);
        this.acting.set(false);
        this.closePanel();
      },
      error: (err) => {
        this.acting.set(false);
        this.error.set(httpErrorMessage(err, `Could not ${kind} admission.`));
      },
    });
  }
}
