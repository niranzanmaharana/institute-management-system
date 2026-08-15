import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export type CodeEntityType =
  | 'STUDENT'
  | 'COURSE'
  | 'FEE_PLAN'
  | 'BATCH'
  | 'ACADEMIC_YEAR'
  | 'FEE_CATEGORY'
  | 'ADMISSION'
  | 'INSTITUTE'
  | 'FACULTY'
  | 'STAFF';

export interface NextCodeResponse {
  type: string;
  code: string;
}

@Injectable({ providedIn: 'root' })
export class CodeService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/v1/codes`;

  next(type: CodeEntityType): Observable<NextCodeResponse> {
    const params = new HttpParams().set('type', type);
    return this.http.get<NextCodeResponse>(`${this.base}/next`, { params });
  }
}
