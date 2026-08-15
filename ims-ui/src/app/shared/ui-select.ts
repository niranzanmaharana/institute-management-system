import { Component, Input, forwardRef } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

export interface UiSelectOption {
  value: string;
  label: string;
}

@Component({
  selector: 'app-ui-select',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => UiSelect),
      multi: true,
    },
  ],
  template: `
    <label class="field" [attr.for]="selectId">
      <span class="field__label">{{ label }}</span>
      <select
        class="field__control"
        [id]="selectId"
        [disabled]="disabled"
        [value]="value"
        (change)="onChangeEvent($event)"
        (blur)="onTouched()"
      >
        @if (placeholder) {
          <option value="" disabled [selected]="!value">{{ placeholder }}</option>
        }
        @for (opt of options; track opt.value) {
          <option [value]="opt.value" [selected]="opt.value === value">{{ opt.label }}</option>
        }
      </select>
    </label>
  `,
  styles: `
    .field {
      display: grid;
      gap: 0.35rem;
    }
    .field__label {
      font-size: 0.875rem;
      font-weight: 700;
    }
    .field__control {
      min-height: 44px;
      border: 1px solid var(--ims-border);
      border-radius: 8px;
      padding: 0.55rem 0.75rem;
      font: inherit;
      background: #fff;
    }
    .field__control:focus-visible {
      outline: 2px solid var(--ims-primary-light);
      outline-offset: 2px;
    }
  `,
})
export class UiSelect implements ControlValueAccessor {
  @Input({ required: true }) label!: string;
  @Input({ required: true }) selectId!: string;
  @Input() options: UiSelectOption[] = [];
  @Input() placeholder = '';

  value = '';
  disabled = false;
  private onChange: (value: string) => void = () => undefined;
  onTouched: () => void = () => undefined;

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

  onChangeEvent(event: Event): void {
    const next = (event.target as HTMLSelectElement).value;
    this.value = next;
    this.onChange(next);
  }
}
