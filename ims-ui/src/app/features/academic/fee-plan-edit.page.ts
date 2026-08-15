import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { UiCard } from '../../shared/ui-card';
import { UiButton } from '../../shared/ui-button';
import { UiInput } from '../../shared/ui-input';
import { ErrorState } from '../../shared/error-state';
import { LoadingState } from '../../shared/loading-state';
import { httpErrorMessage, httpLoadError } from '../../core/http-error';
import { AcademicService, FeeCategory } from './academic.service';

@Component({
  selector: 'app-fee-plan-edit-page',
  imports: [ReactiveFormsModule, RouterLink, UiCard, UiButton, UiInput, ErrorState, LoadingState],
  templateUrl: './fee-plan-edit.page.html',
  styleUrl: './fee-plan-edit.page.scss',
})
export class FeePlanEditPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly academicApi = inject(AcademicService);

  readonly loading = signal(true);
  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);
  readonly categories = signal<FeeCategory[]>([]);
  planId = 0;
  courseId = 0;
  code = '';
  currency = 'INR';

  readonly form = this.fb.nonNullable.group({
    name: ['', Validators.required],
    totalAmount: ['', Validators.required],
    feeCategoryId: ['', Validators.required],
    installmentLabel: ['Full fee', Validators.required],
    dueOffsetDays: ['0'],
  });

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isFinite(id) || id <= 0) {
      this.error.set('Invalid fee plan id.');
      this.loading.set(false);
      return;
    }
    this.planId = id;
    this.academicApi.listFeeCategories().subscribe({
      next: (cats) => this.categories.set(cats),
      error: (err) => this.error.set(httpLoadError(err, 'fee categories')),
    });
    this.academicApi.getFeePlan(id).subscribe({
      next: (plan) => {
        this.code = plan.code;
        this.courseId = plan.courseId;
        this.currency = plan.currency;
        const first = plan.installments[0];
        this.form.patchValue({
          name: plan.name,
          totalAmount: String(plan.totalAmount),
          feeCategoryId: first ? String(first.feeCategoryId) : '',
          installmentLabel: first?.label || 'Full fee',
          dueOffsetDays: String(first?.dueOffsetDays ?? 0),
        });
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(httpLoadError(err, 'fee plan'));
        this.loading.set(false);
      },
    });
  }

  submit(): void {
    if (this.form.invalid || this.submitting() || !this.planId) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const totalAmount = Number(v.totalAmount);
    const feeCategoryId = Number(v.feeCategoryId);
    const dueOffsetDays = Number(v.dueOffsetDays || '0');
    if (!Number.isFinite(totalAmount) || totalAmount <= 0) {
      this.error.set('Total amount must be a positive number.');
      return;
    }
    if (!Number.isFinite(feeCategoryId) || feeCategoryId <= 0) {
      this.error.set('Select a fee category.');
      return;
    }
    this.submitting.set(true);
    this.error.set(null);
    this.academicApi
      .updateFeePlan(this.planId, {
        name: v.name.trim(),
        totalAmount,
        installments: [
          {
            seq: 1,
            feeCategoryId,
            label: v.installmentLabel.trim() || 'Full fee',
            amount: totalAmount,
            dueOffsetDays: Number.isFinite(dueOffsetDays) ? dueOffsetDays : 0,
          },
        ],
      })
      .subscribe({
        next: () => {
          this.submitting.set(false);
          void this.router.navigate(['/courses', this.courseId]);
        },
        error: (err) => {
          this.submitting.set(false);
          if (err?.status === 400) {
            this.error.set('Invalid fee plan. Check amounts and installments.');
          } else {
            this.error.set(httpErrorMessage(err, 'Could not save fee plan.'));
          }
        },
      });
  }
}
