import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { UiCard } from '../../shared/ui-card';
import { UiButton } from '../../shared/ui-button';
import { UiInput } from '../../shared/ui-input';
import { UiModal } from '../../shared/ui-modal';
import { EmptyState } from '../../shared/empty-state';
import { LoadingState } from '../../shared/loading-state';
import { ErrorState } from '../../shared/error-state';
import { StatusBadge } from '../../shared/status-badge';
import { httpErrorMessage, httpLoadError } from '../../core/http-error';
import { AuthService } from '../../core/auth.service';
import {
  Adjustment,
  AdjustmentResult,
  FeeAccount,
  FinanceService,
  Payment,
  PaymentResult,
} from './finance.service';

@Component({
  selector: 'app-outstanding-list-page',
  imports: [
    ReactiveFormsModule,
    RouterLink,
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
  private readonly auth = inject(AuthService);
  private readonly fb = inject(FormBuilder);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly rows = signal<FeeAccount[]>([]);

  readonly payOpen = signal(false);
  readonly payTarget = signal<FeeAccount | null>(null);
  readonly paying = signal(false);
  readonly payError = signal<string | null>(null);
  readonly lastReceipt = signal<PaymentResult | null>(null);
  readonly lastAdjustment = signal<AdjustmentResult | null>(null);

  readonly adjustOpen = signal(false);
  readonly adjustTarget = signal<FeeAccount | null>(null);
  readonly adjusting = signal(false);
  readonly adjustError = signal<string | null>(null);
  readonly adjustments = signal<Adjustment[]>([]);

  readonly paymentsOpen = signal(false);
  readonly paymentsTarget = signal<FeeAccount | null>(null);
  readonly payments = signal<Payment[]>([]);
  readonly paymentsError = signal<string | null>(null);
  readonly reversingId = signal<number | null>(null);
  readonly lastReversal = signal<PaymentResult | null>(null);
  readonly statementDownloadingId = signal<number | null>(null);
  readonly statementError = signal<string | null>(null);

  readonly payForm = this.fb.nonNullable.group({
    amount: ['', Validators.required],
    method: ['CASH', Validators.required],
  });

  readonly adjustForm = this.fb.nonNullable.group({
    type: ['WAIVER', Validators.required],
    amount: ['', Validators.required],
    reason: ['', [Validators.required, Validators.minLength(5)]],
  });

  get canAdjust(): boolean {
    return this.auth.hasPermission('fee:adjust');
  }

  get canReverse(): boolean {
    return this.auth.hasPermission('fee:reverse');
  }

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

  openAdjust(row: FeeAccount): void {
    this.adjustTarget.set(row);
    this.adjustError.set(null);
    this.lastAdjustment.set(null);
    this.adjustForm.reset({ type: 'WAIVER', amount: '', reason: '' });
    this.adjustOpen.set(true);
    this.financeApi.listAdjustments(row.id).subscribe({
      next: (rows) => this.adjustments.set(rows),
      error: () => this.adjustments.set([]),
    });
  }

  closeAdjust(): void {
    this.adjustOpen.set(false);
    this.adjustTarget.set(null);
    this.adjustError.set(null);
  }

  applyAdjustment(): void {
    const target = this.adjustTarget();
    if (!target || this.adjustForm.invalid || this.adjusting()) {
      this.adjustForm.markAllAsTouched();
      return;
    }
    const v = this.adjustForm.getRawValue();
    const amount = Number(v.amount);
    if (!Number.isFinite(amount) || amount <= 0) {
      this.adjustError.set('Amount must be greater than zero.');
      return;
    }
    this.adjusting.set(true);
    this.adjustError.set(null);
    this.financeApi
      .createAdjustment(target.id, {
        type: v.type,
        amount,
        reason: v.reason.trim(),
      })
      .subscribe({
        next: (result) => {
          this.adjusting.set(false);
          this.lastAdjustment.set(result);
          this.adjustOpen.set(false);
          this.adjustTarget.set(null);
          this.reload();
        },
        error: (err) => {
          this.adjusting.set(false);
          this.adjustError.set(httpErrorMessage(err, 'Could not apply adjustment.'));
        },
      });
  }

  openPayments(row: FeeAccount): void {
    this.paymentsTarget.set(row);
    this.paymentsError.set(null);
    this.lastReversal.set(null);
    this.paymentsOpen.set(true);
    this.financeApi.listPayments(row.id).subscribe({
      next: (rows) => this.payments.set(rows),
      error: (err) => {
        this.payments.set([]);
        this.paymentsError.set(httpLoadError(err, 'payments'));
      },
    });
  }

  closePayments(): void {
    this.paymentsOpen.set(false);
    this.paymentsTarget.set(null);
    this.paymentsError.set(null);
  }

  reversePayment(payment: Payment): void {
    if (!this.canReverse || payment.status !== 'SUCCESS' || this.reversingId() != null) {
      return;
    }
    const reason = window.prompt('Reverse reason (min 5 characters)', 'Entered in error');
    if (!reason || reason.trim().length < 5) {
      return;
    }
    this.reversingId.set(payment.id);
    this.paymentsError.set(null);
    this.financeApi.reversePayment(payment.id, reason.trim()).subscribe({
      next: (result) => {
        this.reversingId.set(null);
        this.lastReversal.set(result);
        this.payments.update((list) =>
          list.map((row) => (row.id === payment.id ? result.payment : row)),
        );
        this.reload();
      },
      error: (err) => {
        this.reversingId.set(null);
        this.paymentsError.set(httpErrorMessage(err, 'Could not reverse payment.'));
      },
    });
  }

  downloadStatement(row: FeeAccount): void {
    if (this.statementDownloadingId() != null) {
      return;
    }
    this.statementDownloadingId.set(row.id);
    this.statementError.set(null);
    this.financeApi.downloadStatementPdf(row.id).subscribe({
      next: (blob) => {
        this.statementDownloadingId.set(null);
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `fee-statement-${row.id}.pdf`;
        a.click();
        URL.revokeObjectURL(url);
      },
      error: (err) => {
        this.statementDownloadingId.set(null);
        this.statementError.set(httpErrorMessage(err, 'Could not download fee statement.'));
      },
    });
  }
}
