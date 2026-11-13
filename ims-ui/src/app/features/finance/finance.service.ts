import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface FeeAccount {
  id: number;
  studentId: number;
  enrollmentId: number;
  currency: string;
  status: string;
  outstandingAmount: number;
  version: number;
}

export interface Invoice {
  id: number;
  feeAccountId: number;
  invoiceNo: string;
  feeCategoryId: number;
  financialYearId: number;
  description: string;
  amount: number;
  amountPaid: number;
  dueDate: string;
  status: string;
  installmentNo: number;
}

export interface Payment {
  id: number;
  feeAccountId: number;
  paymentNo: string;
  amount: number;
  method: string;
  paidAt: string;
  status: string;
  idempotencyKey?: string;
  reverseReason?: string | null;
  reversedAt?: string | null;
}

export interface Receipt {
  id: number;
  paymentId: number;
  receiptNo: string;
  issuedAt: string;
  status?: string;
}

export interface PaymentResult {
  payment: Payment;
  receipt: Receipt;
  feeAccount: FeeAccount;
}

export interface CollectPaymentRequest {
  amount: number;
  method: string;
}

export interface Adjustment {
  id: number;
  feeAccountId: number;
  type: string;
  amount: number;
  reason: string;
  approvedBy?: number | null;
  createdAt: string;
}

export interface AdjustmentResult {
  adjustment: Adjustment;
  feeAccount: FeeAccount;
}

export interface CreateAdjustmentRequest {
  type: string;
  amount: number;
  reason: string;
}

export interface RefundRequestRow {
  id: number;
  feeAccountId: number;
  amount: number;
  status: string;
  reason: string;
  requestedBy?: number | null;
  decidedBy?: number | null;
  processedAt?: string | null;
  processNote?: string | null;
  createdAt: string;
}

export interface RefundResult {
  refund: RefundRequestRow;
  feeAccount: FeeAccount;
}

export interface AgingBucket {
  bucket: string;
  amount: number;
  invoiceCount: number;
}

export interface AgingInvoiceRow {
  invoiceId: number;
  invoiceNo: string;
  feeAccountId: number;
  studentId?: number | null;
  dueDate: string;
  daysOverdue: number;
  bucket: string;
  remaining: number;
  status: string;
}

export interface AgingReport {
  asOf: string;
  buckets: AgingBucket[];
  invoices: AgingInvoiceRow[];
}

export interface OutstandingReportRow {
  feeAccountId: number;
  studentId: number;
  enrollmentId: number;
  currency: string;
  outstandingAmount: number;
}

export interface DailyCollectionRow {
  date: string;
  amount: number;
  paymentCount: number;
}

export interface NamedAmount {
  key: string;
  label: string;
  amount: number;
  count: number;
}

export interface FinancialYear {
  id: number;
  name: string;
  startDate: string;
  endDate: string;
  active: boolean;
}

@Injectable({ providedIn: 'root' })
export class FinanceService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/v1`;

  listOutstanding(): Observable<FeeAccount[]> {
    return this.http.get<FeeAccount[]>(`${this.base}/outstanding`);
  }

  listFeeAccountsByStudent(studentId: number): Observable<FeeAccount[]> {
    const params = new HttpParams().set('studentId', studentId);
    return this.http.get<FeeAccount[]>(`${this.base}/fee-accounts`, { params });
  }

  listInvoices(feeAccountId: number): Observable<Invoice[]> {
    return this.http.get<Invoice[]>(`${this.base}/fee-accounts/${feeAccountId}/invoices`);
  }

  downloadStatementPdf(feeAccountId: number): Observable<Blob> {
    return this.http.get(`${this.base}/fee-accounts/${feeAccountId}/statement.pdf`, {
      responseType: 'blob',
    });
  }

  listPayments(feeAccountId: number): Observable<Payment[]> {
    return this.http.get<Payment[]>(`${this.base}/fee-accounts/${feeAccountId}/payments`);
  }

  collectPayment(
    feeAccountId: number,
    body: CollectPaymentRequest,
    idempotencyKey: string,
  ): Observable<PaymentResult> {
    const headers = new HttpHeaders().set('Idempotency-Key', idempotencyKey);
    return this.http.post<PaymentResult>(
      `${this.base}/fee-accounts/${feeAccountId}/payments`,
      body,
      { headers },
    );
  }

  reversePayment(paymentId: number, reason: string): Observable<PaymentResult> {
    return this.http.post<PaymentResult>(`${this.base}/payments/${paymentId}/reverse`, { reason });
  }

  listAdjustments(feeAccountId: number): Observable<Adjustment[]> {
    return this.http.get<Adjustment[]>(`${this.base}/fee-accounts/${feeAccountId}/adjustments`);
  }

  createAdjustment(
    feeAccountId: number,
    body: CreateAdjustmentRequest,
  ): Observable<AdjustmentResult> {
    return this.http.post<AdjustmentResult>(
      `${this.base}/fee-accounts/${feeAccountId}/adjustments`,
      body,
    );
  }

  listRefunds(feeAccountId?: number, status?: string): Observable<RefundRequestRow[]> {
    let params = new HttpParams();
    if (feeAccountId != null) {
      params = params.set('feeAccountId', feeAccountId);
    }
    if (status?.trim()) {
      params = params.set('status', status.trim());
    }
    return this.http.get<RefundRequestRow[]>(`${this.base}/refunds`, { params });
  }

  requestRefund(
    feeAccountId: number,
    body: { amount: number; reason: string },
  ): Observable<RefundResult> {
    return this.http.post<RefundResult>(`${this.base}/fee-accounts/${feeAccountId}/refunds`, body);
  }

  approveRefund(id: number): Observable<RefundRequestRow> {
    return this.http.post<RefundRequestRow>(`${this.base}/refunds/${id}/approve`, {});
  }

  rejectRefund(id: number, reason: string): Observable<RefundRequestRow> {
    return this.http.post<RefundRequestRow>(`${this.base}/refunds/${id}/reject`, { reason });
  }

  processRefund(
    id: number,
    body: { success: boolean; note?: string },
    idempotencyKey: string,
  ): Observable<RefundResult> {
    const headers = new HttpHeaders().set('Idempotency-Key', idempotencyKey);
    return this.http.post<RefundResult>(`${this.base}/refunds/${id}/process`, body, { headers });
  }

  agingReport(asOf?: string): Observable<AgingReport> {
    let params = new HttpParams();
    if (asOf?.trim()) {
      params = params.set('asOf', asOf.trim());
    }
    return this.http.get<AgingReport>(`${this.base}/finance/reports/aging`, { params });
  }

  outstandingReport(): Observable<OutstandingReportRow[]> {
    return this.http.get<OutstandingReportRow[]>(`${this.base}/finance/reports/outstanding`);
  }

  dailyCollection(from?: string, to?: string): Observable<DailyCollectionRow[]> {
    return this.http.get<DailyCollectionRow[]>(`${this.base}/finance/reports/daily-collection`, {
      params: this.dateParams(from, to),
    });
  }

  collectionByMethod(from?: string, to?: string): Observable<NamedAmount[]> {
    return this.http.get<NamedAmount[]>(`${this.base}/finance/reports/collection-by-method`, {
      params: this.dateParams(from, to),
    });
  }

  collectionByCategory(from?: string, to?: string): Observable<NamedAmount[]> {
    return this.http.get<NamedAmount[]>(`${this.base}/finance/reports/collection-by-category`, {
      params: this.dateParams(from, to),
    });
  }

  listFinancialYears(): Observable<FinancialYear[]> {
    return this.http.get<FinancialYear[]>(`${this.base}/financial-years`);
  }

  createFinancialYear(body: {
    name: string;
    startDate: string;
    endDate: string;
    activate: boolean;
  }): Observable<FinancialYear> {
    return this.http.post<FinancialYear>(`${this.base}/financial-years`, body);
  }

  activateFinancialYear(id: number): Observable<FinancialYear> {
    return this.http.post<FinancialYear>(`${this.base}/financial-years/${id}/activate`, {});
  }

  private dateParams(from?: string, to?: string): HttpParams {
    let params = new HttpParams();
    if (from?.trim()) {
      params = params.set('from', from.trim());
    }
    if (to?.trim()) {
      params = params.set('to', to.trim());
    }
    return params;
  }
}
