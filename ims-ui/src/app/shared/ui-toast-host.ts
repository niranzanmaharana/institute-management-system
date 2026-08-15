import { Component, inject } from '@angular/core';
import { ToastService } from './toast.service';

@Component({
  selector: 'app-ui-toast-host',
  template: `
    <div class="toasts" aria-live="polite" aria-relevant="additions">
      @for (toast of toastService.toasts(); track toast.id) {
        <div class="toast" [attr.data-tone]="toast.tone" role="status">
          <span>{{ toast.message }}</span>
          <button type="button" class="toast__close" [attr.aria-label]="'Dismiss notification'" (click)="toastService.dismiss(toast.id)">
            ×
          </button>
        </div>
      }
    </div>
  `,
  styles: `
    .toasts {
      position: fixed;
      right: 1rem;
      bottom: 1rem;
      z-index: 1100;
      display: grid;
      gap: 0.5rem;
      width: min(360px, calc(100vw - 2rem));
    }
    .toast {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 0.75rem;
      padding: 0.85rem 1rem;
      border-radius: 8px;
      background: #fff;
      border: 1px solid var(--ims-border);
      box-shadow: var(--ims-shadow);
      color: var(--ims-text);
    }
    .toast[data-tone='success'] {
      border-color: var(--ims-success);
      background: var(--ims-success-soft);
    }
    .toast[data-tone='warning'] {
      border-color: var(--ims-warning);
      background: var(--ims-warning-soft);
    }
    .toast[data-tone='danger'] {
      border-color: var(--ims-danger);
      background: var(--ims-danger-soft);
    }
    .toast__close {
      border: 0;
      background: transparent;
      font-size: 1.25rem;
      cursor: pointer;
      line-height: 1;
    }
  `,
})
export class UiToastHost {
  readonly toastService = inject(ToastService);
}
