import { Component, OnInit, inject, signal } from '@angular/core';
import { FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { UiCard } from '../../shared/ui-card';
import { UiButton } from '../../shared/ui-button';
import { UiInput } from '../../shared/ui-input';
import { EmptyState } from '../../shared/empty-state';
import { LoadingState } from '../../shared/loading-state';
import { ErrorState } from '../../shared/error-state';
import { BadgeTone, StatusBadge } from '../../shared/status-badge';
import { httpErrorMessage, httpLoadError } from '../../core/http-error';
import { AuthService } from '../../core/auth.service';
import { AcademicService, Batch } from '../academic/academic.service';
import { Exam, ExamResult, ExamService } from './exam.service';

@Component({
  selector: 'app-exam-detail-page',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    UiCard,
    UiButton,
    UiInput,
    EmptyState,
    LoadingState,
    ErrorState,
    StatusBadge,
  ],
  templateUrl: './exam-detail.page.html',
  styleUrl: './exam-detail.page.scss',
})
export class ExamDetailPage implements OnInit {
  private readonly examApi = inject(ExamService);
  private readonly academicApi = inject(AcademicService);
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly exam = signal<Exam | null>(null);
  readonly results = signal<ExamResult[]>([]);
  readonly grades = signal<string[]>([]);
  readonly batches = signal<Batch[]>([]);
  readonly actionError = signal<string | null>(null);
  readonly busy = signal(false);

  readonly componentForm = this.fb.nonNullable.group({
    code: ['', Validators.required],
    name: ['', Validators.required],
    maxMarks: [100, [Validators.required, Validators.min(0.01)]],
    passMarks: [40, [Validators.required, Validators.min(0)]],
  });

  readonly marksForm = this.fb.group({
    rows: this.fb.array([] as ReturnType<ExamDetailPage['buildResultGroup']>[]),
  });

  readonly unlockReason = this.fb.nonNullable.control('', Validators.required);

  get canMark(): boolean {
    return this.auth.hasPermission('exam:mark');
  }

  get canPublish(): boolean {
    return this.auth.hasPermission('exam:publish');
  }

  get rows(): FormArray {
    return this.marksForm.get('rows') as FormArray;
  }

  get status(): string {
    return this.exam()?.status ?? '';
  }

  get marksEditable(): boolean {
    return this.canMark && this.status === 'DRAFT_MARKS';
  }

  ngOnInit(): void {
    this.academicApi.listBatches().subscribe({
      next: (rows) => this.batches.set(rows),
      error: () => this.batches.set([]),
    });
    this.examApi.listGrades().subscribe({
      next: (grades) => this.grades.set(grades),
      error: () => this.grades.set(['O', 'A+', 'A', 'B+', 'B', 'C', 'D', 'F']),
    });
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isFinite(id) || id <= 0) {
      this.error.set('Invalid exam id.');
      this.loading.set(false);
      return;
    }
    this.reload(id);
  }

  batchLabel(batchId: number): string {
    const batch = this.batches().find((b) => b.id === batchId);
    return batch ? `${batch.code} — ${batch.name}` : `#${batchId}`;
  }

  statusTone(status: string): BadgeTone {
    switch (status) {
      case 'PUBLISHED':
      case 'LOCKED':
        return 'success';
      case 'DRAFT_MARKS':
        return 'info';
      case 'SCHEDULED':
        return 'warning';
      case 'CANCELLED':
        return 'danger';
      default:
        return 'neutral';
    }
  }

  reload(id = this.exam()?.id): void {
    if (id == null) {
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.actionError.set(null);
    this.examApi.get(id).subscribe({
      next: (exam) => {
        this.exam.set(exam);
        this.examApi.listResults(id).subscribe({
          next: (results) => {
            this.results.set(results);
            this.rebuildMarksForm(exam, results);
            this.loading.set(false);
          },
          error: (err) => {
            this.error.set(httpLoadError(err, 'results'));
            this.loading.set(false);
          },
        });
      },
      error: (err) => {
        this.error.set(httpLoadError(err, 'exam'));
        this.loading.set(false);
      },
    });
  }

  addComponent(): void {
    const exam = this.exam();
    if (!exam || !this.canMark || this.status !== 'SCHEDULED' || this.busy()) {
      return;
    }
    if (this.componentForm.invalid) {
      this.componentForm.markAllAsTouched();
      this.actionError.set('Component code, name, max marks, and pass marks are required.');
      return;
    }
    const v = this.componentForm.getRawValue();
    this.busy.set(true);
    this.actionError.set(null);
    this.examApi
      .addComponent(exam.id, {
        code: v.code.trim(),
        name: v.name.trim(),
        maxMarks: Number(v.maxMarks),
        passMarks: Number(v.passMarks),
      })
      .subscribe({
        next: () => {
          this.busy.set(false);
          this.componentForm.reset({ code: '', name: '', maxMarks: 100, passMarks: 40 });
          this.reload(exam.id);
        },
        error: (err) => {
          this.busy.set(false);
          this.actionError.set(httpErrorMessage(err, 'Could not add component.'));
        },
      });
  }

  openMarks(): void {
    this.runExamAction((id) => this.examApi.openMarks(id), 'Could not open marks entry.');
  }

  saveMarks(): void {
    const exam = this.exam();
    if (!exam || !this.marksEditable || this.busy()) {
      return;
    }
    const payload = this.rows.controls.map((ctrl) => {
      const v = ctrl.getRawValue() as {
        studentId: number;
        resultStatus: string;
        graceMarks: number | null;
        grade: string;
        remarks: string;
        marks: Record<string, number | null>;
      };
      const componentMarks = (exam.components ?? []).map((c) => {
        const raw = v.marks?.[String(c.id)];
        const marks =
          raw === null || raw === undefined || raw === ('' as never) || Number.isNaN(Number(raw))
            ? null
            : Number(raw);
        return { componentId: c.id, marks };
      });
      return {
        studentId: Number(v.studentId),
        resultStatus: v.resultStatus,
        graceMarks:
          v.graceMarks == null || v.graceMarks === ('' as never) || Number.isNaN(Number(v.graceMarks))
            ? null
            : Number(v.graceMarks),
        grade: v.grade?.trim() || null,
        remarks: v.remarks?.trim() || undefined,
        componentMarks,
      };
    });
    this.busy.set(true);
    this.actionError.set(null);
    this.examApi.upsertResults(exam.id, payload).subscribe({
      next: () => {
        this.busy.set(false);
        this.reload(exam.id);
      },
      error: (err) => {
        this.busy.set(false);
        this.actionError.set(httpErrorMessage(err, 'Could not save marks.'));
      },
    });
  }

  publish(): void {
    this.runExamAction((id) => this.examApi.publish(id), 'Could not publish results.');
  }

  lock(): void {
    this.runExamAction((id) => this.examApi.lock(id), 'Could not lock exam.');
  }

  unlock(): void {
    const exam = this.exam();
    if (!exam || !this.canPublish || this.busy()) {
      return;
    }
    if (this.unlockReason.invalid) {
      this.unlockReason.markAsTouched();
      this.actionError.set('Unlock reason is required.');
      return;
    }
    this.busy.set(true);
    this.actionError.set(null);
    this.examApi.unlock(exam.id, this.unlockReason.value.trim()).subscribe({
      next: () => {
        this.busy.set(false);
        this.unlockReason.reset('');
        this.reload(exam.id);
      },
      error: (err) => {
        this.busy.set(false);
        this.actionError.set(httpErrorMessage(err, 'Could not unlock exam.'));
      },
    });
  }

  private runExamAction(
    action: (id: number) => import('rxjs').Observable<Exam>,
    fallback: string,
  ): void {
    const exam = this.exam();
    if (!exam || this.busy()) {
      return;
    }
    this.busy.set(true);
    this.actionError.set(null);
    action(exam.id).subscribe({
      next: () => {
        this.busy.set(false);
        this.reload(exam.id);
      },
      error: (err) => {
        this.busy.set(false);
        this.actionError.set(httpErrorMessage(err, fallback));
      },
    });
  }

  private rebuildMarksForm(exam: Exam, results: ExamResult[]): void {
    this.rows.clear();
    for (const r of results) {
      this.rows.push(this.buildResultGroup(exam, r));
    }
  }

  private buildResultGroup(exam: Exam, result: ExamResult) {
    const marksGroup = this.fb.group(
      Object.fromEntries(
        (exam.components ?? []).map((c) => {
          const existing = result.componentMarks?.find((m) => m.componentId === c.id);
          return [String(c.id), this.fb.control<number | null>(existing?.marks ?? null)];
        }),
      ),
    );
    return this.fb.group({
      studentId: this.fb.nonNullable.control(result.studentId),
      studentLabel: this.fb.nonNullable.control(`${result.studentCode} — ${result.studentName}`),
      resultStatus: this.fb.nonNullable.control(result.resultStatus || 'MARKS'),
      graceMarks: this.fb.control<number | null>(result.graceMarks ?? null),
      grade: this.fb.nonNullable.control(result.grade ?? ''),
      remarks: this.fb.nonNullable.control(result.remarks ?? ''),
      totalMarks: this.fb.control(result.totalMarks ?? null),
      passFail: this.fb.control(result.passFail ?? null),
      marks: marksGroup,
    });
  }
}
