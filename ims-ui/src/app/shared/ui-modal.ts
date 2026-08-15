import {
  Component,
  ElementRef,
  HostListener,
  ViewChild,
  effect,
  input,
  output,
} from '@angular/core';

/** Modal/drawer dialog with Escape close and Tab focus trap (Phase 1 a11y). */
@Component({
  selector: 'app-ui-modal',
  template: `
    @if (open()) {
      <div class="modal-root" role="presentation">
        <button type="button" class="modal-backdrop" aria-label="Close dialog" (click)="closed.emit()"></button>
        <div
          #dialog
          class="modal"
          role="dialog"
          aria-modal="true"
          [attr.aria-labelledby]="titleId"
          tabindex="-1"
        >
          <header class="modal__header">
            <h2 [id]="titleId" class="modal__title">{{ title() }}</h2>
            <button type="button" class="icon-btn" aria-label="Close" (click)="closed.emit()">×</button>
          </header>
          <div class="modal__body">
            <ng-content />
          </div>
          <footer class="modal__footer">
            <ng-content select="[modal-actions]" />
          </footer>
        </div>
      </div>
    }
  `,
  styles: `
    .modal-root {
      position: fixed;
      inset: 0;
      z-index: 1000;
      display: grid;
      place-items: center;
      padding: 1rem;
    }
    .modal-backdrop {
      position: absolute;
      inset: 0;
      border: 0;
      background: rgba(52, 57, 94, 0.45);
      cursor: pointer;
    }
    .modal {
      position: relative;
      z-index: 1;
      width: min(480px, 100%);
      max-height: min(90vh, 640px);
      overflow: auto;
      background: var(--ims-card-bg);
      border-radius: var(--ims-radius);
      box-shadow: var(--ims-shadow);
      outline: none;
    }
    .modal__header,
    .modal__footer {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 0.75rem;
      padding: 1rem 1.25rem;
    }
    .modal__footer {
      justify-content: flex-end;
      border-top: 1px solid var(--ims-border);
    }
    .modal__title {
      margin: 0;
      font-size: 1.125rem;
    }
    .modal__body {
      padding: 0 1.25rem 1rem;
    }
    .icon-btn {
      border: 0;
      background: transparent;
      font-size: 1.5rem;
      line-height: 1;
      cursor: pointer;
      min-width: 44px;
      min-height: 44px;
    }
  `,
})
export class UiModal {
  readonly open = input(false);
  readonly title = input('Dialog');
  readonly closed = output<void>();
  @ViewChild('dialog') dialog?: ElementRef<HTMLElement>;

  readonly titleId = `modal-title-${Math.random().toString(36).slice(2, 9)}`;
  private previouslyFocused: HTMLElement | null = null;

  constructor() {
    effect(() => {
      if (this.open()) {
        queueMicrotask(() => this.focusDialog());
      } else {
        this.restoreFocus();
      }
    });
  }

  @HostListener('document:keydown', ['$event'])
  onKeydown(event: KeyboardEvent): void {
    if (!this.open()) {
      return;
    }
    if (event.key === 'Escape') {
      event.preventDefault();
      this.closed.emit();
      return;
    }
    if (event.key !== 'Tab' || !this.dialog?.nativeElement) {
      return;
    }
    const focusable = this.focusableElements();
    if (focusable.length === 0) {
      event.preventDefault();
      this.dialog.nativeElement.focus();
      return;
    }
    const first = focusable[0];
    const last = focusable[focusable.length - 1];
    const active = document.activeElement as HTMLElement | null;
    if (event.shiftKey && active === first) {
      event.preventDefault();
      last.focus();
    } else if (!event.shiftKey && active === last) {
      event.preventDefault();
      first.focus();
    }
  }

  private focusDialog(): void {
    this.previouslyFocused = document.activeElement as HTMLElement | null;
    const focusable = this.focusableElements();
    (focusable[0] ?? this.dialog?.nativeElement)?.focus();
  }

  private restoreFocus(): void {
    this.previouslyFocused?.focus?.();
    this.previouslyFocused = null;
  }

  private focusableElements(): HTMLElement[] {
    const root = this.dialog?.nativeElement;
    if (!root) {
      return [];
    }
    return Array.from(
      root.querySelectorAll<HTMLElement>(
        'button:not([disabled]), [href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])',
      ),
    );
  }
}
