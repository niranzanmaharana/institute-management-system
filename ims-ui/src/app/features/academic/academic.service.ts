import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface AcademicYear {
  id: number;
  code: string;
  name: string;
  startDate: string;
  endDate: string;
  status: string;
}

export interface FeeCategory {
  id: number;
  code: string;
  name: string;
}

export interface Course {
  id: number;
  code: string;
  name: string;
  description?: string;
  status: string;
}

export interface FeePlanInstallment {
  id?: number;
  seq: number;
  feeCategoryId: number;
  label: string;
  amount: number;
  dueOffsetDays: number;
}

export interface FeePlan {
  id: number;
  courseId: number;
  code: string;
  name: string;
  currency: string;
  totalAmount: number;
  status: string;
  installments: FeePlanInstallment[];
}

export interface Batch {
  id: number;
  courseId: number;
  academicYearId: number;
  code: string;
  name: string;
  capacity: number;
  status: string;
  startDate?: string;
  endDate?: string;
}

export interface CreateAcademicYearRequest {
  code?: string;
  name: string;
  startDate: string;
  endDate: string;
}

export interface CreateFeeCategoryRequest {
  code?: string;
  name: string;
}

export interface CreateCourseRequest {
  code?: string;
  name: string;
  description?: string;
}

export interface CreateFeePlanRequest {
  code?: string;
  name: string;
  currency?: string;
  totalAmount: number;
  installments: FeePlanInstallment[];
}

export interface CreateBatchRequest {
  courseId: number;
  academicYearId: number;
  code?: string;
  name: string;
  capacity: number;
  startDate?: string;
  endDate?: string;
}

export interface UpdateCourseRequest {
  name: string;
  description?: string;
}

export interface UpdateBatchRequest {
  name: string;
  capacity: number;
  status?: string;
  startDate?: string;
  endDate?: string;
}

export interface UpdateFeePlanRequest {
  name: string;
  totalAmount: number;
  installments: FeePlanInstallment[];
}

@Injectable({ providedIn: 'root' })
export class AcademicService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/v1`;

  listAcademicYears(): Observable<AcademicYear[]> {
    return this.http.get<AcademicYear[]>(`${this.base}/academic-years`);
  }

  createAcademicYear(body: CreateAcademicYearRequest): Observable<AcademicYear> {
    return this.http.post<AcademicYear>(`${this.base}/academic-years`, body);
  }

  listFeeCategories(): Observable<FeeCategory[]> {
    return this.http.get<FeeCategory[]>(`${this.base}/fee-categories`);
  }

  createFeeCategory(body: CreateFeeCategoryRequest): Observable<FeeCategory> {
    return this.http.post<FeeCategory>(`${this.base}/fee-categories`, body);
  }

  listCourses(q = '', page = 0, size = 20): Observable<PageResponse<Course>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (q.trim()) {
      params = params.set('q', q.trim());
    }
    return this.http.get<PageResponse<Course>>(`${this.base}/courses`, { params });
  }

  getCourse(id: number): Observable<Course> {
    return this.http.get<Course>(`${this.base}/courses/${id}`);
  }

  createCourse(body: CreateCourseRequest): Observable<Course> {
    return this.http.post<Course>(`${this.base}/courses`, body);
  }

  updateCourse(id: number, body: UpdateCourseRequest): Observable<Course> {
    return this.http.put<Course>(`${this.base}/courses/${id}`, body);
  }

  deactivateCourse(id: number): Observable<Course> {
    return this.http.post<Course>(`${this.base}/courses/${id}/deactivate`, {});
  }

  listFeePlans(courseId: number): Observable<FeePlan[]> {
    return this.http.get<FeePlan[]>(`${this.base}/courses/${courseId}/fee-plans`);
  }

  getFeePlan(id: number): Observable<FeePlan> {
    return this.http.get<FeePlan>(`${this.base}/fee-plans/${id}`);
  }

  createFeePlan(courseId: number, body: CreateFeePlanRequest): Observable<FeePlan> {
    return this.http.post<FeePlan>(`${this.base}/courses/${courseId}/fee-plans`, body);
  }

  updateFeePlan(id: number, body: UpdateFeePlanRequest): Observable<FeePlan> {
    return this.http.put<FeePlan>(`${this.base}/fee-plans/${id}`, body);
  }

  listBatches(courseId?: number): Observable<Batch[]> {
    let params = new HttpParams();
    if (courseId != null) {
      params = params.set('courseId', courseId);
    }
    return this.http.get<Batch[]>(`${this.base}/batches`, { params });
  }

  getBatch(id: number): Observable<Batch> {
    return this.http.get<Batch>(`${this.base}/batches/${id}`);
  }

  createBatch(body: CreateBatchRequest): Observable<Batch> {
    return this.http.post<Batch>(`${this.base}/batches`, body);
  }

  updateBatch(id: number, body: UpdateBatchRequest): Observable<Batch> {
    return this.http.put<Batch>(`${this.base}/batches/${id}`, body);
  }
}
