import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { UiCard } from '../../shared/ui-card';
import { UiButton } from '../../shared/ui-button';
import { UiInput } from '../../shared/ui-input';
import { ErrorState } from '../../shared/error-state';
import { LoadingState } from '../../shared/loading-state';
import { httpErrorMessage, httpLoadError } from '../../core/http-error';
import { AcademicService } from './academic.service';

@Component({
  selector: 'app-batch-edit-page',
  imports: [ReactiveFormsModule, RouterLink, UiCard, UiButton, UiInput, ErrorState, LoadingState],
  templateUrl: './batch-edit.page.html',
  styleUrl: './batch-edit.page.scss',
})
export class BatchEditPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly academicApi = inject(AcademicService);

  readonly loading = signal(true);
  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);
  batchId = 0;
  code = '';

  readonly form = this.fb.nonNullable.group({
    name: ['', Validators.required],
    capacity: ['30', Validators.required],
    status: ['OPEN', Validators.required],
    startDate: [''],
    endDate: [''],
  });

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isFinite(id) || id <= 0) {
      this.error.set('Invalid batch id.');
      this.loading.set(false);
      return;
    }
    this.batchId = id;
    this.academicApi.getBatch(id).subscribe({
      next: (row) => {
        this.code = row.code;
        this.form.patchValue({
          name: row.name,
          capacity: String(row.capacity),
          status: row.status || 'OPEN',
          startDate: row.startDate || '',
          endDate: row.endDate || '',
        });
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(httpLoadError(err, 'batch'));
        this.loading.set(false);
      },
    });
  }

  submit(): void {
    if (this.form.invalid || this.submitting() || !this.batchId) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const capacity = Number(v.capacity);
    if (!Number.isFinite(capacity) || capacity < 1) {
      this.error.set('Capacity must be at least 1.');
      return;
    }
    this.submitting.set(true);
    this.error.set(null);
    this.academicApi
      .updateBatch(this.batchId, {
        name: v.name.trim(),
        capacity,
        status: v.status.trim() || undefined,
        startDate: v.startDate || undefined,
        endDate: v.endDate || undefined,
      })
      .subscribe({
        next: () => {
          this.submitting.set(false);
          void this.router.navigate(['/batches', this.batchId]);
        },
        error: (err) => {
          this.submitting.set(false);
          this.error.set(httpErrorMessage(err, 'Could not save batch.'));
        },
      });
  }
}
