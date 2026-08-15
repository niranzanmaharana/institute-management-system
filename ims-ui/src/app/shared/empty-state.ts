import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-empty-state',
  template: `
    <div class="state">
      <p class="state__title">{{ title }}</p>
      <p class="state__message">{{ message }}</p>
      <ng-content />
    </div>
  `,
  styles: `
    .state {
      text-align: center;
      padding: 2rem 1rem;
      color: var(--ims-text-muted);
    }
    .state__title {
      margin: 0 0 0.35rem;
      font-weight: 700;
      color: var(--ims-text);
    }
    .state__message {
      margin: 0;
    }
  `,
})
export class EmptyState {
  @Input() title = 'Nothing here yet';
  @Input() message = 'Data will appear once this module is enabled.';
}
