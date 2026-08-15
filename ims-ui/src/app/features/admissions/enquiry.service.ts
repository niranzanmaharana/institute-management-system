import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Admission, PageResponse } from './admission.service';

export interface Enquiry {
  id: number;
  name: string;
  phone?: string;
  email?: string;
  interestedCourseId?: number;
  status: string;
  notes?: string;
  convertedApplicationId?: number;
  createdAt: string;
}

export interface CreateEnquiryRequest {
  name: string;
  phone?: string;
  email?: string;
  interestedCourseId?: number;
  notes?: string;
}

export interface ConvertEnquiryRequest {
  applicationNo?: string;
  courseId?: number;
}

@Injectable({ providedIn: 'root' })
export class EnquiryService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/v1/enquiries`;

  list(q = '', page = 0, size = 20, status = ''): Observable<PageResponse<Enquiry>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (q.trim()) {
      params = params.set('q', q.trim());
    }
    if (status.trim()) {
      params = params.set('status', status.trim());
    }
    return this.http.get<PageResponse<Enquiry>>(this.base, { params });
  }

  get(id: number): Observable<Enquiry> {
    return this.http.get<Enquiry>(`${this.base}/${id}`);
  }

  create(body: CreateEnquiryRequest): Observable<Enquiry> {
    return this.http.post<Enquiry>(this.base, body);
  }

  close(id: number, notes?: string): Observable<Enquiry> {
    return this.http.post<Enquiry>(`${this.base}/${id}/close`, { notes });
  }

  convert(id: number, body: ConvertEnquiryRequest = {}): Observable<Admission> {
    return this.http.post<Admission>(`${this.base}/${id}/convert`, body);
  }
}
