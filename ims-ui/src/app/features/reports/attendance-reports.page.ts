import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { UiCard } from '../../shared/ui-card';
import { UiButton } from '../../shared/ui-button';
import { EmptyState } from '../../shared/empty-state';
import { LoadingState } from '../../shared/loading-state';
import { ToastService } from '../../shared/toast.service';
import { httpLoadError } from '../../core/http-error';
import { AuthService } from '../../core/auth.service';
import { AcademicService, Batch } from '../academic/academic.service';
import { AttendanceService, BatchAttendanceSummary } from '../attendance/attendance.service';
import { LowAttendanceReport, ReportService } from './report.service';
import { csvCell, downloadCsv } from './report-csv';

@Component({
  selector: 'app-attendance-reports-page',
  imports: [ReactiveFormsModule, UiCard, UiButton, EmptyState, LoadingState],
  templateUrl: './attendance-reports.page.html',
  styleUrl: './reports-shared.scss',
})
export class AttendanceReportsPage implements OnInit {
  private readonly reports = inject(ReportService);
  private readonly academic = inject(AcademicService);
  private readonly attendanceApi = inject(AttendanceService);
  private readonly auth = inject(AuthService);
  private readonly toasts = inject(ToastService);
  private readonly fb = inject(FormBuilder);

  readonly batches = signal<Batch[]>([]);
  readonly attendanceSummary = signal<BatchAttendanceSummary | null>(null);
  readonly lowAttendance = signal<LowAttendanceReport | null>(null);
  readonly loading = signal(false);

  readonly form = this.fb.nonNullable.group({
    batchId: [''],
    thresholdPercent: [''],
  });

  get canRun(): boolean {
    return (
      this.auth.hasPermission('attendance:mark') || this.auth.hasPermission('attendance:unlock')
    );
  }

  ngOnInit(): void {
    this.academic.listBatches().subscribe({
      next: (rows) => this.batches.set(rows),
      error: () => this.batches.set([]),
    });
  }

  run(): void {
    if (!this.canRun || this.loading()) return;
    const v = this.form.getRawValue();
    if (!v.batchId) {
      this.toasts.warning('Select a batch.');
      return;
    }
    const batchId = Number(v.batchId);
    const threshold = v.thresholdPercent ? Number(v.thresholdPercent) : undefined;
    this.loading.set(true);
    forkJoin({
      summary: this.attendanceApi.batchSummary(batchId),
      low: this.reports.lowAttendance(batchId, threshold),
    }).subscribe({
      next: (data) => {
        this.attendanceSummary.set(data.summary);
        this.lowAttendance.set(data.low);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.toasts.danger(httpLoadError(err, 'attendance reports'));
      },
    });
  }

  exportAttendanceCsv(): void {
    const summary = this.attendanceSummary();
    if (!summary) return;
    const header =
      'studentId,presentEffective,absent,excused,sessionsCounted,percentage,minAttendancePercent,belowThreshold';
    const lines = summary.students.map(
      (r) =>
        `${r.studentId},${r.presentEffective},${r.absent},${r.excused},${r.sessionsCounted},${r.percentage},${r.minAttendancePercent},${r.belowThreshold}`,
    );
    downloadCsv('rp-at-02-batch-attendance.csv', [header, ...lines].join('\n'));
  }

  exportLowAttendanceCsv(): void {
    const report = this.lowAttendance();
    if (!report) return;
    const header =
      'studentCode,studentName,presentEffective,absent,sessionsCounted,percentage,threshold';
    const lines = report.students.map(
      (r) =>
        `${csvCell(r.studentCode)},${csvCell(r.studentName)},${r.presentEffective},${r.absent},${r.sessionsCounted},${r.percentage},${r.minAttendancePercent}`,
    );
    downloadCsv('rp-at-03-low-attendance.csv', [header, ...lines].join('\n'));
  }
}
