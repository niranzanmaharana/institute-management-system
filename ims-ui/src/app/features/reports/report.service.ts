import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface StudentListRow {
  studentId: number;
  studentCode: string;
  fullName: string;
  status: string;
  phone?: string | null;
  email?: string | null;
  batchId?: number | null;
  batchCode?: string | null;
  courseId?: number | null;
  courseCode?: string | null;
  enrollmentStatus?: string | null;
}

export interface ActiveCountRow {
  batchId: number;
  batchCode: string;
  batchName: string;
  courseId: number;
  courseCode?: string | null;
  courseName?: string | null;
  activeCount: number;
}

export interface AdmissionReportRow {
  applicationId: number;
  applicationNo: string;
  applicantName: string;
  courseId: number;
  courseCode?: string | null;
  status: string;
  phone?: string | null;
  email?: string | null;
  studentId?: number | null;
  createdAt: string;
  decidedAt?: string | null;
}

export interface AdjustmentReportRow {
  adjustmentId: number;
  feeAccountId: number;
  studentId?: number | null;
  studentCode?: string | null;
  studentName?: string | null;
  enrollmentId?: number | null;
  type: string;
  amount: number;
  reason: string;
  createdAt: string;
}

export interface LowAttendanceRow {
  studentId: number;
  studentCode?: string | null;
  studentName?: string | null;
  batchId: number;
  presentEffective: number;
  absent: number;
  sessionsCounted: number;
  percentage: number;
  minAttendancePercent: number;
}

export interface LowAttendanceReport {
  batchId: number;
  thresholdPercent: number;
  students: LowAttendanceRow[];
}

export interface ExamResultReportRow {
  examId: number;
  examName: string;
  examStatus: string;
  examDate: string;
  studentId: number;
  studentCode: string;
  studentName: string;
  resultStatus: string;
  totalMarks?: number | null;
  passFail?: string | null;
  grade?: string | null;
}

export interface WithdrawalReportRow {
  enrollmentId: number;
  studentId: number;
  studentCode?: string | null;
  studentName?: string | null;
  batchId: number;
  batchCode?: string | null;
  courseId: number;
  courseCode?: string | null;
  enrollmentStatus: string;
  withdrawnAt?: string | null;
  activatedAt?: string | null;
}

export interface BatchRosterRow {
  enrollmentId: number;
  studentId: number;
  studentCode: string;
  studentName: string;
  phone?: string | null;
  email?: string | null;
  batchId: number;
  batchCode: string;
  batchName: string;
  courseId: number;
  courseCode?: string | null;
  enrollmentStatus: string;
  activatedAt?: string | null;
}

@Injectable({ providedIn: 'root' })
export class ReportService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/v1/reports`;

  studentList(filters: {
    status?: string;
    batchId?: number;
    courseId?: number;
    q?: string;
  }): Observable<StudentListRow[]> {
    let params = new HttpParams();
    if (filters.status) params = params.set('status', filters.status);
    if (filters.batchId != null) params = params.set('batchId', filters.batchId);
    if (filters.courseId != null) params = params.set('courseId', filters.courseId);
    if (filters.q?.trim()) params = params.set('q', filters.q.trim());
    return this.http.get<StudentListRow[]>(`${this.base}/students`, { params });
  }

  activeCounts(courseId?: number): Observable<ActiveCountRow[]> {
    let params = new HttpParams();
    if (courseId != null) params = params.set('courseId', courseId);
    return this.http.get<ActiveCountRow[]>(`${this.base}/students/active-counts`, { params });
  }

  admissions(from: string, to: string, status?: string): Observable<AdmissionReportRow[]> {
    let params = new HttpParams().set('from', from).set('to', to);
    if (status) params = params.set('status', status);
    return this.http.get<AdmissionReportRow[]>(`${this.base}/admissions`, { params });
  }

  adjustments(from: string, to: string, type?: string): Observable<AdjustmentReportRow[]> {
    let params = new HttpParams().set('from', from).set('to', to);
    if (type) params = params.set('type', type);
    return this.http.get<AdjustmentReportRow[]>(`${this.base}/adjustments`, { params });
  }

  lowAttendance(batchId: number, thresholdPercent?: number): Observable<LowAttendanceReport> {
    let params = new HttpParams().set('batchId', batchId);
    if (thresholdPercent != null) params = params.set('thresholdPercent', thresholdPercent);
    return this.http.get<LowAttendanceReport>(`${this.base}/attendance/low`, { params });
  }

  examResults(batchId: number, examId: number): Observable<ExamResultReportRow[]> {
    const params = new HttpParams().set('batchId', batchId).set('examId', examId);
    return this.http.get<ExamResultReportRow[]>(`${this.base}/exams/results`, { params });
  }

  withdrawals(filters: {
    from: string;
    to: string;
    batchId?: number;
    courseId?: number;
  }): Observable<WithdrawalReportRow[]> {
    let params = new HttpParams().set('from', filters.from).set('to', filters.to);
    if (filters.batchId != null) params = params.set('batchId', filters.batchId);
    if (filters.courseId != null) params = params.set('courseId', filters.courseId);
    return this.http.get<WithdrawalReportRow[]>(`${this.base}/students/withdrawals`, { params });
  }

  roster(batchId: number, status?: string): Observable<BatchRosterRow[]> {
    let params = new HttpParams().set('batchId', batchId);
    if (status) params = params.set('status', status);
    return this.http.get<BatchRosterRow[]>(`${this.base}/students/roster`, { params });
  }
}
