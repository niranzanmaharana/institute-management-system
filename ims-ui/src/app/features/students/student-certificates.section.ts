import { DatePipe } from '@angular/common';
import { Component, Input, OnChanges, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/auth.service';
import { httpErrorMessage, httpLoadError } from '../../core/http-error';
import { EmptyState } from '../../shared/empty-state';
import { ErrorState } from '../../shared/error-state';
import { LoadingState } from '../../shared/loading-state';
import { StatusBadge } from '../../shared/status-badge';
import { ToastService } from '../../shared/toast.service';
import { UiButton } from '../../shared/ui-button';
import { Enrollment, EnrollmentService } from '../enrollments/enrollment.service';
import {
  CertificateService,
  CertificateTemplate,
  GeneratedCertificate,
} from './certificate.service';

@Component({
  selector: 'app-student-certificates',
  imports: [DatePipe, FormsModule, UiButton, LoadingState, ErrorState, EmptyState, StatusBadge],
  templateUrl: './student-certificates.section.html',
  styleUrl: './student-certificates.section.scss',
})
export class StudentCertificatesSection implements OnChanges {
  private readonly certApi = inject(CertificateService);
  private readonly enrollmentApi = inject(EnrollmentService);
  private readonly toasts = inject(ToastService);
  readonly auth = inject(AuthService);

  @Input({ required: true }) studentId!: number;

  readonly loading = signal(false);
  readonly generating = signal(false);
  readonly error = signal<string | null>(null);
  readonly rows = signal<GeneratedCertificate[]>([]);
  readonly templates = signal<CertificateTemplate[]>([]);
  readonly enrollments = signal<Enrollment[]>([]);

  templateCode = 'BONAFIDE';
  enrollmentId = '';

  get canGenerate(): boolean {
    return this.auth.hasPermission('certificate:generate');
  }

  get canRead(): boolean {
    return (
      this.auth.hasPermission('certificate:read') ||
      this.auth.hasPermission('certificate:generate') ||
      this.auth.hasRole('STUDENT')
    );
  }

  get needsEnrollment(): boolean {
    return this.templateCode === 'COMPLETION';
  }

  get enrollmentOptions(): Enrollment[] {
    const all = this.enrollments();
    if (this.templateCode === 'COMPLETION') {
      return all.filter((e) => e.status === 'COMPLETED');
    }
    return all.filter((e) => e.status !== 'CANCELLED');
  }

  ngOnChanges(): void {
    if (this.studentId > 0 && this.canRead) {
      this.reload();
    }
  }

  reload(): void {
    this.loading.set(true);
    this.error.set(null);
    this.certApi.listTemplates().subscribe({
      next: (templates) => this.templates.set(templates.filter((t) => t.active)),
      error: () => this.templates.set([]),
    });
    this.enrollmentApi.list().subscribe({
      next: (rows) =>
        this.enrollments.set(rows.filter((e) => e.studentId === this.studentId)),
      error: () => this.enrollments.set([]),
    });
    this.certApi.listForStudent(this.studentId).subscribe({
      next: (rows) => {
        this.rows.set(rows);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(httpLoadError(err, 'certificates'));
        this.loading.set(false);
      },
    });
  }

  generate(): void {
    if (!this.canGenerate || this.generating()) {
      return;
    }
    if (!this.templateCode) {
      this.toasts.warning('Select a certificate template.');
      return;
    }
    let enrollmentId: number | null = null;
    if (this.needsEnrollment || this.enrollmentId) {
      enrollmentId = Number(this.enrollmentId);
      if (!Number.isFinite(enrollmentId) || enrollmentId <= 0) {
        this.toasts.warning(
          this.needsEnrollment
            ? 'Select a completed enrollment for the completion certificate.'
            : 'Select a valid enrollment.',
        );
        return;
      }
    }
    this.generating.set(true);
    this.certApi
      .generate({
        studentId: this.studentId,
        templateCode: this.templateCode,
        enrollmentId,
      })
      .subscribe({
        next: () => {
          this.generating.set(false);
          this.reload();
        },
        error: (err) => {
          this.generating.set(false);
          this.toasts.danger(httpErrorMessage(err, 'Could not generate certificate.'));
        },
      });
  }

  download(row: GeneratedCertificate): void {
    this.certApi.download(row.id).subscribe({
      next: (res) => window.open(res.url, '_blank', 'noopener'),
      error: (err) =>
        this.toasts.danger(httpErrorMessage(err, 'Could not download certificate.')),
    });
  }
}
