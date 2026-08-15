import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs/operators';
import { AuthService } from '../core/auth.service';

const MOBILE_MAX = 767;

@Component({
  selector: 'app-admin-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './admin-layout.html',
  styleUrl: './admin-layout.scss',
})
export class AdminLayout implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly router = inject(Router);
  readonly auth = inject(AuthService);

  /** Desktop: expanded vs icon rail. Mobile: drawer open vs closed. */
  readonly sidebarOpen = signal(true);
  readonly isMobile = signal(false);

  ngOnInit(): void {
    this.syncViewport();
    const mq = window.matchMedia(`(max-width: ${MOBILE_MAX}px)`);
    const onChange = () => this.syncViewport();
    mq.addEventListener('change', onChange);
    this.destroyRef.onDestroy(() => mq.removeEventListener('change', onChange));

    this.router.events
      .pipe(
        filter((e): e is NavigationEnd => e instanceof NavigationEnd),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => {
        if (this.isMobile()) {
          this.sidebarOpen.set(false);
        }
      });
  }

  toggleSidebar(): void {
    this.sidebarOpen.update((open) => !open);
  }

  closeSidebar(): void {
    this.sidebarOpen.set(false);
  }

  onNavClick(): void {
    if (this.isMobile()) {
      this.closeSidebar();
    }
  }

  openSidebar(): void {
    this.sidebarOpen.set(true);
  }

  logout(): void {
    this.auth.logout();
  }

  private syncViewport(): void {
    const mobile = window.innerWidth <= MOBILE_MAX;
    this.isMobile.set(mobile);
    // Mobile starts with drawer closed; desktop starts expanded.
    this.sidebarOpen.set(!mobile);
  }
}
