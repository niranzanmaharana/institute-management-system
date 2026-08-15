import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { UiCard } from '../../shared/ui-card';
import { UiButton } from '../../shared/ui-button';
import { UiInput } from '../../shared/ui-input';
import { LoadingState } from '../../shared/loading-state';
import { ErrorState } from '../../shared/error-state';
import { StatusBadge, BadgeTone } from '../../shared/status-badge';
import { AuthService } from '../../core/auth.service';
import { httpErrorMessage, httpLoadError } from '../../core/http-error';
import { AcademicService, Batch } from '../academic/academic.service';
import {
  ActivationResult,
  ActivateEnrollmentRequest,
  Enrollment,
  EnrollmentService,
} from './enrollment.service';

type ActionPanel =
  | 'activate'
  | 'cancel'
  | 'suspend'
  | 'resume'
  | 'withdraw'
  | 'complete'
  | 'transfer'
  | null;

@Component({
  selector: 'app-enrollment-detail-page',
  imports: [
    RouterLink,
    FormsModule,
    UiCard,
    UiButton,
    UiInput,
    LoadingState,
    ErrorState,
    StatusBadge,
  ],
  templateUrl: './enrollment-detail.page.html',
  styleUrl: './enrollment-detail.page.scss',
})
export class EnrollmentDetailPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly enrollmentsApi = inject(EnrollmentService);
  private readonly academicApi = inject(AcademicService);
  readonly auth = inject(AuthService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly enrollment = signal<Enrollment | null>(null);
  readonly acting = signal(false);
  readonly activation = signal<ActivationResult | null>(null);
  readonly panel = signal<ActionPanel>(null);
  readonly transferBatches = signal<Batch[]>([]);

  reason = '';
  overrideReason = '';
  waiverReason = '';
  capacityOverride = false;
  admissionWaiver = false;
  targetBatchId = '';

  private enrollmentId = 0;

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isFinite(id) || id <= 0) {
      this.error.set('Invalid enrollment id.');
      this.loading.set(false);
      return;
    }
    this.enrollmentId = id;
    this.load(id);
  }

  load(id: number): void {
    this.loading.set(true);
    this.error.set(null);
    this.enrollmentsApi.get(id).subscribe({
      next: (row) => {
        this.enrollment.set(row);
        this.loading.set(false);
        this.loadTransferBatches(row.courseId, row.batchId);
      },
      error: (err) => {
        this.error.set(httpLoadError(err, 'enrollment'));
        this.loading.set(false);
      },
    });
  }

  private loadTransferBatches(courseId: number, currentBatchId: number): void {
    this.academicApi.listBatches(courseId).subscribe({
      next: (batches) => {
        this.transferBatches.set(batches.filter((b) => b.id !== currentBatchId && b.status === 'OPEN'));
      },
      error: () => this.transferBatches.set([]),
    });
  }

  tone(status: string): BadgeTone {
    if (status === 'ACTIVE') return 'success';
    if (status === 'APPLIED') return 'info';
    if (status === 'SUSPENDED') return 'warning';
    if (status === 'WITHDRAWN' || status === 'CANCELLED') return 'danger';
    if (status === 'COMPLETED') return 'neutral';
    return 'neutral';
  }

  openPanel(next: ActionPanel): void {
    this.panel.set(next);
    this.reason = '';
    this.overrideReason = '';
    this.waiverReason = '';
    this.capacityOverride = false;
    this.admissionWaiver = false;
    this.targetBatchId = '';
    this.error.set(null);
  }

  closePanel(): void {
    this.panel.set(null);
  }

  private activateBody(): ActivateEnrollmentRequest {
    return {
      capacityOverride: this.capacityOverride,
      overrideReason: this.capacityOverride ? this.overrideReason.trim() : undefined,
      admissionWaiver: this.admissionWaiver,
      waiverReason: this.admissionWaiver ? this.waiverReason.trim() : undefined,
    };
  }

  runActivate(): void {
    const row = this.enrollment();
    if (!row || this.acting()) return;
    if (this.capacityOverride && !this.overrideReason.trim()) {
      this.error.set('Capacity override reason is required.');
      return;
    }
    if (this.admissionWaiver && !this.waiverReason.trim()) {
      this.error.set('Admission waiver reason is required.');
      return;
    }
    this.acting.set(true);
    this.error.set(null);
    this.enrollmentsApi.activate(row.id, this.activateBody(), crypto.randomUUID()).subscribe({
      next: (result) => {
        this.activation.set(result);
        this.enrollment.set({
          ...row,
          status: result.enrollmentStatus,
          activatedAt: result.activatedAt,
        });
        this.acting.set(false);
        this.closePanel();
      },
      error: (err) => this.fail(err, 'Could not activate enrollment.'),
    });
  }

  runReasonAction(kind: 'cancel' | 'suspend' | 'withdraw' | 'complete'): void {
    const row = this.enrollment();
    if (!row || this.acting()) return;
    const reason = this.reason.trim();
    if (!reason) {
      this.error.set('Reason is required.');
      return;
    }
    this.acting.set(true);
    this.error.set(null);
    const req =
      kind === 'cancel'
        ? this.enrollmentsApi.cancel(row.id, { reason })
        : kind === 'suspend'
          ? this.enrollmentsApi.suspend(row.id, { reason })
          : kind === 'withdraw'
            ? this.enrollmentsApi.withdraw(row.id, { reason })
            : this.enrollmentsApi.complete(row.id, { reason });
    req.subscribe({
      next: (updated) => {
        this.enrollment.set(updated);
        this.acting.set(false);
        this.closePanel();
        this.loadTransferBatches(updated.courseId, updated.batchId);
      },
      error: (err) => this.fail(err, `Could not ${kind} enrollment.`),
    });
  }

  confirmReasonPanel(): void {
    const p = this.panel();
    if (p === 'cancel' || p === 'suspend' || p === 'withdraw' || p === 'complete') {
      this.runReasonAction(p);
    }
  }

  runResume(): void {
    const row = this.enrollment();
    if (!row || this.acting()) return;
    if (this.capacityOverride && !this.overrideReason.trim()) {
      this.error.set('Capacity override reason is required.');
      return;
    }
    this.acting.set(true);
    this.error.set(null);
    this.enrollmentsApi.resume(row.id, this.activateBody()).subscribe({
      next: (updated) => {
        this.enrollment.set(updated);
        this.acting.set(false);
        this.closePanel();
      },
      error: (err) => this.fail(err, 'Could not resume enrollment.'),
    });
  }

  runTransfer(): void {
    const row = this.enrollment();
    if (!row || this.acting()) return;
    const targetBatchId = Number(this.targetBatchId);
    if (!targetBatchId) {
      this.error.set('Select a target batch.');
      return;
    }
    if (this.capacityOverride && !this.overrideReason.trim()) {
      this.error.set('Capacity override reason is required.');
      return;
    }
    this.acting.set(true);
    this.error.set(null);
    this.enrollmentsApi
      .transfer(row.id, {
        targetBatchId,
        capacityOverride: this.capacityOverride,
        overrideReason: this.capacityOverride ? this.overrideReason.trim() : undefined,
      })
      .subscribe({
        next: (updated) => {
          this.enrollment.set(updated);
          this.acting.set(false);
          this.closePanel();
          this.loadTransferBatches(updated.courseId, updated.batchId);
        },
        error: (err) => this.fail(err, 'Could not transfer enrollment.'),
      });
  }

  private fail(err: unknown, fallback: string): void {
    this.acting.set(false);
    this.error.set(httpErrorMessage(err, fallback));
  }
}
