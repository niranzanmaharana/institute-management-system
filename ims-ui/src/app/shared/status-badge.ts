import { Component, Input } from '@angular/core';

export type BadgeTone = 'success' | 'warning' | 'danger' | 'info' | 'neutral';

@Component({
  selector: 'app-status-badge',
  template: `<span class="badge" [attr.data-tone]="tone">{{ label }}</span>`,
  styles: `
    .badge {
      display: inline-flex;
      align-items: center;
      padding: 0.2rem 0.55rem;
      border-radius: 999px;
      font-size: 0.75rem;
      font-weight: 700;
    }
    .badge[data-tone='success'] {
      background: var(--ims-success-soft);
      color: var(--ims-success);
    }
    .badge[data-tone='warning'] {
      background: var(--ims-warning-soft);
      color: #c79100;
    }
    .badge[data-tone='danger'] {
      background: var(--ims-danger-soft);
      color: var(--ims-danger);
    }
    .badge[data-tone='info'] {
      background: #e3f2fd;
      color: var(--ims-info);
    }
    .badge[data-tone='neutral'] {
      background: var(--ims-bg);
      color: var(--ims-text-muted);
    }
  `,
})
export class StatusBadge {
  @Input({ required: true }) label!: string;
  @Input() tone: BadgeTone = 'neutral';
}
