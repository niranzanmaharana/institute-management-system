import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-error-state',
  template: `
    <div class="state" role="alert">
      <p class="state__title">{{ title }}</p>
      <p>{{ message }}</p>
      <ng-content />
    </div>
  `,
  styles: `
    .state {
      padding: 1.5rem;
      text-align: center;
      background: var(--ims-danger-soft);
      color: var(--ims-danger);
      border-radius: var(--ims-radius);
    }
    .state__title {
      margin: 0 0 0.35rem;
      font-weight: 700;
    }
  `,
})
export class ErrorState {
  @Input() title = 'Something went wrong';
  @Input() message = 'Please try again.';
}
