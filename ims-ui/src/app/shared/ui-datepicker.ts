import { Component, Input, forwardRef } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

@Component({
  selector: 'app-ui-datepicker',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => UiDatepicker),
      multi: true,
    },
  ],
  template: `
    <label class="field" [attr.for]="inputId">
      <span class="field__label">{{ label }}</span>
      <input
        class="field__control"
        type="date"
        [id]="inputId"
        [disabled]="disabled"
        [value]="value"
        [attr.min]="min || null"
        [attr.max]="max || null"
        (input)="onInput($event)"
        (blur)="onTouched()"
      />
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
export class UiDatepicker implements ControlValueAccessor {
  @Input({ required: true }) label!: string;
  @Input({ required: true }) inputId!: string;
  @Input() min = '';
  @Input() max = '';

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

  onInput(event: Event): void {
    const next = (event.target as HTMLInputElement).value;
    this.value = next;
    this.onChange(next);
  }
}
