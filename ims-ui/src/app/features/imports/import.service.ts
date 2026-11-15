import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface ImportRowError {
  rowNumber: number;
  field: string;
  message: string;
  raw: string;
}

export interface ImportResult {
  entityType: string;
  totalRows: number;
  inserted: number;
  failed: number;
  insertedIds: number[];
  errors: ImportRowError[];
  errorReportCsv: string;
}

@Injectable({ providedIn: 'root' })
export class ImportService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/v1/imports`;

  downloadStudentTemplate(): Observable<Blob> {
    return this.http.get(`${this.base}/students/template`, { responseType: 'blob' });
  }

  downloadFacultyTemplate(): Observable<Blob> {
    return this.http.get(`${this.base}/faculties/template`, { responseType: 'blob' });
  }

  importStudents(file: File): Observable<ImportResult> {
    const form = new FormData();
    form.append('file', file, file.name);
    return this.http.post<ImportResult>(`${this.base}/students`, form);
  }

  importFaculties(file: File): Observable<ImportResult> {
    const form = new FormData();
    form.append('file', file, file.name);
    return this.http.post<ImportResult>(`${this.base}/faculties`, form);
  }
}
