import {
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
import { Nav } from '../nav/nav';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [Nav, RouterLink],
  templateUrl: './header.html',
  styleUrls: ['./header.css'],
})
export class Header implements OnDestroy {
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

  get avatarText(): string {
    const email = (this.userEmail || '').trim();
    if (email) return email[0].toUpperCase();

    const label = (this.userLabel || '').trim();
    if (label) return label[0].toUpperCase();

    return '?';
  }

  get isAdmin(): boolean {
    return this.roleLabel?.toLowerCase() === 'admin';
  }

  toggleUserMenu() {
    this.userMenuOpen = !this.userMenuOpen;
    if (this.userMenuOpen) {
      this.addGlobalClickListener();
    } else {
      this.removeGlobalClickListener();
    }
  }

  closeUserMenu() {
    this.userMenuOpen = false;
    this.removeGlobalClickListener();
  }

  private addGlobalClickListener() {
    this.removeGlobalClickListener();
    this.globalClickListener = (event: MouseEvent) => {
      // Only close if click is outside the header element
      if (!this.el.nativeElement.contains(event.target as Node)) {
        // Run inside Angular zone to trigger change detection
        this.zone.run(() => this.closeUserMenu());
      }
    };
    document.addEventListener('click', this.globalClickListener, true);
  }

  private removeGlobalClickListener() {
    if (this.globalClickListener) {
      document.removeEventListener('click', this.globalClickListener, true);
      this.globalClickListener = undefined;
    }
  }

  onLogout() {
    this.logout.emit();
    this.closeUserMenu();
    this.closeNav.emit();
  }

  onOpenProfile() {
    this.openProfile.emit();
    this.closeUserMenu();
    this.closeNav.emit();
  }

  onOpenAdminDashboard() {
    this.router.navigate(['/admin-dashboard']);
    this.closeUserMenu();
    this.closeNav.emit();
  }

  onOpenMyBookings() {
    this.router.navigate(['/my-bookings']);
    this.closeUserMenu();
    this.closeNav.emit();
  }

  onOpenSystemSettings() {
    this.openSystemSettings.emit();
    this.closeUserMenu();
    this.closeNav.emit();
  }

  onOpenPaymentTransactions() {
    this.router.navigate(['/payment-transactions']);
    this.closeUserMenu();
    this.closeNav.emit();
  }

  ngOnDestroy() {
    this.removeGlobalClickListener();
  }
}
