import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { UiCard } from '../../shared/ui-card';
import { UiButton } from '../../shared/ui-button';
import { UiInput } from '../../shared/ui-input';
import { UiDatepicker } from '../../shared/ui-datepicker';
import { EmptyState } from '../../shared/empty-state';
import { LoadingState } from '../../shared/loading-state';
import { ErrorState } from '../../shared/error-state';
import { BadgeTone, StatusBadge } from '../../shared/status-badge';
import { httpErrorMessage, httpLoadError } from '../../core/http-error';
import { AuthService } from '../../core/auth.service';
import { AcademicService, Batch } from '../academic/academic.service';
import { Exam, ExamService } from './exam.service';

@Component({
  selector: 'app-exams-list-page',
  imports: [
    FormsModule,
    ReactiveFormsModule,
    RouterLink,
    UiCard,
    UiButton,
    UiInput,
    UiDatepicker,
    EmptyState,
    LoadingState,
    ErrorState,
    StatusBadge,
  ],
  templateUrl: './exams-list.page.html',
  styleUrl: './exams-list.page.scss',
})
export class ExamsListPage implements OnInit {
  private readonly examApi = inject(ExamService);
  private readonly academicApi = inject(AcademicService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly rows = signal<Exam[]>([]);
  readonly batches = signal<Batch[]>([]);
  readonly filterBatchId = signal('');
  readonly creating = signal(false);
  readonly createError = signal<string | null>(null);

  readonly createForm = this.fb.nonNullable.group({
    batchId: ['', Validators.required],
    name: ['', Validators.required],
    examDate: [new Date().toISOString().slice(0, 10), Validators.required],
  });

  get canMark(): boolean {
    return this.auth.hasPermission('exam:mark');
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

  onFilterBatch(value: string): void {
    this.filterBatchId.set(value);
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.error.set(null);
    const batchId = Number(this.filterBatchId());
    this.examApi.list(Number.isFinite(batchId) && batchId > 0 ? batchId : undefined).subscribe({
      next: (rows) => {
        this.rows.set(rows);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(httpLoadError(err, 'exams'));
        this.loading.set(false);
      },
    });
  }

  create(): void {
    if (!this.canMark || this.creating()) {
      return;
    }
    if (this.createForm.invalid) {
      this.createForm.markAllAsTouched();
      this.createError.set('Batch, name, and exam date are required.');
      return;
    }
    const v = this.createForm.getRawValue();
    this.creating.set(true);
    this.createError.set(null);
    this.examApi
      .create({
        batchId: Number(v.batchId),
        name: v.name.trim(),
        examDate: v.examDate,
      })
      .subscribe({
        next: (exam) => {
          this.creating.set(false);
          void this.router.navigate(['/exams', exam.id]);
        },
        error: (err) => {
          this.creating.set(false);
          this.createError.set(httpErrorMessage(err, 'Could not create exam.'));
        },
      });
  }
}
