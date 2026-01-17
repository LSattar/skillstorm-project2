import {
  AfterViewInit,
  Component,
  ElementRef,
  EventEmitter,
  inject,
  Input,
  NgZone,
  OnDestroy,
  Output,
} from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { NavComponent } from '../nav/nav';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [NavComponent, RouterLink],
  templateUrl: './header.html',
  styleUrls: ['./header.css'],
})
export class Header implements OnDestroy, AfterViewInit {
  private readonly router = inject(Router);
  private readonly el = inject(ElementRef<HTMLElement>);
  private readonly zone = inject(NgZone);

  @Input() isNavOpen = false;
  @Input() isAuthenticated = false;
  @Input() userLabel = '';
  @Input() roleLabel = '';
  @Input() userEmail = '';
  @Input() showSystemSettings = false;
  @Input() showPaymentTransactions = false;

  @Output() toggleNav = new EventEmitter<void>();
  @Output() closeNav = new EventEmitter<void>();
  @Output() openBooking = new EventEmitter<void>();
  @Output() openSignIn = new EventEmitter<void>();
  @Output() logout = new EventEmitter<void>();
  @Output() openProfile = new EventEmitter<void>();
  @Output() openSystemSettings = new EventEmitter<void>();

  userMenuOpen = false;
  private globalClickListener?: (event: MouseEvent) => void;

  get currentRoute(): string {
    return this.router.url;
  }

  get isAdmin(): boolean {
    return this.roleLabel?.toLowerCase() === 'admin';
  }

  get avatarText(): string {
    const email = (this.userEmail || '').trim();
    if (email) return email[0].toUpperCase();

    const label = (this.userLabel || '').trim();
    if (label) return label[0].toUpperCase();

    return '?';
  }

  ngAfterViewInit(): void {
    // Close user menu if on profile settings page to prevent modal whitespace
    if (this.router.url === '/profile-settings' && this.userMenuOpen) {
      this.closeUserMenu();
    }
  }

  toggleUserMenu(): void {
    this.userMenuOpen = !this.userMenuOpen;
    if (this.userMenuOpen) {
      this.addGlobalClickListener();
    } else {
      this.removeGlobalClickListener();
    }
  }

  closeUserMenu(): void {
    this.userMenuOpen = false;
    this.removeGlobalClickListener();
  }

  private addGlobalClickListener(): void {
    this.removeGlobalClickListener();
    this.globalClickListener = (event: MouseEvent) => {
      if (!this.el.nativeElement.contains(event.target as Node)) {
        this.zone.run(() => this.closeUserMenu());
      }
    };
    document.addEventListener('click', this.globalClickListener, true);
  }

  private removeGlobalClickListener(): void {
    if (this.globalClickListener) {
      document.removeEventListener('click', this.globalClickListener, true);
      this.globalClickListener = undefined;
    }
  }

  onLogout(): void {
    this.logout.emit();
    this.closeUserMenu();
    this.closeNav.emit();
  }

  onOpenProfile(): void {
    this.openProfile.emit();
    this.closeUserMenu();
    this.closeNav.emit();
  }

  onOpenAdminDashboard(): void {
    this.router.navigate(['/admin-dashboard']);
    this.closeUserMenu();
    this.closeNav.emit();
  }

  onOpenMyBookings(): void {
    this.router.navigate(['/my-bookings']);
    this.closeUserMenu();
    this.closeNav.emit();
  }

  onOpenSystemSettings(): void {
    this.router.navigate(['/admin/system-settings']);
    this.closeUserMenu();
    this.closeNav.emit();
  }

  onOpenPaymentTransactions(): void {
    this.router.navigate(['/payment-transactions']);
    this.closeUserMenu();
    this.closeNav.emit();
  }

  scrollToReports(event: Event): void {
    event.preventDefault();
    // Only scroll if we're on the admin dashboard
    if (this.currentRoute === '/admin-dashboard') {
      const reportsSection = document.getElementById('reports');
      if (reportsSection) {
        reportsSection.scrollIntoView({ behavior: 'smooth', block: 'start' });
      } else {
        // Navigate to admin dashboard with reports fragment if not already there
        this.router.navigate(['/admin-dashboard'], { fragment: 'reports' });
      }
    } else {
      // Navigate to admin dashboard with reports fragment
      this.router.navigate(['/admin-dashboard'], { fragment: 'reports' });
    }
    this.closeNav.emit();
  }

  ngOnDestroy(): void {
    this.removeGlobalClickListener();
  }
}
