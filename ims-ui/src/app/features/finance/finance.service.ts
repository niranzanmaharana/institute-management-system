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
}

export interface Receipt {
  id: number;
  paymentId: number;
  receiptNo: string;
  issuedAt: string;
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
}
