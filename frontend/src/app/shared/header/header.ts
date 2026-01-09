import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { Nav } from '../nav/nav';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [Nav, RouterLink],
  templateUrl: './header.html',
  styleUrls: ['./header.css'],
})
export class Header {
  private readonly router = inject(Router);

  @Input() isNavOpen = false;
  @Input() isAuthenticated = false;
  @Input() userLabel = '';
  @Input() roleLabel = '';
  @Input() userEmail = '';
  @Input() showSystemSettings = false;

  @Output() toggleNav = new EventEmitter<void>();
  @Output() closeNav = new EventEmitter<void>();
  @Output() openBooking = new EventEmitter<void>();
  @Output() openSignIn = new EventEmitter<void>();
  @Output() logout = new EventEmitter<void>();
  @Output() openProfile = new EventEmitter<void>();
  @Output() openSystemSettings = new EventEmitter<void>();

  userMenuOpen = false;

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
  }

  closeUserMenu() {
    this.userMenuOpen = false;
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

  onOpenSystemSettings() {
    this.openSystemSettings.emit();
    this.closeUserMenu();
    this.closeNav.emit();
  }
}
