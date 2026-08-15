import { Component, Input, forwardRef } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

@Component({
  selector: 'app-ui-input',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => UiInput),
      multi: true,
    },
  ],
  template: `
    <label class="field" [attr.for]="inputId">
      <span class="field__label">{{ label }}</span>
      <span class="field__control-wrap" [class.field__control-wrap--password]="isPassword">
        <input
          class="field__control"
          [class.field__control--with-toggle]="isPassword"
          [id]="inputId"
          [type]="resolvedType"
          [attr.autocomplete]="autocomplete || null"
          [disabled]="disabled"
          [value]="value"
          (input)="onInput($event)"
          (blur)="onTouched()"
        />
        @if (isPassword) {
          <button
            type="button"
            class="field__toggle"
            [attr.aria-label]="revealed ? 'Hide password' : 'Show password'"
            [attr.aria-pressed]="revealed"
            (click)="toggleReveal($event)"
          >
            <i
              class="fa-solid"
              [class.fa-eye]="!revealed"
              [class.fa-eye-slash]="revealed"
              aria-hidden="true"
            ></i>
          </button>
        }
      </span>
      @if (hint) {
        <span class="field__hint">{{ hint }}</span>
      }
    </label>
  `,
  styles: `
    .field {
      display: flex;
      flex-direction: column;
      gap: 0.35rem;
    }
    .field__label {
      font-weight: 700;
      font-size: 0.9rem;
    }
    .field__control-wrap {
      position: relative;
      display: block;
    }
    .field__control {
      width: 100%;
      box-sizing: border-box;
      border: 1px solid var(--ims-border);
      border-radius: 8px;
      padding: 0.7rem 0.85rem;
      font: inherit;
      color: var(--ims-text);
      background: #fff;
    }
    .field__control--with-toggle {
      padding-right: 2.75rem;
    }
    .field__control:focus {
      outline: 2px solid var(--ims-primary-light);
      outline-offset: 1px;
    }
    .field__toggle {
      position: absolute;
      top: 50%;
      right: 0.35rem;
      transform: translateY(-50%);
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 2.25rem;
      height: 2.25rem;
      border: 0;
      border-radius: 6px;
      background: transparent;
      color: var(--ims-text-muted);
      cursor: pointer;
      padding: 0;
    }
    .field__toggle:hover {
      color: var(--ims-text);
      background: color-mix(in srgb, var(--ims-border) 55%, transparent);
    }
    .field__toggle:focus-visible {
      outline: 2px solid var(--ims-primary-light);
      outline-offset: 1px;
    }
    .field__hint {
      color: var(--ims-text-muted);
      font-size: 0.8rem;
    }
  `,
})
export class UiInput implements ControlValueAccessor {
  @Input({ required: true }) label!: string;
  @Input({ required: true }) inputId!: string;
  @Input() type = 'text';
  @Input() hint = '';
  @Input() autocomplete = '';

  value = '';
  disabled = false;
  revealed = false;
  private onChange: (value: string) => void = () => undefined;
  onTouched: () => void = () => undefined;

  get isPassword(): boolean {
    return this.type === 'password';
  }

  get resolvedType(): string {
    if (!this.isPassword) {
      return this.type;
    }
    return this.revealed ? 'text' : 'password';
  }

  writeValue(value: string | null): void {
    this.value = value ?? '';
  }

  registerOnChange(fn: (value: string) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  onInput(event: Event): void {
    const next = (event.target as HTMLInputElement).value;
    this.value = next;
    this.onChange(next);
  }

  toggleReveal(event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    this.revealed = !this.revealed;
  }
}
