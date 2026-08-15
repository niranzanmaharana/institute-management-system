import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-loading-state',
  template: `
    <div class="state" role="status">
      <p>{{ message }}</p>
    </div>
  `,
  styles: `
    .state {
      padding: 1.5rem;
      text-align: center;
      color: var(--ims-text-muted);
      font-weight: 600;
    }
  `,
})
export class LoadingState {
  @Input() message = 'Loading…';
}
