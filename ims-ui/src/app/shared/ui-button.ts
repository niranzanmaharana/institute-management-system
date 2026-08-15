import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-ui-button',
  template: `
    <button class="btn" [type]="type" [disabled]="disabled || loading" [class.btn--block]="block">
      @if (loading) {
        <span>Please wait…</span>
      } @else {
        <ng-content />
      }
    </button>
  `,
  styles: `
    .btn {
      border: 0;
      border-radius: 8px;
      background: var(--ims-primary);
      color: #fff;
      font: inherit;
      font-weight: 700;
      padding: 0.75rem 1rem;
      cursor: pointer;
      min-height: 44px;
    }
    .btn:disabled {
      opacity: 0.65;
      cursor: not-allowed;
    }
    .btn--block {
      width: 100%;
    }
    .btn:focus-visible {
      outline: 2px solid var(--ims-primary-light);
      outline-offset: 2px;
    }
  `,
})
export class UiButton {
  @Input() type: 'button' | 'submit' = 'button';
  @Input() loading = false;
  @Input() disabled = false;
  @Input() block = false;
}
