import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { UiCard } from '../../shared/ui-card';
import { UiButton } from '../../shared/ui-button';
import { UiInput } from '../../shared/ui-input';
import { EmptyState } from '../../shared/empty-state';
import { LoadingState } from '../../shared/loading-state';
import { ErrorState } from '../../shared/error-state';
import { StatusBadge } from '../../shared/status-badge';
import { ToastService } from '../../shared/toast.service';
import { Observable } from 'rxjs';
import { httpErrorMessage, httpLoadError } from '../../core/http-error';
import { AuthService } from '../../core/auth.service';
import { FinanceService, RefundRequestRow } from './finance.service';

@Component({
  selector: 'app-refunds-list-page',
  imports: [
    ReactiveFormsModule,
    UiCard,
    UiButton,
    UiInput,
    EmptyState,
    LoadingState,
    ErrorState,
    StatusBadge,
  ],
  templateUrl: './refunds-list.page.html',
  styleUrl: './refunds-list.page.scss',
})
export class RefundsListPage implements OnInit {
  private readonly financeApi = inject(FinanceService);
  private readonly auth = inject(AuthService);
  private readonly toasts = inject(ToastService);
  private readonly fb = inject(FormBuilder);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly rows = signal<RefundRequestRow[]>([]);
  readonly busyId = signal<number | null>(null);
  readonly requesting = signal(false);

  readonly requestForm = this.fb.nonNullable.group({
    feeAccountId: ['', Validators.required],
    amount: ['', Validators.required],
    reason: ['', [Validators.required, Validators.minLength(5)]],
  });

  get canRefund(): boolean {
    return this.auth.hasPermission('fee:refund');
  }

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.error.set(null);
    this.financeApi.listRefunds().subscribe({
      next: (rows) => {
        this.rows.set(rows);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(httpLoadError(err, 'refunds'));
        this.loading.set(false);
      },
    });
  }

  requestRefund(): void {
    if (!this.canRefund || this.requestForm.invalid || this.requesting()) {
      this.requestForm.markAllAsTouched();
      return;
    }
    const v = this.requestForm.getRawValue();
    const feeAccountId = Number(v.feeAccountId);
    const amount = Number(v.amount);
    this.requesting.set(true);
    this.financeApi.requestRefund(feeAccountId, { amount, reason: v.reason.trim() }).subscribe({
      next: () => {
        this.requesting.set(false);
        this.requestForm.reset({ feeAccountId: '', amount: '', reason: '' });
        this.reload();
      },
      error: (err) => {
        this.requesting.set(false);
        this.toasts.danger(httpErrorMessage(err, 'Could not request refund.'));
      },
    });
  }

  approve(row: RefundRequestRow): void {
    this.run(row.id, () => this.financeApi.approveRefund(row.id));
  }

  reject(row: RefundRequestRow): void {
    const reason = window.prompt('Reject reason (min 5 characters)', 'Not eligible');
    if (!reason || reason.trim().length < 5) {
      return;
    }
    this.run(row.id, () => this.financeApi.rejectRefund(row.id, reason.trim()));
  }

  process(row: RefundRequestRow, success: boolean): void {
    this.run(row.id, () =>
      this.financeApi.processRefund(row.id, { success, note: success ? 'Paid out' : 'Payout failed' }, crypto.randomUUID()),
    );
  }

  exportCsv(): void {
    const header = 'id,feeAccountId,amount,status,reason,createdAt';
    const lines = this.rows().map(
      (r) =>
        `${r.id},${r.feeAccountId},${r.amount},${r.status},"${(r.reason || '').replaceAll('"', '""')}",${r.createdAt}`,
    );
    const blob = new Blob([[header, ...lines].join('\n')], { type: 'text/csv;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'refunds.csv';
    a.click();
    URL.revokeObjectURL(url);
  }

  private run(id: number, call: () => Observable<unknown>): void {
    this.busyId.set(id);
    call().subscribe({
      next: () => {
        this.busyId.set(null);
        this.reload();
      },
      error: (err) => {
        this.busyId.set(null);
        this.toasts.danger(httpErrorMessage(err, 'Refund action failed.'));
      },
    });
  }
}
