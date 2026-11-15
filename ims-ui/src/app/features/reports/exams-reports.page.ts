import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { UiCard } from '../../shared/ui-card';
import { UiButton } from '../../shared/ui-button';
import { EmptyState } from '../../shared/empty-state';
import { LoadingState } from '../../shared/loading-state';
import { ToastService } from '../../shared/toast.service';
import { httpErrorMessage } from '../../core/http-error';
import { AuthService } from '../../core/auth.service';
import { AcademicService, Batch } from '../academic/academic.service';
import { Exam, ExamService } from '../exams/exam.service';
import { ExamResultReportRow, ReportService } from './report.service';
import { csvCell, downloadCsv } from './report-csv';

@Component({
  selector: 'app-exams-reports-page',
  imports: [ReactiveFormsModule, UiCard, UiButton, EmptyState, LoadingState],
  templateUrl: './exams-reports.page.html',
  styleUrl: './reports-shared.scss',
})
export class ExamsReportsPage implements OnInit {
  private readonly reports = inject(ReportService);
  private readonly academic = inject(AcademicService);
  private readonly examApi = inject(ExamService);
  private readonly auth = inject(AuthService);
  private readonly toasts = inject(ToastService);
  private readonly fb = inject(FormBuilder);

  readonly batches = signal<Batch[]>([]);
  readonly exams = signal<Exam[]>([]);
  readonly examResults = signal<ExamResultReportRow[]>([]);
  readonly loading = signal(false);

  readonly form = this.fb.nonNullable.group({
    batchId: [''],
    examId: [''],
  });

  get canRun(): boolean {
    return this.auth.hasPermission('exam:mark') || this.auth.hasPermission('exam:publish');
  }

  ngOnInit(): void {
    this.academic.listBatches().subscribe({
      next: (rows) => this.batches.set(rows),
      error: () => this.batches.set([]),
    });
  }

  onBatchChange(): void {
    const batchId = this.form.getRawValue().batchId;
    this.form.patchValue({ examId: '' });
    this.exams.set([]);
    if (!batchId) return;
    this.examApi.list(Number(batchId)).subscribe({
      next: (rows) => this.exams.set(rows),
      error: () => this.exams.set([]),
    });
  }

  run(): void {
    if (!this.canRun || this.loading()) return;
    const v = this.form.getRawValue();
    if (!v.batchId || !v.examId) {
      this.toasts.warning('Select batch and exam.');
      return;
    }
    this.loading.set(true);
    this.reports.examResults(Number(v.batchId), Number(v.examId)).subscribe({
      next: (rows) => {
        this.examResults.set(rows);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.toasts.danger(httpErrorMessage(err, 'Could not load exam results.'));
      },
    });
  }

  exportCsv(): void {
    const header =
      'examName,examStatus,examDate,studentCode,studentName,resultStatus,totalMarks,passFail,grade';
    const lines = this.examResults().map(
      (r) =>
        `${csvCell(r.examName)},${r.examStatus},${r.examDate},${csvCell(r.studentCode)},${csvCell(r.studentName)},${r.resultStatus},${r.totalMarks ?? ''},${csvCell(r.passFail)},${csvCell(r.grade)}`,
    );
    downloadCsv('rp-ex-01-exam-results.csv', [header, ...lines].join('\n'));
  }
}
