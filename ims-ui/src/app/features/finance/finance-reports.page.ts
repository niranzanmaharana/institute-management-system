import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { UiCard } from '../../shared/ui-card';
import { UiButton } from '../../shared/ui-button';
import { UiInput } from '../../shared/ui-input';
import { UiDatepicker } from '../../shared/ui-datepicker';
import { EmptyState } from '../../shared/empty-state';
import { LoadingState } from '../../shared/loading-state';
import { ErrorState } from '../../shared/error-state';
import { StatusBadge } from '../../shared/status-badge';
import { httpErrorMessage, httpLoadError } from '../../core/http-error';
import { AuthService } from '../../core/auth.service';
import {
  AgingReport,
  DailyCollectionRow,
  FinanceService,
  FinancialYear,
  NamedAmount,
  OutstandingReportRow,
} from './finance.service';

@Component({
  selector: 'app-finance-reports-page',
  imports: [
    ReactiveFormsModule,
    UiCard,
    UiButton,
    UiInput,
    UiDatepicker,
    EmptyState,
    LoadingState,
    ErrorState,
    StatusBadge,
  ],
  templateUrl: './finance-reports.page.html',
  styleUrl: './finance-reports.page.scss',
})
export class FinanceReportsPage implements OnInit {
  private readonly financeApi = inject(FinanceService);
  private readonly auth = inject(AuthService);
  private readonly fb = inject(FormBuilder);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly aging = signal<AgingReport | null>(null);
  readonly outstanding = signal<OutstandingReportRow[]>([]);
  readonly daily = signal<DailyCollectionRow[]>([]);
  readonly byMethod = signal<NamedAmount[]>([]);
  readonly byCategory = signal<NamedAmount[]>([]);
  readonly years = signal<FinancialYear[]>([]);
  readonly yearError = signal<string | null>(null);
  readonly yearSaving = signal(false);
  readonly activatingId = signal<number | null>(null);

  readonly rangeForm = this.fb.nonNullable.group({
    from: [''],
    to: [''],
  });

  readonly yearForm = this.fb.nonNullable.group({
    name: ['', Validators.required],
    startDate: ['', Validators.required],
    endDate: ['', Validators.required],
    activate: [true],
  });

  get canManageYears(): boolean {
    return this.auth.hasPermission('fee:adjust');
  }

  ngOnInit(): void {
    const today = new Date();
    const from = new Date(today);
    from.setDate(today.getDate() - 30);
    this.rangeForm.patchValue({
      from: from.toISOString().slice(0, 10),
      to: today.toISOString().slice(0, 10),
    });
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.error.set(null);
    const v = this.rangeForm.getRawValue();
    forkJoin({
      aging: this.financeApi.agingReport(),
      outstanding: this.financeApi.outstandingReport(),
      daily: this.financeApi.dailyCollection(v.from || undefined, v.to || undefined),
      byMethod: this.financeApi.collectionByMethod(v.from || undefined, v.to || undefined),
      byCategory: this.financeApi.collectionByCategory(v.from || undefined, v.to || undefined),
      years: this.financeApi.listFinancialYears(),
    }).subscribe({
      next: (data) => {
        this.aging.set(data.aging);
        this.outstanding.set(data.outstanding);
        this.daily.set(data.daily);
        this.byMethod.set(data.byMethod);
        this.byCategory.set(data.byCategory);
        this.years.set(data.years);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(httpLoadError(err, 'finance reports'));
        this.loading.set(false);
      },
    });
  }

  createYear(): void {
    if (!this.canManageYears || this.yearForm.invalid || this.yearSaving()) {
      this.yearForm.markAllAsTouched();
      return;
    }
    const v = this.yearForm.getRawValue();
    this.yearSaving.set(true);
    this.yearError.set(null);
    this.financeApi
      .createFinancialYear({
        name: v.name.trim(),
        startDate: v.startDate,
        endDate: v.endDate,
        activate: v.activate,
      })
      .subscribe({
        next: () => {
          this.yearSaving.set(false);
          this.yearForm.reset({ name: '', startDate: '', endDate: '', activate: true });
          this.reload();
        },
        error: (err) => {
          this.yearSaving.set(false);
          this.yearError.set(httpErrorMessage(err, 'Could not create financial year.'));
        },
      });
  }

  activateYear(row: FinancialYear): void {
    if (!this.canManageYears || this.activatingId() != null) {
      return;
    }
    this.activatingId.set(row.id);
    this.yearError.set(null);
    this.financeApi.activateFinancialYear(row.id).subscribe({
      next: () => {
        this.activatingId.set(null);
        this.reload();
      },
      error: (err) => {
        this.activatingId.set(null);
        this.yearError.set(httpErrorMessage(err, 'Could not activate financial year.'));
      },
    });
  }

  exportAgingCsv(): void {
    const report = this.aging();
    if (!report) {
      return;
    }
    const header = 'invoiceNo,feeAccountId,studentId,dueDate,daysOverdue,bucket,remaining,status';
    const lines = report.invoices.map(
      (r) =>
        `${r.invoiceNo},${r.feeAccountId},${r.studentId ?? ''},${r.dueDate},${r.daysOverdue},${r.bucket},${r.remaining},${r.status}`,
    );
    this.downloadCsv('aging-outstanding.csv', [header, ...lines].join('\n'));
  }

  exportDailyCsv(): void {
    const header = 'date,amount,paymentCount';
    const lines = this.daily().map((r) => `${r.date},${r.amount},${r.paymentCount}`);
    this.downloadCsv('daily-collection.csv', [header, ...lines].join('\n'));
  }

  exportMethodCsv(): void {
    const header = 'method,amount,count';
    const lines = this.byMethod().map((r) => `${r.label},${r.amount},${r.count}`);
    this.downloadCsv('collection-by-method.csv', [header, ...lines].join('\n'));
  }

  exportCategoryCsv(): void {
    const header = 'category,amount,allocationCount';
    const lines = this.byCategory().map((r) => `"${r.label.replaceAll('"', '""')}",${r.amount},${r.count}`);
    this.downloadCsv('collection-by-category.csv', [header, ...lines].join('\n'));
  }

  exportOutstandingCsv(): void {
    const header = 'feeAccountId,studentId,enrollmentId,currency,outstandingAmount';
    const lines = this.outstanding().map(
      (r) =>
        `${r.feeAccountId},${r.studentId},${r.enrollmentId},${r.currency},${r.outstandingAmount}`,
    );
    this.downloadCsv('outstanding-fees.csv', [header, ...lines].join('\n'));
  }

  private downloadCsv(filename: string, content: string): void {
    const blob = new Blob([content], { type: 'text/csv;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    URL.revokeObjectURL(url);
  }
}
