import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { filter, Subject, takeUntil } from 'rxjs';

@Component({
  selector: 'app-nav',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './nav.html',
  styleUrls: ['./nav.css'],
})
export class NavComponent implements OnInit, OnDestroy {
  scrollToSection(sectionId: string) {
    setTimeout(() => {
      const el = document.getElementById(sectionId);
      if (el) {
        el.scrollIntoView({ behavior: 'smooth', block: 'start' });
      }
    }, 50);
  }
  @Input() isOpen = false;
  @Input() isAdmin = false;
  @Input() isAuthenticated = false;

  @Output() toggle = new EventEmitter<void>();
  @Output() close = new EventEmitter<void>();

  moreOpen = false;

  // Computed locally (do NOT take as input)
  isOnAdminDashboard = false;

  // If true => render no <li> items anywhere
  hideAllNavItems = false;

  private destroy$ = new Subject<void>();

  constructor(private router: Router) {}

  ngOnInit(): void {
    this.updateRouteFlags(this.router.url);

    this.router.events
      .pipe(
        filter((e): e is NavigationEnd => e instanceof NavigationEnd),
        takeUntil(this.destroy$)
      )
      .subscribe((e) => {
        this.updateRouteFlags(e.urlAfterRedirects);
        this.moreOpen = false;
      });
  }

  private updateRouteFlags(url: string): void {
    // Normalize (strip query + fragment)
    const path = url.split('?')[0].split('#')[0];

    this.isOnAdminDashboard = path.startsWith('/admin-dashboard');

    // Pages where you want NO nav items at all (regardless of auth)
    const hideOnTheseRoutes =
      path.startsWith('/payment-transactions') ||
      path.startsWith('/profile-settings') ||
      path.startsWith('/system-admin');

    // Your rule:
    // - nav items only show on admin-dashboard
    // - so admins should have no items anywhere else
    const adminOffDashboard = this.isAdmin && !this.isOnAdminDashboard;

    // If you ALSO want to hide for non-admins on those specific pages, keep hideOnTheseRoutes.
    // If you truly want "ONLY admin-dashboard shows items for everyone", then set hideAllNavItems = !isOnAdminDashboard.
    this.hideAllNavItems = hideOnTheseRoutes || adminOffDashboard;
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  toggleMore(): void {
    this.moreOpen = !this.moreOpen;
  }

  onMoreKeydown(event: KeyboardEvent): void {
    if (event.key === 'Escape') this.moreOpen = false;
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      this.toggleMore();
    }
  }
}
