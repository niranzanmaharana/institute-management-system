import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { UiToastHost } from './shared/ui-toast-host';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, UiToastHost],
  template: `
    <router-outlet />
    <app-ui-toast-host />
  `,
  styles: `
    :host {
      display: block;
      min-height: 100%;
    }
  `,
})
export class App {}
