import { Injectable, signal } from '@angular/core';

export type ToastTone = 'info' | 'success' | 'warning' | 'danger';

export interface ToastMessage {
  id: number;
  message: string;
  tone: ToastTone;
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  private nextId = 1;
  readonly toasts = signal<ToastMessage[]>([]);

  show(message: string, tone: ToastTone = 'info', ttlMs = 3500): void {
    const id = this.nextId++;
    this.toasts.update((list) => [...list, { id, message, tone }]);
    window.setTimeout(() => this.dismiss(id), ttlMs);
  }

  success(message: string, ttlMs = 3500): void {
    this.show(message, 'success', ttlMs);
  }

  info(message: string, ttlMs = 3500): void {
    this.show(message, 'info', ttlMs);
  }

  warning(message: string, ttlMs = 4500): void {
    this.show(message, 'warning', ttlMs);
  }

  danger(message: string, ttlMs = 5000): void {
    this.show(message, 'danger', ttlMs);
  }

  dismiss(id: number): void {
    this.toasts.update((list) => list.filter((t) => t.id !== id));
  }
}
