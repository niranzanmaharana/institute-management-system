import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface Room {
  id: number;
  code: string;
  name: string;
  capacity: number;
  status: string;
}

export interface TimetableSlot {
  id: number;
  batchId: number;
  subjectId?: number | null;
  subjectCode?: string | null;
  subjectName?: string | null;
  facultyId?: number | null;
  facultyCode?: string | null;
  facultyName?: string | null;
  roomId: number;
  roomCode: string;
  roomName: string;
  dayOfWeek: number;
  startTime: string;
  endTime: string;
  status: string;
}

export interface BatchTimetable {
  batchId: number;
  slots: TimetableSlot[];
}

@Injectable({ providedIn: 'root' })
export class TimetableService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/v1`;

  listRooms(activeOnly = true): Observable<Room[]> {
    const params = new HttpParams().set('activeOnly', activeOnly);
    return this.http.get<Room[]>(`${this.base}/rooms`, { params });
  }

  createRoom(body: { name: string; capacity: number; code?: string }): Observable<Room> {
    return this.http.post<Room>(`${this.base}/rooms`, body);
  }

  deactivateRoom(id: number): Observable<Room> {
    return this.http.post<Room>(`${this.base}/rooms/${id}/deactivate`, {});
  }

  batchTimetable(batchId: number, dayOfWeek?: number): Observable<BatchTimetable> {
    let params = new HttpParams();
    if (dayOfWeek != null) {
      params = params.set('dayOfWeek', dayOfWeek);
    }
    return this.http.get<BatchTimetable>(`${this.base}/timetable/batches/${batchId}`, { params });
  }

  createSlot(body: {
    batchId: number;
    subjectId?: number | null;
    facultyId?: number | null;
    roomId: number;
    dayOfWeek: number;
    startTime: string;
    endTime: string;
  }): Observable<TimetableSlot> {
    return this.http.post<TimetableSlot>(`${this.base}/timetable/slots`, body);
  }

  cancelSlot(id: number): Observable<void> {
    return this.http.post<void>(`${this.base}/timetable/slots/${id}/cancel`, {});
  }
}
