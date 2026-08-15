import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface Admission {
  id: number;
  applicationNo: string;
  enquiryId?: number;
  courseId: number;
  applicantName: string;
  phone?: string;
  email?: string;
  status: string;
  studentId?: number;
  decidedBy?: number;
  decisionReason?: string;
  decidedAt?: string;
  createdAt: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface CreateAdmissionRequest {
  applicationNo?: string;
  courseId: number;
  applicantName: string;
  phone?: string;
  email?: string;
  enquiryId?: number;
}

export interface CreateStudentOnApprove {
  studentCode?: string;
  firstName: string;
  lastName: string;
  phone?: string;
  email?: string;
}

export interface ApproveAdmissionRequest {
  createStudent?: CreateStudentOnApprove;
  studentId?: number;
  reason?: string;
}

export interface ReasonRequest {
  reason: string;
}

@Injectable({ providedIn: 'root' })
export class AdmissionService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/v1/admissions`;

  list(q = '', page = 0, size = 20, status = ''): Observable<PageResponse<Admission>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (q.trim()) {
      params = params.set('q', q.trim());
    }
    if (status.trim()) {
      params = params.set('status', status.trim());
    }
    return this.http.get<PageResponse<Admission>>(this.base, { params });
  }

  get(id: number): Observable<Admission> {
    return this.http.get<Admission>(`${this.base}/${id}`);
  }

  create(body: CreateAdmissionRequest): Observable<Admission> {
    return this.http.post<Admission>(this.base, body);
  }

  approve(id: number, body: ApproveAdmissionRequest): Observable<Admission> {
    return this.http.post<Admission>(`${this.base}/${id}/approve`, body);
  }

  reject(id: number, body: ReasonRequest): Observable<Admission> {
    return this.http.post<Admission>(`${this.base}/${id}/reject`, body);
  }

  cancel(id: number, body: ReasonRequest): Observable<Admission> {
    return this.http.post<Admission>(`${this.base}/${id}/cancel`, body);
  }
}
