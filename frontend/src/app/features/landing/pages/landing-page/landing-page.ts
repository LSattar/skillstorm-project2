import { CommonModule, DOCUMENT } from '@angular/common';
import { Component, HostListener, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';

// ✅ adjust these import paths if your folder depth differs
import { Footer } from '../../../../shared/footer/footer';
import { Header } from '../../../../shared/header/header';

type RoomType = 'any' | 'standard' | 'deluxe' | 'suite';

type BookingFormModel = {
  checkin: string;
  checkout: string;
  guests: number;
  roomType: RoomType;
  promo: string;
};

@Component({
  selector: 'app-landing-page',
  standalone: true,
  imports: [CommonModule, FormsModule, Header, Footer],
  templateUrl: './landing-page.html',
  styleUrl: './landing-page.css',
})
export class LandingPage {
  readonly today = new Date();
  private readonly document = inject(DOCUMENT);

  isNavOpen = false;
  isBookingOpen = false;

  booking: BookingFormModel = {
    checkin: '',
    checkout: '',
    guests: 2,
    roomType: 'any',
    promo: '',
  };

  toggleNav(): void {
    this.isNavOpen = !this.isNavOpen;
  }

  closeNav(): void {
    this.isNavOpen = false;
  }

  openBooking(): void {
    this.isBookingOpen = true;
    this.lockBodyScroll(true);
  }

  closeBooking(): void {
    this.isBookingOpen = false;
    this.lockBodyScroll(false);
  }

  submitBooking(): void {
    console.log('Booking search submitted:', this.booking);
    this.closeBooking();
  }

  @HostListener('document:keydown.escape')
  onEsc(): void {
    if (this.isBookingOpen) this.closeBooking();
    if (this.isNavOpen) this.closeNav();
  }

  @HostListener('window:resize')
  onResize(): void {
    if (window.innerWidth > 720 && this.isNavOpen) this.closeNav();
  }

  private lockBodyScroll(locked: boolean): void {
    this.document.body.classList.toggle('no-scroll', locked);
  }
}
