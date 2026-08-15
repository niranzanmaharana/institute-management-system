import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface FacultyAddress {
  id?: number;
  line1: string;
  line2?: string;
  city: string;
  state?: string;
  postalCode?: string;
  country?: string;
  primaryAddress: boolean;
}

export interface Faculty {
  id: number;
  facultyCode: string;
  firstName: string;
  lastName: string;
  phone?: string;
  email?: string;
  department?: string;
  status: string;
  addresses: FacultyAddress[];
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface CreateFacultyRequest {
  facultyCode?: string;
  firstName: string;
  lastName: string;
  phone?: string;
  email?: string;
  department?: string;
  addresses?: FacultyAddress[];
}

export interface UpdateFacultyRequest {
  firstName: string;
  lastName: string;
  phone?: string;
  email?: string;
  department?: string;
  addresses?: FacultyAddress[];
}

@Injectable({ providedIn: 'root' })
export class FacultyService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/v1/faculties`;

  list(q = '', page = 0, size = 20, status = ''): Observable<PageResponse<Faculty>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (q.trim()) {
      params = params.set('q', q.trim());
    }
    if (status.trim()) {
      params = params.set('status', status.trim());
    }
    return this.http.get<PageResponse<Faculty>>(this.base, { params });
  }

  get(id: number): Observable<Faculty> {
    return this.http.get<Faculty>(`${this.base}/${id}`);
  }

  create(body: CreateFacultyRequest): Observable<Faculty> {
    return this.http.post<Faculty>(this.base, body);
  }

  update(id: number, body: UpdateFacultyRequest): Observable<Faculty> {
    return this.http.put<Faculty>(`${this.base}/${id}`, body);
  }

  deactivate(id: number): Observable<Faculty> {
    return this.http.post<Faculty>(`${this.base}/${id}/deactivate`, {});
  }
}
