import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { UiCard } from '../../shared/ui-card';
import { UiButton } from '../../shared/ui-button';
import { EmptyState } from '../../shared/empty-state';
import { LoadingState } from '../../shared/loading-state';
import { ToastService } from '../../shared/toast.service';
import { httpErrorMessage } from '../../core/http-error';
import { AuthService } from '../../core/auth.service';
import { AcademicService, Batch, Course } from '../academic/academic.service';
import {
  ActiveCountRow,
  BatchRosterRow,
  ReportService,
  StudentListRow,
  WithdrawalReportRow,
} from './report.service';
import { csvCell, downloadCsv } from './report-csv';

@Component({
  selector: 'app-students-reports-page',
  imports: [ReactiveFormsModule, UiCard, UiButton, EmptyState, LoadingState],
  templateUrl: './students-reports.page.html',
  styleUrl: './reports-shared.scss',
})
export class StudentsReportsPage implements OnInit {
  private readonly reports = inject(ReportService);
  private readonly academic = inject(AcademicService);
  private readonly auth = inject(AuthService);
  private readonly toasts = inject(ToastService);
  private readonly fb = inject(FormBuilder);

  readonly batches = signal<Batch[]>([]);
  readonly courses = signal<Course[]>([]);
  readonly students = signal<StudentListRow[]>([]);
  readonly activeCounts = signal<ActiveCountRow[]>([]);
  readonly withdrawals = signal<WithdrawalReportRow[]>([]);
  readonly roster = signal<BatchRosterRow[]>([]);
  readonly loadingStudents = signal(false);
  readonly loadingCounts = signal(false);
  readonly loadingWithdrawals = signal(false);
  readonly loadingRoster = signal(false);

  readonly studentForm = this.fb.nonNullable.group({
    status: ['ACTIVE'],
    courseId: [''],
    batchId: [''],
    q: [''],
  });

  readonly countForm = this.fb.nonNullable.group({
    courseId: [''],
  });

  readonly withdrawalForm = this.fb.nonNullable.group({
    from: [isoDateOffset(-30), Validators.required],
    to: [isoDateOffset(0), Validators.required],
    courseId: [''],
    batchId: [''],
  });

  readonly rosterForm = this.fb.nonNullable.group({
    batchId: ['', Validators.required],
    status: ['ACTIVE'],
  });

  get canRun(): boolean {
    return this.auth.hasPermission('student:read');
  }

  ngOnInit(): void {
    this.academic.listBatches().subscribe({
      next: (rows) => {
        this.batches.set(rows);
        if (rows.length > 0 && !this.rosterForm.controls.batchId.value) {
          this.rosterForm.controls.batchId.setValue(String(rows[0].id));
        }
      },
      error: () => this.batches.set([]),
    });
    this.academic.listCourses('', 0, 100).subscribe({
      next: (page) => this.courses.set(page.content ?? []),
      error: () => this.courses.set([]),
    });
  }

  runStudentList(): void {
    if (!this.canRun || this.loadingStudents()) return;
    const v = this.studentForm.getRawValue();
    this.loadingStudents.set(true);
    this.reports
      .studentList({
        status: v.status || undefined,
        courseId: v.courseId ? Number(v.courseId) : undefined,
        batchId: v.batchId ? Number(v.batchId) : undefined,
        q: v.q || undefined,
      })
      .subscribe({
        next: (rows) => {
          this.students.set(rows);
          this.loadingStudents.set(false);
        },
        error: (err) => {
          this.loadingStudents.set(false);
          this.toasts.danger(httpErrorMessage(err, 'Could not load student list.'));
        },
      });
  }

  runActiveCounts(): void {
    if (!this.canRun || this.loadingCounts()) return;
    const courseId = this.countForm.getRawValue().courseId;
    this.loadingCounts.set(true);
    this.reports.activeCounts(courseId ? Number(courseId) : undefined).subscribe({
      next: (rows) => {
        this.activeCounts.set(rows);
        this.loadingCounts.set(false);
      },
      error: (err) => {
        this.loadingCounts.set(false);
        this.toasts.danger(httpErrorMessage(err, 'Could not load active counts.'));
      },
    });
  }

  runWithdrawals(): void {
    if (!this.canRun || this.loadingWithdrawals()) return;
    if (this.withdrawalForm.invalid) {
      this.withdrawalForm.markAllAsTouched();
      this.toasts.warning('From and to dates are required.');
      return;
    }
    const v = this.withdrawalForm.getRawValue();
    this.loadingWithdrawals.set(true);
    this.reports
      .withdrawals({
        from: v.from,
        to: v.to,
        courseId: v.courseId ? Number(v.courseId) : undefined,
        batchId: v.batchId ? Number(v.batchId) : undefined,
      })
      .subscribe({
        next: (rows) => {
          this.withdrawals.set(rows);
          this.loadingWithdrawals.set(false);
        },
        error: (err) => {
          this.loadingWithdrawals.set(false);
          this.toasts.danger(httpErrorMessage(err, 'Could not load withdrawals.'));
        },
      });
  }

  runRoster(): void {
    if (!this.canRun || this.loadingRoster()) return;
    if (this.rosterForm.invalid) {
      this.rosterForm.markAllAsTouched();
      this.toasts.warning('Select a batch.');
      return;
    }
    const v = this.rosterForm.getRawValue();
    this.loadingRoster.set(true);
    this.reports.roster(Number(v.batchId), v.status || undefined).subscribe({
      next: (rows) => {
        this.roster.set(rows);
        this.loadingRoster.set(false);
      },
      error: (err) => {
        this.loadingRoster.set(false);
        this.toasts.danger(httpErrorMessage(err, 'Could not load batch roster.'));
      },
    });
  }

  exportStudentsCsv(): void {
    const header =
      'studentCode,fullName,status,phone,email,batchCode,courseCode,enrollmentStatus';
    const lines = this.students().map(
      (r) =>
        `${csvCell(r.studentCode)},${csvCell(r.fullName)},${r.status},${csvCell(r.phone)},${csvCell(r.email)},${csvCell(r.batchCode)},${csvCell(r.courseCode)},${csvCell(r.enrollmentStatus)}`,
    );
    downloadCsv('rp-st-01-students.csv', [header, ...lines].join('\n'));
  }

  exportActiveCountsCsv(): void {
    const header = 'batchCode,batchName,courseCode,courseName,activeCount';
    const lines = this.activeCounts().map(
      (r) =>
        `${csvCell(r.batchCode)},${csvCell(r.batchName)},${csvCell(r.courseCode)},${csvCell(r.courseName)},${r.activeCount}`,
    );
    downloadCsv('rp-st-02-active-counts.csv', [header, ...lines].join('\n'));
  }

  exportWithdrawalsCsv(): void {
    const header =
      'studentCode,studentName,batchCode,courseCode,enrollmentStatus,withdrawnAt,activatedAt';
    const lines = this.withdrawals().map(
      (r) =>
        `${csvCell(r.studentCode)},${csvCell(r.studentName)},${csvCell(r.batchCode)},${csvCell(r.courseCode)},${r.enrollmentStatus},${csvCell(r.withdrawnAt)},${csvCell(r.activatedAt)}`,
    );
    downloadCsv('rp-st-04-withdrawals.csv', [header, ...lines].join('\n'));
  }

  exportRosterCsv(): void {
    const header =
      'studentCode,studentName,phone,email,batchCode,batchName,courseCode,enrollmentStatus,activatedAt';
    const lines = this.roster().map(
      (r) =>
        `${csvCell(r.studentCode)},${csvCell(r.studentName)},${csvCell(r.phone)},${csvCell(r.email)},${csvCell(r.batchCode)},${csvCell(r.batchName)},${csvCell(r.courseCode)},${r.enrollmentStatus},${csvCell(r.activatedAt)}`,
    );
    downloadCsv('rp-st-05-batch-roster.csv', [header, ...lines].join('\n'));
  }
}

function isoDateOffset(days: number): string {
  const d = new Date();
  d.setDate(d.getDate() + days);
  return d.toISOString().slice(0, 10);
}
