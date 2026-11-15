import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface ExamComponent {
  id: number;
  subjectId?: number | null;
  code: string;
  name: string;
  maxMarks: number;
  passMarks: number;
  weightage?: number | null;
  seq: number;
}

export interface Exam {
  id: number;
  batchId: number;
  name: string;
  examDate: string;
  status: string;
  originalExamId?: number | null;
  publishedAt?: string | null;
  lockedAt?: string | null;
  components: ExamComponent[];
}

export interface ComponentMark {
  componentId: number;
  componentCode: string;
  marks?: number | null;
}

export interface ExamResult {
  id: number;
  examId: number;
  studentId: number;
  studentCode: string;
  studentName: string;
  resultStatus: string;
  totalMarks?: number | null;
  graceMarks?: number | null;
  passFail?: string | null;
  grade?: string | null;
  remarks?: string | null;
  componentMarks: ComponentMark[];
}

export interface MarksImportJob {
  id: number;
  examId: number;
  status: string;
  fileName?: string | null;
  totalRows: number;
  successCount: number;
  failedCount: number;
  errorReportCsv: string;
  message?: string | null;
  async: boolean;
  createdAt?: string;
  startedAt?: string | null;
  completedAt?: string | null;
}

@Injectable({ providedIn: 'root' })
export class ExamService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/v1`;

  listGrades(): Observable<string[]> {
    return this.http.get<string[]>(`${this.base}/exam-grades`);
  }

  list(batchId?: number): Observable<Exam[]> {
    let params = new HttpParams();
    if (batchId != null) {
      params = params.set('batchId', batchId);
    }
    return this.http.get<Exam[]>(`${this.base}/exams`, { params });
  }

  get(id: number): Observable<Exam> {
    return this.http.get<Exam>(`${this.base}/exams/${id}`);
  }

  create(body: {
    batchId: number;
    name: string;
    examDate: string;
    originalExamId?: number | null;
  }): Observable<Exam> {
    return this.http.post<Exam>(`${this.base}/exams`, body);
  }

  addComponent(
    examId: number,
    body: {
      code: string;
      name: string;
      maxMarks: number;
      passMarks: number;
      subjectId?: number | null;
      seq?: number;
    },
  ): Observable<ExamComponent> {
    return this.http.post<ExamComponent>(`${this.base}/exams/${examId}/components`, body);
  }

  openMarks(examId: number): Observable<Exam> {
    return this.http.post<Exam>(`${this.base}/exams/${examId}/open-marks`, {});
  }

  listResults(examId: number): Observable<ExamResult[]> {
    return this.http.get<ExamResult[]>(`${this.base}/exams/${examId}/results`);
  }

  upsertResults(
    examId: number,
    results: {
      studentId: number;
      resultStatus?: string;
      graceMarks?: number | null;
      grade?: string | null;
      remarks?: string;
      componentMarks?: { componentId: number; marks: number | null }[];
    }[],
  ): Observable<ExamResult[]> {
    return this.http.put<ExamResult[]>(`${this.base}/exams/${examId}/results`, { results });
  }

  publish(examId: number): Observable<Exam> {
    return this.http.post<Exam>(`${this.base}/exams/${examId}/publish`, {});
  }

  lock(examId: number): Observable<Exam> {
    return this.http.post<Exam>(`${this.base}/exams/${examId}/lock`, {});
  }

  unlock(examId: number, reason: string): Observable<Exam> {
    return this.http.post<Exam>(`${this.base}/exams/${examId}/unlock`, { reason });
  }

  downloadMarksTemplate(examId: number): Observable<Blob> {
    return this.http.get(`${this.base}/exams/${examId}/marks-import/template`, {
      responseType: 'blob',
    });
  }

  uploadMarksCsv(examId: number, file: File): Observable<MarksImportJob> {
    const form = new FormData();
    form.append('file', file, file.name);
    return this.http.post<MarksImportJob>(`${this.base}/exams/${examId}/marks-import`, form);
  }

  getMarksImportJob(jobId: number): Observable<MarksImportJob> {
    return this.http.get<MarksImportJob>(`${this.base}/marks-import-jobs/${jobId}`);
  }
}
