import { CommonModule } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Footer } from '../../../../shared/footer/footer';
import { Header } from '../../../../shared/header/header';
import { AuthService } from '../../../auth/services/auth.service';

@Component({
  selector: 'app-landing-page',
  standalone: true,
  imports: [CommonModule, FormsModule, Header, Footer],
  templateUrl: './landing-page.html',
  styleUrls: ['./landing-page.css'],
})
export class LandingPage {
  protected readonly auth = inject(AuthService);

  protected readonly isAuthenticated = this.auth.isAuthenticated;
  protected readonly roleLabel = this.auth.primaryRoleLabel;
  protected readonly userLabel = computed(() => {
    const me = this.auth.meSignal();
    if (!me) return '';
    const first = me.firstName?.trim();
    const last = me.lastName?.trim();
    const fullName = [first, last].filter(Boolean).join(' ');
    return fullName || me.email || '';
  });

  isNavOpen = false;
  isBookingOpen = false;
  isSignInOpen = false;

  toggleNav() {
    this.isNavOpen = !this.isNavOpen;
  }

  closeNav() {
    this.isNavOpen = false;
  }

  today = new Date();

  booking = {
    checkin: '',
    checkout: '',
    guests: 1,
    roomType: 'any',
    promo: '',
  };

  firstName = '';
  lastName = '';
  email = '';

  // Booking modal
  openBooking() {
    this.isBookingOpen = true;
  }

  closeBooking() {
    this.isBookingOpen = false;
  }

  submitBooking() {
    this.closeBooking();
  }

  // Sign-in modal
  openSignIn() {
    this.isSignInOpen = true;
  }

  closeSignIn() {
    this.isSignInOpen = false;
  }

  continueWithGoogle() {
    this.isSignInOpen = false;
    this.auth.startGoogleLogin();
  }

  signOut() {
    this.auth.logout().subscribe({
      next: () => {},
      error: () => {},
    });
  }
}
