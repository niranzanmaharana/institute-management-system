import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { UiCard } from '../../shared/ui-card';
import { UiButton } from '../../shared/ui-button';
import { UiDatepicker } from '../../shared/ui-datepicker';
import { EmptyState } from '../../shared/empty-state';
import { LoadingState } from '../../shared/loading-state';
import { ToastService } from '../../shared/toast.service';
import { httpErrorMessage } from '../../core/http-error';
import { AuthService } from '../../core/auth.service';
import { AdmissionReportRow, ReportService } from './report.service';
import { csvCell, downloadCsv } from './report-csv';

@Component({
  selector: 'app-admissions-reports-page',
  imports: [
    ReactiveFormsModule,
    UiCard,
    UiButton,
    UiDatepicker,
    EmptyState,
    LoadingState,
  ],
  templateUrl: './admissions-reports.page.html',
  styleUrl: './reports-shared.scss',
})
export class AdmissionsReportsPage implements OnInit {
  private readonly reports = inject(ReportService);
  private readonly auth = inject(AuthService);
  private readonly toasts = inject(ToastService);
  private readonly fb = inject(FormBuilder);

  readonly admissions = signal<AdmissionReportRow[]>([]);
  readonly loading = signal(false);

  readonly form = this.fb.nonNullable.group({
    from: [''],
    to: [''],
    status: [''],
  });

  get canRun(): boolean {
    return (
      this.auth.hasPermission('admission:write') ||
      this.auth.hasPermission('admission:approve') ||
      this.auth.hasPermission('student:read')
    );
  }

  ngOnInit(): void {
    const today = new Date();
    const from = new Date(today);
    from.setDate(today.getDate() - 30);
    this.form.patchValue({
      from: from.toISOString().slice(0, 10),
      to: today.toISOString().slice(0, 10),
    });
  }

  run(): void {
    if (!this.canRun || this.loading()) return;
    const v = this.form.getRawValue();
    if (!v.from || !v.to) {
      this.toasts.warning('From and to dates are required.');
      return;
    }
    this.loading.set(true);
    this.reports.admissions(v.from, v.to, v.status || undefined).subscribe({
      next: (rows) => {
        this.admissions.set(rows);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.toasts.danger(httpErrorMessage(err, 'Could not load admissions report.'));
      },
    });
  }

  exportCsv(): void {
    const header =
      'applicationNo,applicantName,courseCode,status,phone,email,studentId,createdAt,decidedAt';
    const lines = this.admissions().map(
      (r) =>
        `${csvCell(r.applicationNo)},${csvCell(r.applicantName)},${csvCell(r.courseCode)},${r.status},${csvCell(r.phone)},${csvCell(r.email)},${r.studentId ?? ''},${r.createdAt},${r.decidedAt ?? ''}`,
    );
    downloadCsv('rp-st-03-admissions.csv', [header, ...lines].join('\n'));
  }
}
