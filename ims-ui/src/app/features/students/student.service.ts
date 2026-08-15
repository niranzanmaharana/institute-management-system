import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface StudentGuardian {
  id?: number;
  name: string;
  phone?: string;
  email?: string;
  relation: string;
  primaryGuardian: boolean;
}

export interface StudentAddress {
  id?: number;
  line1: string;
  line2?: string;
  city: string;
  state?: string;
  postalCode?: string;
  country?: string;
  primaryAddress: boolean;
}

export interface Student {
  id: number;
  studentCode: string;
  firstName: string;
  lastName: string;
  phone?: string;
  email?: string;
  status: string;
  guardians: StudentGuardian[];
  addresses: StudentAddress[];
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface CreateStudentRequest {
  studentCode?: string;
  firstName: string;
  lastName: string;
  phone?: string;
  email?: string;
  guardians?: StudentGuardian[];
  addresses?: StudentAddress[];
}

export interface UpdateStudentRequest {
  firstName: string;
  lastName: string;
  phone?: string;
  email?: string;
  guardians?: StudentGuardian[];
  addresses?: StudentAddress[];
}

@Injectable({ providedIn: 'root' })
export class StudentService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/v1/students`;

  list(q = '', page = 0, size = 20, status = ''): Observable<PageResponse<Student>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (q.trim()) {
      params = params.set('q', q.trim());
    }
    if (status.trim()) {
      params = params.set('status', status.trim());
    }
    return this.http.get<PageResponse<Student>>(this.base, { params });
  }

  get(id: number): Observable<Student> {
    return this.http.get<Student>(`${this.base}/${id}`);
  }

  create(body: CreateStudentRequest): Observable<Student> {
    return this.http.post<Student>(this.base, body);
  }

  update(id: number, body: UpdateStudentRequest): Observable<Student> {
    return this.http.put<Student>(`${this.base}/${id}`, body);
  }

  deactivate(id: number): Observable<Student> {
    return this.http.post<Student>(`${this.base}/${id}/deactivate`, {});
  }
}
