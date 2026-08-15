import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { UiCard } from '../../shared/ui-card';
import { UiButton } from '../../shared/ui-button';
import { UiInput } from '../../shared/ui-input';
import { UiModal } from '../../shared/ui-modal';
import { EmptyState } from '../../shared/empty-state';
import { LoadingState } from '../../shared/loading-state';
import { ErrorState } from '../../shared/error-state';
import { StatusBadge } from '../../shared/status-badge';
import { httpErrorMessage, httpLoadError } from '../../core/http-error';
import { FeeAccount, FinanceService, PaymentResult } from './finance.service';

@Component({
  selector: 'app-outstanding-list-page',
  imports: [
    ReactiveFormsModule,
    UiCard,
    UiButton,
    UiInput,
    UiModal,
    EmptyState,
    LoadingState,
    ErrorState,
    StatusBadge,
  ],
  templateUrl: './outstanding-list.page.html',
  styleUrl: './outstanding-list.page.scss',
})
export class OutstandingListPage implements OnInit {
  private readonly financeApi = inject(FinanceService);
  private readonly fb = inject(FormBuilder);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly rows = signal<FeeAccount[]>([]);

  readonly payOpen = signal(false);
  readonly payTarget = signal<FeeAccount | null>(null);
  readonly paying = signal(false);
  readonly payError = signal<string | null>(null);
  readonly lastReceipt = signal<PaymentResult | null>(null);

  readonly payForm = this.fb.nonNullable.group({
    amount: ['', Validators.required],
    method: ['CASH', Validators.required],
  });

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.error.set(null);
    this.financeApi.listOutstanding().subscribe({
      next: (rows) => {
        this.rows.set(rows);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(httpLoadError(err, 'outstanding accounts'));
        this.loading.set(false);
      },
    });
  }

  openCollect(row: FeeAccount): void {
    this.payTarget.set(row);
    this.payError.set(null);
    this.lastReceipt.set(null);
    this.payForm.reset({
      amount: String(row.outstandingAmount),
      method: 'CASH',
    });
    this.payOpen.set(true);
  }

  closeCollect(): void {
    this.payOpen.set(false);
    this.payTarget.set(null);
    this.payError.set(null);
  }

  collect(): void {
    const target = this.payTarget();
    if (!target || this.payForm.invalid || this.paying()) {
      this.payForm.markAllAsTouched();
      return;
    }
    const v = this.payForm.getRawValue();
    const amount = Number(v.amount);
    if (!Number.isFinite(amount) || amount <= 0) {
      this.payError.set('Amount must be greater than zero.');
      return;
    }

    this.paying.set(true);
    this.payError.set(null);
    const key = crypto.randomUUID();
    this.financeApi
      .collectPayment(target.id, { amount, method: v.method.trim() || 'CASH' }, key)
      .subscribe({
        next: (result) => {
          this.paying.set(false);
          this.lastReceipt.set(result);
          this.payOpen.set(false);
          this.payTarget.set(null);
          this.reload();
        },
        error: (err) => {
          this.paying.set(false);
          if (err?.status === 409) {
            this.payError.set('Payment conflict — try again with a fresh amount.');
          } else {
            this.payError.set(httpErrorMessage(err, 'Could not collect payment.'));
          }
        },
      });
  }
}
