import { CommonModule } from '@angular/common';
import {
  Component,
  ElementRef,
  EventEmitter,
  HostListener,
  Input,
  OnDestroy,
  OnInit,
  Output,
} from '@angular/core';
import { NavigationEnd, Router, RouterLink } from '@angular/router';
import { Subject, filter, takeUntil } from 'rxjs';

@Component({
  selector: 'app-nav',
  standalone: true,
  imports: [RouterLink, CommonModule],
  templateUrl: './nav.html',
  styleUrls: ['./nav.css'],
})
export class NavComponent implements OnInit, OnDestroy {
  @Input() isOpen = false;
  @Input() isAdmin = false;
  @Input() hideAllNavItems = false;

  // matches your header binding so NG8002 never happens
  @Input() isAuthenticated = false;

  @Output() toggle = new EventEmitter<void>();
  @Output() close = new EventEmitter<void>();

  // NEW: emit when user wants to generate a report (on /payment-transactions)
  @Output() report = new EventEmitter<void>();

  isOnAdminDashboard = false;
  isOnPaymentTransactions = false;

  moreOpen = false;

  private destroy$ = new Subject<void>();

  constructor(
    private router: Router,
    private host: ElementRef<HTMLElement>,
  ) {}

  ngOnInit(): void {
    this.setRouteFlags(this.router.url);

    this.router.events
      .pipe(
        filter((e): e is NavigationEnd => e instanceof NavigationEnd),
        takeUntil(this.destroy$),
      )
      .subscribe((e) => this.setRouteFlags(e.urlAfterRedirects));
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private setRouteFlags(url: string): void {
    const clean = url.split('?')[0].split('#')[0];

    this.isOnAdminDashboard = clean === '/admin-dashboard';
    this.isOnPaymentTransactions = clean === '/payment-transactions';

    this.moreOpen = false;
  }

  toggleMore(): void {
    this.moreOpen = !this.moreOpen;
  }

  closeAllMenus(): void {
    this.moreOpen = false;
    this.close.emit();
  }

  scrollToSection(sectionId: string): void {
    const el = document.getElementById(sectionId);
    if (!el) return;
    el.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.moreOpen) this.closeAllMenus();
  }

  @HostListener('document:click', ['$event'])
  onDocClick(event: MouseEvent): void {
    if (!this.moreOpen) return;

    const target = event.target as Node | null;
    if (!target) return;

    // close More menu when clicking outside the nav
    if (!this.host.nativeElement.contains(target)) {
      this.closeAllMenus();
    }
  }
}
