import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface AttendanceRecord {
  id?: number | null;
  studentId: number;
  studentCode: string;
  studentName: string;
  status: string | null;
  note?: string | null;
  eligible: boolean;
}

export interface AttendanceSession {
  id: number;
  batchId: number;
  sessionDate: string;
  subjectId?: number | null;
  status: string;
  markedBy?: number | null;
  lockedAt?: string | null;
  unlockedAt?: string | null;
  createdAt: string;
  records: AttendanceRecord[];
}

export interface StudentAttendanceSummary {
  studentId: number;
  batchId: number;
  presentEffective: number;
  absent: number;
  excused: number;
  sessionsCounted: number;
  percentage: number;
  minAttendancePercent: number;
  belowThreshold: boolean;
}

export interface BatchAttendanceSummary {
  batchId: number;
  sessionCount: number;
  draftCount: number;
  submittedCount: number;
  lockedCount: number;
  students: StudentAttendanceSummary[];
}

@Injectable({ providedIn: 'root' })
export class AttendanceService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/v1`;

  listSessions(batchId?: number): Observable<AttendanceSession[]> {
    let params = new HttpParams();
    if (batchId != null) {
      params = params.set('batchId', batchId);
    }
    return this.http.get<AttendanceSession[]>(`${this.base}/attendance/sessions`, { params });
  }

  getSession(id: number): Observable<AttendanceSession> {
    return this.http.get<AttendanceSession>(`${this.base}/attendance/sessions/${id}`);
  }

  createSession(body: {
    batchId: number;
    sessionDate: string;
    subjectId?: number | null;
  }): Observable<AttendanceSession> {
    return this.http.post<AttendanceSession>(`${this.base}/attendance/sessions`, body);
  }

  markRecords(
    sessionId: number,
    records: { studentId: number; status: string; note?: string }[],
  ): Observable<AttendanceSession> {
    return this.http.put<AttendanceSession>(
      `${this.base}/attendance/sessions/${sessionId}/records`,
      { records },
    );
  }

  submit(sessionId: number): Observable<AttendanceSession> {
    return this.http.post<AttendanceSession>(
      `${this.base}/attendance/sessions/${sessionId}/submit`,
      {},
    );
  }

  lock(sessionId: number): Observable<AttendanceSession> {
    return this.http.post<AttendanceSession>(
      `${this.base}/attendance/sessions/${sessionId}/lock`,
      {},
    );
  }

  unlock(sessionId: number, reason: string): Observable<AttendanceSession> {
    return this.http.post<AttendanceSession>(
      `${this.base}/attendance/sessions/${sessionId}/unlock`,
      { reason },
    );
  }

  batchSummary(batchId: number): Observable<BatchAttendanceSummary> {
    return this.http.get<BatchAttendanceSummary>(
      `${this.base}/attendance/batches/${batchId}/summary`,
    );
  }
}
