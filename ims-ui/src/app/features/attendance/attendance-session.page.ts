import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { UiCard } from '../../shared/ui-card';
import { UiButton } from '../../shared/ui-button';
import { EmptyState } from '../../shared/empty-state';
import { LoadingState } from '../../shared/loading-state';
import { ErrorState } from '../../shared/error-state';
import { BadgeTone, StatusBadge } from '../../shared/status-badge';
import { ToastService } from '../../shared/toast.service';
import { httpErrorMessage, httpLoadError } from '../../core/http-error';
import { AuthService } from '../../core/auth.service';
import { AcademicService, Batch } from '../academic/academic.service';
import {
  AttendanceRecord,
  AttendanceService,
  AttendanceSession,
  BatchAttendanceSummary,
} from './attendance.service';

@Component({
  selector: 'app-attendance-session-page',
  imports: [
    RouterLink,
    FormsModule,
    UiCard,
    UiButton,
    EmptyState,
    LoadingState,
    ErrorState,
    StatusBadge,
  ],
  templateUrl: './attendance-session.page.html',
  styleUrl: './attendance-session.page.scss',
})
export class AttendanceSessionPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly attendanceApi = inject(AttendanceService);
  private readonly academicApi = inject(AcademicService);
  private readonly auth = inject(AuthService);
  private readonly toasts = inject(ToastService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly session = signal<AttendanceSession | null>(null);
  readonly summary = signal<BatchAttendanceSummary | null>(null);
  readonly batches = signal<Batch[]>([]);
  readonly busy = signal(false);
  readonly draftMarks = signal<Record<number, string>>({});

  get canMark(): boolean {
    return this.auth.hasPermission('attendance:mark');
  }

  get canUnlock(): boolean {
    return this.auth.hasPermission('attendance:unlock');
  }

  get editable(): boolean {
    const s = this.session();
    return !!s && s.status !== 'LOCKED' && this.canMark;
  }

  ngOnInit(): void {
    this.academicApi.listBatches().subscribe({
      next: (rows) => this.batches.set(rows),
      error: () => this.batches.set([]),
    });
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isFinite(id) || id <= 0) {
      this.error.set('Invalid session id.');
      this.loading.set(false);
      return;
    }
    this.load(id);
  }

  batchLabel(batchId: number): string {
    const batch = this.batches().find((b) => b.id === batchId);
    return batch ? `${batch.code} — ${batch.name}` : `#${batchId}`;
  }

  sessionTone(status: string): BadgeTone {
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

  load(id: number): void {
    this.loading.set(true);
    this.error.set(null);
    this.attendanceApi.getSession(id).subscribe({
      next: (session) => {
        this.session.set(session);
        const marks: Record<number, string> = {};
        for (const row of session.records) {
          marks[row.studentId] = row.status || 'PRESENT';
        }
        this.draftMarks.set(marks);
        this.loading.set(false);
        this.attendanceApi.batchSummary(session.batchId).subscribe({
          next: (sum) => this.summary.set(sum),
          error: () => this.summary.set(null),
        });
      },
      error: (err) => {
        this.error.set(httpLoadError(err, 'attendance session'));
        this.loading.set(false);
      },
    });
  }

  setMark(studentId: number, status: string): void {
    this.draftMarks.update((m) => ({ ...m, [studentId]: status }));
  }

  saveMarks(): void {
    const session = this.session();
    if (!session || !this.editable || this.busy()) {
      return;
    }
    const records = session.records
      .filter((r) => r.eligible)
      .map((r) => ({
        studentId: r.studentId,
        status: this.draftMarks()[r.studentId] || 'PRESENT',
      }));
    this.busy.set(true);
    this.attendanceApi.markRecords(session.id, records).subscribe({
      next: (updated) => {
        this.busy.set(false);
        this.session.set(updated);
      },
      error: (err) => {
        this.busy.set(false);
        this.toasts.danger(httpErrorMessage(err, 'Could not save marks.'));
      },
    });
  }

  submit(): void {
    this.runLifecycle((id) => this.attendanceApi.submit(id));
  }

  lock(): void {
    this.runLifecycle((id) => this.attendanceApi.lock(id));
  }

  unlock(): void {
    const reason = window.prompt('Unlock reason (min 5 characters)', 'Correction needed');
    if (!reason || reason.trim().length < 5) {
      return;
    }
    this.runLifecycle((id) => this.attendanceApi.unlock(id, reason.trim()));
  }

  private runLifecycle(
    call: (id: number) => ReturnType<AttendanceService['submit']>,
  ): void {
    const session = this.session();
    if (!session || this.busy()) {
      return;
    }
    this.busy.set(true);
    call(session.id).subscribe({
      next: (updated) => {
        this.busy.set(false);
        this.session.set(updated);
        this.attendanceApi.batchSummary(updated.batchId).subscribe({
          next: (sum) => this.summary.set(sum),
          error: () => undefined,
        });
      },
      error: (err) => {
        this.busy.set(false);
        this.toasts.danger(httpErrorMessage(err, 'Action failed.'));
      },
    });
  }

  tone(status: string | null | undefined): BadgeTone {
    if (status === 'PRESENT' || status === 'LATE') {
      return 'success';
    }
    if (status === 'ABSENT') {
      return 'warning';
    }
    if (status === 'EXCUSED') {
      return 'info';
    }
    return 'neutral';
  }

  trackRecord(_index: number, row: AttendanceRecord): number {
    return row.studentId;
  }
}
