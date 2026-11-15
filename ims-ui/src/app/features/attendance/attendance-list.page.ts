import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { UiCard } from '../../shared/ui-card';
import { UiButton } from '../../shared/ui-button';
import { UiDatepicker } from '../../shared/ui-datepicker';
import { EmptyState } from '../../shared/empty-state';
import { LoadingState } from '../../shared/loading-state';
import { ErrorState } from '../../shared/error-state';
import { BadgeTone, StatusBadge } from '../../shared/status-badge';
import { ToastService } from '../../shared/toast.service';
import { httpErrorMessage, httpLoadError } from '../../core/http-error';
import { AuthService } from '../../core/auth.service';
import { AcademicService, Batch } from '../academic/academic.service';
import { AttendanceService, AttendanceSession } from './attendance.service';

@Component({
  selector: 'app-attendance-list-page',
  imports: [
    FormsModule,
    ReactiveFormsModule,
    RouterLink,
    UiCard,
    UiButton,
    UiDatepicker,
    EmptyState,
    LoadingState,
    ErrorState,
    StatusBadge,
  ],
  templateUrl: './attendance-list.page.html',
  styleUrl: './attendance-list.page.scss',
})
export class AttendanceListPage implements OnInit {
  private readonly attendanceApi = inject(AttendanceService);
  private readonly academicApi = inject(AcademicService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly toasts = inject(ToastService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly rows = signal<AttendanceSession[]>([]);
  readonly batches = signal<Batch[]>([]);
  readonly filterBatchId = signal('');
  readonly creating = signal(false);

  readonly createForm = this.fb.nonNullable.group({
    batchId: ['', Validators.required],
    sessionDate: [new Date().toISOString().slice(0, 10), Validators.required],
  });

  get canMark(): boolean {
    return this.auth.hasPermission('attendance:mark');
  }

  ngOnInit(): void {
    this.academicApi.listBatches().subscribe({
      next: (rows) => this.batches.set(rows),
      error: () => this.batches.set([]),
    });
    this.reload();
  }

  batchLabel(batchId: number): string {
    const batch = this.batches().find((b) => b.id === batchId);
    return batch ? `${batch.code} — ${batch.name}` : `#${batchId}`;
  }

  statusTone(status: string): BadgeTone {
    switch (status) {
      case 'LOCKED':
        return 'success';
      case 'SUBMITTED':
        return 'info';
      case 'DRAFT':
        return 'warning';
      default:
        return 'neutral';
    }
  }

  onFilterBatch(value: string): void {
    this.filterBatchId.set(value);
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.error.set(null);
    const batchId = Number(this.filterBatchId());
    this.attendanceApi
      .listSessions(Number.isFinite(batchId) && batchId > 0 ? batchId : undefined)
      .subscribe({
        next: (rows) => {
          this.rows.set(rows);
          this.loading.set(false);
        },
        error: (err) => {
          this.error.set(httpLoadError(err, 'attendance sessions'));
          this.loading.set(false);
        },
      });
  }

  create(): void {
    if (!this.canMark || this.createForm.invalid || this.creating()) {
      this.createForm.markAllAsTouched();
      return;
    }
    const v = this.createForm.getRawValue();
    this.creating.set(true);
    this.attendanceApi
      .createSession({ batchId: Number(v.batchId), sessionDate: v.sessionDate })
      .subscribe({
        next: (session) => {
          this.creating.set(false);
          void this.router.navigate(['/attendance', session.id]);
        },
        error: (err) => {
          this.creating.set(false);
          this.toasts.danger(httpErrorMessage(err, 'Could not create session.'));
        },
      });
  }
}
