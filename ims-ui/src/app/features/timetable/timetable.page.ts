import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { UiCard } from '../../shared/ui-card';
import { UiButton } from '../../shared/ui-button';
import { UiInput } from '../../shared/ui-input';
import { UiDatepicker } from '../../shared/ui-datepicker';
import { EmptyState } from '../../shared/empty-state';
import { LoadingState } from '../../shared/loading-state';
import { ErrorState } from '../../shared/error-state';
import { StatusBadge } from '../../shared/status-badge';
import { ToastService } from '../../shared/toast.service';
import { httpErrorMessage, httpLoadError } from '../../core/http-error';
import { AuthService } from '../../core/auth.service';
import { AcademicService, Batch } from '../academic/academic.service';
import { Faculty, FacultyService } from '../faculty/faculty.service';
import { Room, TimetableService, TimetableSlot } from './timetable.service';

const DAY_LABELS = ['', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

@Component({
  selector: 'app-timetable-page',
  imports: [
    ReactiveFormsModule,
    UiCard,
    UiButton,
    UiInput,
    UiDatepicker,
    EmptyState,
    LoadingState,
    ErrorState,
    StatusBadge,
  ],
  templateUrl: './timetable.page.html',
  styleUrl: './timetable.page.scss',
})
export class TimetablePage implements OnInit {
  private readonly timetableApi = inject(TimetableService);
  private readonly academicApi = inject(AcademicService);
  private readonly facultyApi = inject(FacultyService);
  private readonly auth = inject(AuthService);
  private readonly fb = inject(FormBuilder);
  private readonly toasts = inject(ToastService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly rooms = signal<Room[]>([]);
  readonly batches = signal<Batch[]>([]);
  readonly faculty = signal<Faculty[]>([]);
  readonly slots = signal<TimetableSlot[]>([]);
  readonly selectedBatchId = signal<number | null>(null);
  readonly selectedDay = signal<number>(0); // 0 = week
  readonly busy = signal(false);

  readonly roomForm = this.fb.nonNullable.group({
    name: ['', Validators.required],
    capacity: ['30', [Validators.required, Validators.min(0)]],
  });

  readonly slotForm = this.fb.nonNullable.group({
    batchId: ['', Validators.required],
    roomId: ['', Validators.required],
    facultyId: [''],
    dayOfWeek: ['1', Validators.required],
    startTime: ['09:00', Validators.required],
    endTime: ['10:00', Validators.required],
  });

  readonly dayLabels = DAY_LABELS;

  readonly weekDays = [1, 2, 3, 4, 5, 6, 7];

  readonly slotsByDay = computed(() => {
    const map = new Map<number, TimetableSlot[]>();
    for (const d of this.weekDays) {
      map.set(d, []);
    }
    for (const slot of this.slots()) {
      const list = map.get(slot.dayOfWeek) ?? [];
      list.push(slot);
      map.set(slot.dayOfWeek, list);
    }
    return map;
  });

  readonly agendaSlots = computed(() => {
    const day = this.selectedDay();
    if (day === 0) {
      return this.slots();
    }
    return this.slots().filter((s) => s.dayOfWeek === day);
  });

  get canWrite(): boolean {
    return this.auth.hasPermission('timetable:write');
  }

  ngOnInit(): void {
    this.academicApi.listBatches().subscribe({
      next: (rows) => this.batches.set(rows),
      error: () => this.batches.set([]),
    });
    this.facultyApi.list('', 0, 100, 'ACTIVE').subscribe({
      next: (page) => this.faculty.set(page.content),
      error: () => this.faculty.set([]),
    });
    this.reloadRooms();
  }

  reloadRooms(): void {
    this.timetableApi.listRooms(false).subscribe({
      next: (rows) => this.rooms.set(rows),
      error: (err) => this.error.set(httpLoadError(err, 'rooms')),
    });
  }

  selectBatch(batchId: string): void {
    const id = Number(batchId);
    if (!Number.isFinite(id) || id <= 0) {
      this.selectedBatchId.set(null);
      this.slots.set([]);
      this.loading.set(false);
      return;
    }
    this.selectedBatchId.set(id);
    this.slotForm.patchValue({ batchId: String(id) });
    this.reloadTimetable();
  }

  setDay(day: number): void {
    this.selectedDay.set(day);
  }

  reloadTimetable(): void {
    const batchId = this.selectedBatchId();
    if (batchId == null) {
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.timetableApi.batchTimetable(batchId).subscribe({
      next: (data) => {
        this.slots.set(data.slots);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(httpLoadError(err, 'timetable'));
        this.loading.set(false);
      },
    });
  }

  createRoom(): void {
    if (!this.canWrite || this.busy()) {
      return;
    }
    if (this.roomForm.invalid) {
      this.roomForm.markAllAsTouched();
      const nameMissing = this.roomForm.controls.name.invalid;
      this.toasts.warning(
        nameMissing ? 'Room name is required.' : 'Check room name and capacity.',
      );
      return;
    }
    const v = this.roomForm.getRawValue();
    const name = v.name.trim();
    if (!name) {
      this.roomForm.controls.name.setErrors({ required: true });
      this.toasts.warning('Room name is required.');
      return;
    }
    this.busy.set(true);
    this.timetableApi
      .createRoom({
        name,
        capacity: Number(v.capacity) || 0,
      })
      .subscribe({
        next: () => {
          this.busy.set(false);
          this.roomForm.reset({ name: '', capacity: '30' });
          this.reloadRooms();
        },
        error: (err) => {
          this.busy.set(false);
          this.toasts.danger(httpErrorMessage(err, 'Could not create room.'));
        },
      });
  }

  deactivateRoom(room: Room): void {
    if (!this.canWrite || room.status !== 'ACTIVE' || this.busy()) {
      return;
    }
    this.busy.set(true);
    this.timetableApi.deactivateRoom(room.id).subscribe({
      next: () => {
        this.busy.set(false);
        this.reloadRooms();
      },
      error: (err) => {
        this.busy.set(false);
        this.toasts.danger(httpErrorMessage(err, 'Could not deactivate room.'));
      },
    });
  }

  createSlot(): void {
    if (!this.canWrite || this.busy()) {
      return;
    }
    if (this.slotForm.invalid) {
      this.slotForm.markAllAsTouched();
      this.toasts.warning('Select batch, room, day, and start/end times before adding a slot.');
      return;
    }
    const v = this.slotForm.getRawValue();
    const facultyId = v.facultyId ? Number(v.facultyId) : null;
    this.busy.set(true);
    this.timetableApi
      .createSlot({
        batchId: Number(v.batchId),
        roomId: Number(v.roomId),
        facultyId: facultyId && facultyId > 0 ? facultyId : null,
        dayOfWeek: Number(v.dayOfWeek),
        startTime: this.normalizeTime(v.startTime),
        endTime: this.normalizeTime(v.endTime),
      })
      .subscribe({
        next: () => {
          this.busy.set(false);
          this.selectedBatchId.set(Number(v.batchId));
          this.reloadTimetable();
        },
        error: (err) => {
          this.busy.set(false);
          this.toasts.danger(httpErrorMessage(err, 'Could not create slot.'));
        },
      });
  }

  cancelSlot(slot: TimetableSlot): void {
    if (!this.canWrite || this.busy()) {
      return;
    }
    if (!confirm(`Cancel slot ${DAY_LABELS[slot.dayOfWeek]} ${slot.startTime}-${slot.endTime}?`)) {
      return;
    }
    this.busy.set(true);
    this.timetableApi.cancelSlot(slot.id).subscribe({
      next: () => {
        this.busy.set(false);
        this.reloadTimetable();
      },
      error: (err) => {
        this.busy.set(false);
        this.toasts.danger(httpErrorMessage(err, 'Could not cancel slot.'));
      },
    });
  }

  dayName(day: number): string {
    return DAY_LABELS[day] || String(day);
  }

  private normalizeTime(value: string): string {
    const v = value.trim();
    return v.length === 5 ? `${v}:00` : v;
  }
}
