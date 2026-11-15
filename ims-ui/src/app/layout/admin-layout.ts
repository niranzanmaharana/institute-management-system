import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs/operators';
import { AuthService } from '../core/auth.service';
import { InstituteService } from '../features/institutes/institute.service';

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
  private readonly institutesApi = inject(InstituteService);
  readonly auth = inject(AuthService);

  /** Desktop: expanded vs icon rail. Mobile: drawer open vs closed. */
  readonly sidebarOpen = signal(true);
  readonly isMobile = signal(false);
  readonly reportsMenuOpen = signal(false);
  readonly reportsRouteActive = signal(false);
  readonly brandLogoUrl = signal<string | null>(null);
  readonly brandTitle = signal('Skydash IMS');

  ngOnInit(): void {
    this.syncViewport();
    this.syncReportsMenu(this.router.url);
    this.loadBranding();
    const mq = window.matchMedia(`(max-width: ${MOBILE_MAX}px)`);
    const onChange = () => this.syncViewport();
    mq.addEventListener('change', onChange);
    this.destroyRef.onDestroy(() => mq.removeEventListener('change', onChange));

    this.router.events
      .pipe(
        filter((e): e is NavigationEnd => e instanceof NavigationEnd),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((e) => {
        this.syncReportsMenu(e.urlAfterRedirects);
        if (this.isMobile()) {
          this.sidebarOpen.set(false);
        }
      });
  }

  toggleReportsMenu(): void {
    if (!this.sidebarOpen() && !this.isMobile()) {
      this.sidebarOpen.set(true);
      this.reportsMenuOpen.set(true);
      return;
    }
    this.reportsMenuOpen.update((open) => !open);
  }

  private syncReportsMenu(url: string): void {
    const onReports = url.includes('/reports') || url.includes('/finance-reports');
    this.reportsRouteActive.set(onReports);
    if (onReports) {
      this.reportsMenuOpen.set(true);
    }
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

  private loadBranding(): void {
    const user = this.auth.user();
    if (!user?.instituteId) {
      this.brandTitle.set('Skydash IMS');
      this.brandLogoUrl.set(null);
      return;
    }
    if (user.instituteName) {
      this.brandTitle.set(user.instituteName);
    }
    this.institutesApi.currentBranding().subscribe({
      next: (branding) => {
        this.brandTitle.set(branding.name || user.instituteName || 'Skydash IMS');
        this.brandLogoUrl.set(branding.iconDownloadUrl || null);
      },
      error: () => {
        this.brandLogoUrl.set(null);
      },
    });
  }
}
