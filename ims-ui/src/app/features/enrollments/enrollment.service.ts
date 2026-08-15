import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface Enrollment {
  id: number;
  studentId: number;
  batchId: number;
  courseId: number;
  feePlanId: number;
  status: string;
  activatedAt?: string;
  suspendedAt?: string;
  withdrawnAt?: string;
  completedAt?: string;
  cancelledAt?: string;
  createdAt: string;
}

export interface CreateEnrollmentRequest {
  studentId: number;
  batchId: number;
  feePlanId: number;
}

export interface ActivateEnrollmentRequest {
  capacityOverride?: boolean;
  overrideReason?: string;
  admissionWaiver?: boolean;
  waiverReason?: string;
}

export interface ReasonRequest {
  reason: string;
}

export interface TransferEnrollmentRequest {
  targetBatchId: number;
  newFeePlanId?: number | null;
  capacityOverride?: boolean;
  overrideReason?: string;
}

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

export interface ActivationResult {
  enrollmentId: number;
  enrollmentStatus: string;
  activatedAt: string;
  feeAccount: FeeAccount;
  invoices: Invoice[];
}

@Injectable({ providedIn: 'root' })
export class EnrollmentService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/v1/enrollments`;

  list(): Observable<Enrollment[]> {
    return this.http.get<Enrollment[]>(this.base);
  }

  create(body: CreateEnrollmentRequest): Observable<Enrollment> {
    return this.http.post<Enrollment>(this.base, body);
  }

  get(id: number): Observable<Enrollment> {
    return this.http.get<Enrollment>(`${this.base}/${id}`);
  }

  activate(
    id: number,
    body: ActivateEnrollmentRequest = {},
    idempotencyKey?: string,
  ): Observable<ActivationResult> {
    let headers = new HttpHeaders();
    if (idempotencyKey?.trim()) {
      headers = headers.set('Idempotency-Key', idempotencyKey.trim());
    }
    return this.http.post<ActivationResult>(`${this.base}/${id}/activate`, body, { headers });
  }

  cancel(id: number, body: ReasonRequest): Observable<Enrollment> {
    return this.http.post<Enrollment>(`${this.base}/${id}/cancel`, body);
  }

  suspend(id: number, body: ReasonRequest): Observable<Enrollment> {
    return this.http.post<Enrollment>(`${this.base}/${id}/suspend`, body);
  }

  resume(id: number, body: ActivateEnrollmentRequest = {}): Observable<Enrollment> {
    return this.http.post<Enrollment>(`${this.base}/${id}/resume`, body);
  }

  withdraw(id: number, body: ReasonRequest): Observable<Enrollment> {
    return this.http.post<Enrollment>(`${this.base}/${id}/withdraw`, body);
  }

  complete(id: number, body: ReasonRequest): Observable<Enrollment> {
    return this.http.post<Enrollment>(`${this.base}/${id}/complete`, body);
  }

  transfer(id: number, body: TransferEnrollmentRequest): Observable<Enrollment> {
    return this.http.post<Enrollment>(`${this.base}/${id}/transfer`, body);
  }
}
